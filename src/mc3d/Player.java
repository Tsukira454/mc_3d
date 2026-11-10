package mc3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.glfwGetTime;

/**
 * Camera 3D = le joueur. Position (les yeux), orientation (pitch/yaw) et
 * physique simple.
 *
 * Convention de signe : yVelocity POSITIF = vers le haut, NEGATIF = vers le
 * bas.
 */
public class Player {

	private Vector3f position = new Vector3f(0, 10, 0); // on spawn en l'air -> on tombe sur le sol
	private float pitch; // H/B
	private float yaw; // G/D
	private double walkSpeed = 0.1;
	final private double sensibility = 0.5;
	private double lastAction;

	// --- physique ---
	final private double gravity = 54;
	final private double jumpForce = 12.5;
	private double playerHeight = 1.8;
	final private double playerRadius = 0.3;
	private double yVelocity = 0;
	private boolean onGround = false;
	private boolean run = false;
	private boolean sneak = false;
	private double dt = 0;
	private double lastDt = 0;
	private double mnt;

	int blockPlaceIndex = 0;

	public double getLastAction() {
		return this.lastAction;
	}

	public void setLastAction(double lA) {
		this.lastAction = lA;
	}

	private boolean collides(Maps maps) {

		// player hitbox
		List<List<Double>> playerHitboxe = new ArrayList<>();
		List<Double> playerHitboxeX = Arrays.asList(this.position.x - playerRadius, this.position.x + playerRadius);
		List<Double> playerHitboxeY = Arrays.asList(this.position.y - playerHeight + 0.01, this.position.y + 0.0); // pied
																													// //
																													// tête
		List<Double> playerHitboxeZ = Arrays.asList(this.position.z - playerRadius, this.position.z + playerRadius);
		playerHitboxe.add(playerHitboxeX);
		playerHitboxe.add(playerHitboxeY);
		playerHitboxe.add(playerHitboxeZ);

		// block au alentour
		for (int bx = (int) Math.round(playerHitboxeX.get(0)); bx <= Math.round(playerHitboxeX.get(1)); bx++) {
			for (int by = (int) Math.round(playerHitboxeY.get(0)); by <= Math.round(playerHitboxeY.get(1)); by++) {
				for (int bz = (int) Math.round(playerHitboxeZ.get(0)); bz <= Math.round(playerHitboxeZ.get(1)); bz++) {
					if (maps.isBlock(new Vector3f(bx, by, bz))) {
						// tester les axes
						boolean axeX = (playerHitboxeX.get(0) < bx + 0.5) && (bx - 0.5 < playerHitboxeX.get(1));
						boolean axeY = (playerHitboxeY.get(0) < by + 0.5) && (by - 0.5 < playerHitboxeY.get(1));
						boolean axeZ = (playerHitboxeZ.get(0) < bz + 0.5) && (bz - 0.5 < playerHitboxeZ.get(1));
						if (axeX && axeY && axeZ) {
							return true;
						}
					}
				}
			}
		}
		// si c good
		return false;
	}

	public void newDt() {
		this.mnt = glfwGetTime();
		this.dt = mnt - this.lastDt;
		this.dt = Math.min(dt, 0.05);
		this.lastDt = mnt;
	}

	/** Gravite + collision VERTICALE (atterrissage sur le dessus d'un bloc). */
	public void applyGravity(Maps maps) {
		yVelocity -= gravity * this.dt;
		float lastY = position.y;

		position.y += (float) yVelocity * this.dt;
		if (this.collides(maps)) {
			position.y = lastY;

			if (yVelocity < 0) {
				onGround = true;
			} else {
				onGround = false;
			}
			yVelocity = 0;
		} else {
			onGround = false;
		}

		if (this.position.y <= -50) {
			this.position = new Vector3f(0, 50, 0);
		}
	}

	public void run() {
		this.run = true;
	}

	public void stopRun() {
		this.run = false;
	}

	public void sneak() {
		this.sneak = true;
		this.playerHeight = 1.4;
		this.position.y -= 0.4;
	}

	public void stopSneak() {
		this.sneak = false;
		this.playerHeight = 1.8;
		this.position.y += 0.4;
	}

	public void changeBlock() {
		this.blockPlaceIndex += 1;
		this.blockPlaceIndex %= BlockType.values().length;
	}

