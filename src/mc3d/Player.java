package mc3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Camera 3D = le joueur. Position (les yeux), orientation (pitch/yaw) et
 * physique simple.
 *
 * Convention de signe : yVelocity POSITIF = vers le haut, NEGATIF = vers le bas.
 */
public class Player {

	private Vector3f position = new Vector3f(0, 10, 0); // on spawn en l'air -> on tombe sur le sol
	private float pitch; // H/B
	private float yaw; // G/D
	private double walkSpeed = 0.1;
	final private double sensibility = 0.5;

	// --- physique ---
	final private double gravity = 0.015;
	final private double jumpForce = 0.25;
	final private double playerHeight = 1.8;
	final private double playerRadius = 0.5;
	private double yVelocity = 0;
	private boolean onGround = false;
	private boolean run=false;
	
	int blockPlaceIndex=0;

	private boolean collides(Maps maps) {
		
		//player hitbox
		List<List<Double>> playerHitboxe = new ArrayList<>();
		List<Double> playerHitboxeX = Arrays.asList(this.position.x-playerRadius,this.position.x+playerRadius);
		List<Double> playerHitboxeY = Arrays.asList(this.position.y-playerHeight,this.position.y+0.0); // pied / tête
		List<Double> playerHitboxeZ = Arrays.asList(this.position.z-playerRadius,this.position.z+playerRadius);
		playerHitboxe.add(playerHitboxeX);
		playerHitboxe.add(playerHitboxeY);
		playerHitboxe.add(playerHitboxeZ);
		
		//block au alentour
		for(int bx=(int) Math.round(playerHitboxeX.get(0)); bx<=Math.round(playerHitboxeX.get(1)); bx++) {
			for(int by=(int) Math.round(playerHitboxeY.get(0)); by<=Math.round(playerHitboxeY.get(1)); by++) {
				for(int bz=(int) Math.round(playerHitboxeZ.get(0)); bz<=Math.round(playerHitboxeZ.get(1)); bz++) {
					if(maps.isBlock(new Vector3f(bx,by,bz))) {
						// tester les axes
						boolean axeX = (playerHitboxeX.get(0) < bx + 0.5) && (bx - 0.5 < playerHitboxeX.get(1));
						boolean axeY = (playerHitboxeY.get(0) < by + 0.5) && (by - 0.5 < playerHitboxeY.get(1));
						boolean axeZ = (playerHitboxeZ.get(0) < bz + 0.5) && (bz - 0.5 < playerHitboxeZ.get(1));
						if(axeX && axeY && axeZ){
							return true;
						}
					}
				}
			}
		}
		// si c good
		return false;
	}
	/** Gravite + collision VERTICALE (atterrissage sur le dessus d'un bloc). */
	public void applyGravity(Maps maps) {
		yVelocity -= gravity;

		double feetY = position.y - playerHeight;
		double newFeetY = feetY + yVelocity;

		int cellX = Math.round(position.x);
		int cellZ = Math.round(position.z);
		int cellY = (int) Math.floor(newFeetY + 0.5);

		if (yVelocity <= 0 && maps.isBlock(new Vector3f(cellX, cellY, cellZ))) {
			position.y = (float) (cellY + 0.5 + playerHeight);
			yVelocity = 0;
			onGround = true;
		} else {
			position.y += (float) yVelocity;
			onGround = false;
		}
		
		if(this.position.y <=-50) {
			this.position = new Vector3f(0,50,0);
		}
	}
	
	public void run() {
		this.run=true;
	}
	
	public void stopRun() {
		this.run=false;
	}
	