	/**
	 * Raycast DDA : trouve le bloc que le joueur regarde.
	 *
	 * Idee : on avance le long du rayon "de mur en mur". A chaque etape on se
	 * demande quel mur de case (X, Y ou Z) le rayon touche EN PREMIER, on passe
	 * dans la case voisine de ce cote-la, et on regarde s'il y a un bloc. => 1
	 * etape = 1 nouvelle case, on ne peut jamais en sauter une.
	 *
	 * Rappel : les blocs sont centres sur les entiers, le bloc 3 va de 2.5 a 3.5.
	 *
	 * @return null si rien n'est touche dans la portee, sinon une liste [0] = la
	 *         case vide juste avant le bloc (la ou on POSE), null si l'oeil est
	 *         deja dans un bloc [1] = le bloc touche (celui qu'on CASSE)
	 */
	public List<Vector3f> raycast(Maps maps) {
		double reach = 5.0; // portee max, en blocs

		// ---- 1. Direction du regard (vecteur de longueur 1) ----
		double yawR = Math.toRadians(yaw);
		double pitchR = Math.toRadians(pitch);
		double dx = Math.sin(yawR) * Math.cos(pitchR);
		double dy = -Math.sin(pitchR);
		double dz = -Math.cos(yawR) * Math.cos(pitchR);

		// ---- 2. La case de depart : celle ou sont les yeux ----
		int cx = Math.round(position.x);
		int cy = Math.round(position.y);
		int cz = Math.round(position.z);

		// Oeil deja dans un bloc : on le vise directement, impossible de poser
		if (maps.isBlock(new Vector3f(cx, cy, cz))) {
			List<Vector3f> result = new ArrayList<>();
			result.add(null);
			result.add(new Vector3f(cx, cy, cz));
			return result;
		}

		// ---- 3. Le sens dans lequel on avance sur chaque axe : +1 ou -1 ----
		int stepX = (dx > 0) ? 1 : -1;
		int stepY = (dy > 0) ? 1 : -1;
		int stepZ = (dz > 0) ? 1 : -1;

		// ---- 4. tDelta : distance (le long du rayon) pour traverser UNE case entiere
		// ----
		// Ex : si le rayon avance de 0.5 en X par unite de distance,
		// il faut 1 / 0.5 = 2 de distance pour faire 1 bloc en X.
		// Si d == 0, on n'avance jamais sur cet axe -> distance infinie.
		double tDeltaX = (dx != 0) ? Math.abs(1 / dx) : Double.POSITIVE_INFINITY;
		double tDeltaY = (dy != 0) ? Math.abs(1 / dy) : Double.POSITIVE_INFINITY;
		double tDeltaZ = (dz != 0) ? Math.abs(1 / dz) : Double.POSITIVE_INFINITY;

		// ---- 5. tMax : distance (le long du rayon) jusqu'au PROCHAIN mur sur chaque
		// axe ----
		// Le prochain mur est a case + 0.5 (si on va vers +) ou case - 0.5 (si on va
		// vers -).
		// Il reste (mur - position) a parcourir sur cet axe, et le rayon en fait d par
		// unite de distance -> distance = (mur - position) / d.
		// Ex : x = 3.2, dx = 0.5 -> mur a 3.5 -> (3.5 - 3.2) / 0.5 = 0.6
		double tMaxX = (dx != 0) ? ((cx + 0.5 * stepX) - position.x) / dx : Double.POSITIVE_INFINITY;
		double tMaxY = (dy != 0) ? ((cy + 0.5 * stepY) - position.y) / dy : Double.POSITIVE_INFINITY;
		double tMaxZ = (dz != 0) ? ((cz + 0.5 * stepZ) - position.z) / dz : Double.POSITIVE_INFINITY;

		// ---- 6. La boucle : de mur en mur ----
		while (true) {
			// on retient la case actuelle AVANT de bouger (c'est la qu'on posera un bloc)
			Vector3f avant = new Vector3f(cx, cy, cz);

			// Quel mur est le plus proche ? On passe dans la case voisine de ce cote-la,
			// puis on calcule la distance jusqu'au mur suivant sur ce meme axe.
			if (tMaxX < tMaxY && tMaxX < tMaxZ) {
				if (tMaxX > reach)
					return null; // le prochain mur est trop loin : rien touche
				cx += stepX;
				tMaxX += tDeltaX;
			} else if (tMaxY < tMaxZ) {
				if (tMaxY > reach)
					return null;
				cy += stepY;
				tMaxY += tDeltaY;
			} else {
				if (tMaxZ > reach)
					return null;
				cz += stepZ;
				tMaxZ += tDeltaZ;
			}

			// Nouvelle case : y a-t-il un bloc ?
			Vector3f cell = new Vector3f(cx, cy, cz);
			if (maps.isBlock(cell)) {
				List<Vector3f> result = new ArrayList<>();
				result.add(avant); // la case vide d'avant, collee par une FACE au bloc
				result.add(cell); // le bloc touche
				return result;
			}
		}
	}

	public void place(Maps maps) {
		List<Vector3f> raycastResult = this.raycast(maps);
		if (raycastResult != null) {
			if (raycastResult.get(0) != null) {
				maps.placeBlock(raycastResult.get(0), BlockType.values()[blockPlaceIndex]);
				if (this.collides(maps)) {
					maps.removeBlock(raycastResult.get(0));
				}
			}
		}
	}

	public void destroy(Maps maps) {
		List<Vector3f> raycastResult = this.raycast(maps);
		if (raycastResult != null) {
			maps.removeBlock(raycastResult.get(1));
		}
	}

	/** Matrice "point de vue". On fait l'inverse des mouvements de la camera. */
	public Matrix4f getViewMatrix() {
		return new Matrix4f().rotateX((float) Math.toRadians(pitch)).rotateY((float) Math.toRadians(yaw))
				.translate(-position.x, -position.y, -position.z);
	}

	/** Matrice de perspective. fov = angle de vue, aspect = largeur/hauteur. */
	public Matrix4f getProjectionMatrix(int width, int height) {
		float aspect = (float) width / height;
		return new Matrix4f().perspective((float) Math.toRadians(70.0), aspect, 0.1f, 100.0f);
	}

	public void setPitch(double delta) {
		double newPitch = this.pitch + (delta * this.sensibility);
		if (newPitch <= 89 && newPitch >= -89) {
			this.pitch += delta * this.sensibility;
		}
	}

	public void setYaw(double delta) {
		this.yaw += delta * this.sensibility;
	}

	// --- getters (debug, etc.) ---
	public Vector3f getPosition() {
		return position;
	}

	public float getYaw() {
		return yaw;
	}

	public float getPitch() {
		return pitch;
	}

	private boolean solSousPied(Maps maps) {
		float lastY = position.y;
		position.y -= 0.05;
		if (!this.collides(maps) && this.sneak && this.onGround) {
			position.y = lastY;
			return false;
		}
		position.y = lastY;
		return true;
		
	}

	private void moveCheck(double nextX, double nextZ, Maps maps) {
		float lastX = position.x;
		float lastZ = position.z;
		

		position.x = (float) nextX;
		if (this.collides(maps) || !solSousPied(maps))
			position.x = lastX;

		position.z = (float) nextZ;
		if (this.collides(maps) || !solSousPied(maps))
			position.z = lastZ;
		
		maps.GenerateChunk(position);
	}

	public void move(Direction d, Maps maps) {
		double avantX = Math.sin(Math.toRadians(this.yaw));
		double avantZ = -Math.cos(Math.toRadians(this.yaw));
		double droiteX = Math.cos(Math.toRadians(this.yaw));
		double droiteZ = Math.sin(Math.toRadians(this.yaw));

		if (this.run) {
			this.walkSpeed = 9 * this.dt;
		} else {
			this.walkSpeed = 6 * this.dt;
		}
		if (this.sneak) {
			this.walkSpeed = (this.walkSpeed / 10) * 7;
		}

		switch (d) {
		case Forward -> {
			double nextX = position.x + avantX * walkSpeed;
			double nextZ = position.z + avantZ * walkSpeed;
			moveCheck(nextX, nextZ, maps);
		}
		case Backward -> {
			double nextX = position.x - avantX * walkSpeed;
			double nextZ = position.z - avantZ * walkSpeed;
			moveCheck(nextX, nextZ, maps);
		}
		case Left -> {
			double nextX = position.x - droiteX * walkSpeed;
			double nextZ = position.z - droiteZ * walkSpeed;
			moveCheck(nextX, nextZ, maps);
		}
		case Right -> {
			double nextX = position.x + droiteX * walkSpeed;
			double nextZ = position.z + droiteZ * walkSpeed;
			moveCheck(nextX, nextZ, maps);
		}
		case Up -> {
			if (onGround) {
				yVelocity = jumpForce;
				onGround = false;
				}
			}
		}
	}
}