	public void changeBlock() {
		this.blockPlaceIndex+=1;
		this.blockPlaceIndex%=BlockType.values().length;
	}
	public List<Vector3f> raycast(Maps maps) {
		// direction 3D du regard (en RADIANS)
		double yawR = Math.toRadians(yaw);
		double pitchR = Math.toRadians(pitch);
		double dx = Math.sin(yawR) * Math.cos(pitchR);
		double dy = -Math.sin(pitchR);
		double dz = -Math.cos(yawR) * Math.cos(pitchR);

		double reach = 5.0;
		double stepSize = 0.05;

		Vector3f avant = null;
		for (double dist = 0; dist <= reach; dist += stepSize) {
			int cx = (int) Math.round(position.x + dx * dist);
			int cy = (int) Math.round(position.y + dy * dist);
			int cz = (int) Math.round(position.z + dz * dist);
			Vector3f cell = new Vector3f(cx, cy, cz);

			if (maps.isBlock(cell)) {
				List<Vector3f> result = new ArrayList<>();
				result.add(avant);
				result.add(cell);
				return result;
			}
			avant = cell;
		}
		return null;
	}

	public void place(Maps maps) {
		List<Vector3f> raycastResult = this.raycast(maps);
		if (raycastResult != null) {
			if (raycastResult.get(0) != null) maps.placeBlock(raycastResult.get(0), BlockType.values()[blockPlaceIndex]);
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
	public Vector3f getPosition() { return position; }
	public float getYaw()         { return yaw; }
	public float getPitch()       { return pitch; }

	/**
	 * Y a-t-il un bloc qui gene le joueur a cette position (x,z) ?
	 * Teste toute la BOITE du joueur : largeur (rayon) x hauteur (pieds -> tete).
	 */
	private boolean estBloque(Maps maps, double x, double z) {
		double r = playerRadius;
		double feetY = position.y - playerHeight;
		int celluleBas = (int) Math.round(feetY + 0.05); // case des pieds (marge anti-sol)
		int celluleHaut = (int) Math.round(position.y);  // case de la tete
		for (int cx = (int) Math.round(x - r); cx <= (int) Math.round(x + r); cx++) {
			for (int cz = (int) Math.round(z - r); cz <= (int) Math.round(z + r); cz++) {
				for (int cy = celluleBas; cy <= celluleHaut; cy++) {
					if (maps.isBlock(new Vector3f(cx, cy, cz))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public void move(Direction d, Maps maps) {
		double avantX = Math.sin(Math.toRadians(this.yaw));
		double avantZ = -Math.cos(Math.toRadians(this.yaw));
		double droiteX = Math.cos(Math.toRadians(this.yaw));
		double droiteZ = Math.sin(Math.toRadians(this.yaw));
		if(this.run) {
			this.walkSpeed=0.15;
		} else {
			this.walkSpeed=0.1;
		}
		switch (d) {
		case Forward -> {
			double nextX = position.x + avantX * walkSpeed;
			double nextZ = position.z + avantZ * walkSpeed;
			if (!estBloque(maps, nextX, position.z)) position.x = (float) nextX;
			if (!estBloque(maps, position.x, nextZ)) position.z = (float) nextZ;
		}
		case Backward -> {
			double nextX = position.x - avantX * walkSpeed;
			double nextZ = position.z - avantZ * walkSpeed;
			if (!estBloque(maps, nextX, position.z)) position.x = (float) nextX;
			if (!estBloque(maps, position.x, nextZ)) position.z = (float) nextZ;
		}
		case Left -> {
			double nextX = position.x - droiteX * walkSpeed;
			double nextZ = position.z - droiteZ * walkSpeed;
			if (!estBloque(maps, nextX, position.z)) position.x = (float) nextX;
			if (!estBloque(maps, position.x, nextZ)) position.z = (float) nextZ;
		}
		case Right -> {
			double nextX = position.x + droiteX * walkSpeed;
			double nextZ = position.z + droiteZ * walkSpeed;
			if (!estBloque(maps, nextX, position.z)) position.x = (float) nextX;
			if (!estBloque(maps, position.x, nextZ)) position.z = (float) nextZ;
		}
		case Up -> {
			if (onGround) { // on ne saute que si on touche le sol
				yVelocity = jumpForce;
				onGround = false;
			}
		}
		case Down -> { /* desactive : la gravite gere la verticale */ }
		}
	}
}
