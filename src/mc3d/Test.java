package mc3d;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * ============================================================================
 *  TEST.JAVA : des meshes d'exemple, du plus simple au plus complet.
 *  Lance ce fichier a la place de Main (clic droit > Run As > Java Application).
 * ============================================================================
 *
 * Touches :
 *   Fleches gauche/droite : tourner autour de la scene
 *   Fleches haut/bas      : monter / descendre la camera
 *   F                     : mode "fil de fer" (on voit les TRIANGLES)
 *   C                     : active / desactive le "culling" (faces arriere)
 *   Echap                 : quitter
 *
 * ----------------------------------------------------------------------------
 *  RAPPEL : C'EST QUOI UN MESH ?
 * ----------------------------------------------------------------------------
 * La carte graphique ne sait dessiner QUE des triangles. Un mesh, c'est donc
 * juste une liste de triangles, decrite avec 3 tableaux :
 *
 *   1. positions : les SOMMETS (les coins), 3 floats chacun (x, y, z)
 *   2. uvs       : pour chaque sommet, quel point de l'IMAGE il prend,
 *                  2 floats chacun (u, v). (0,0) = coin bas-gauche de l'image,
 *                  (1,1) = coin haut-droit.
 *   3. indices   : quels sommets relier, 3 par 3, pour faire les triangles.
 *                  Les numeros = la position du sommet dans la liste (0, 1, 2...)
 *
 * Le sommet numero 2, c'est : positions[6], positions[7], positions[8]
 *                        et : uvs[4], uvs[5]
 * (le sommet n a ses x,y,z en 3n, 3n+1, 3n+2 et ses u,v en 2n, 2n+1)
 *
 * ----------------------------------------------------------------------------
 *  L'ORDRE DES SOMMETS (le "winding") : TRES IMPORTANT
 * ----------------------------------------------------------------------------
 * Quand le culling est actif (glEnable(GL_CULL_FACE)), OpenGL ne dessine un
 * triangle QUE si ses 3 sommets tournent dans le sens INVERSE des aiguilles
 * d'une montre, vus depuis la camera. Sinon il considere qu'on le voit "de dos"
 * et il le jette (gain de perf : on ne dessine jamais l'interieur d'un cube).
 *
 * => Regle : on donne toujours les coins dans le sens inverse des aiguilles
 *    d'une montre, VUS DE L'EXTERIEUR de la face.
 *
 * Fais le tour de la scene avec les fleches : les objets plats (triangle,
 * carre, face) DISPARAISSENT quand tu passes derriere. Appuie sur C pour
 * desactiver le culling : ils reapparaissent. C'est exactement ca.
 */
public class Test {

	private static final String VERTEX_SHADER = """
			#version 330 core
			layout (location = 0) in vec3 aPos;
			layout (location = 1) in vec2 aUV;
			out vec2 vUV;
			uniform mat4 projection;
			uniform mat4 view;
			uniform mat4 model;
			void main() {
			    gl_Position = projection * view * model * vec4(aPos, 1.0);
			    vUV = aUV;
			}
			""";

	private static final String FRAGMENT_SHADER = """
			#version 330 core
			in vec2 vUV;
			uniform sampler2D uTexture;
			out vec4 FragColor;
			void main() {
			    vec4 couleur = texture(uTexture, vUV);
			    if (couleur.a < 0.5) discard;
			    FragColor = couleur;
			}
			""";

	// =========================================================================
	// EXEMPLE 1 : UN TRIANGLE
	// =========================================================================
	// Le plus petit mesh possible : 3 sommets, 1 triangle.
	//
	//              2 (0, 0.5)
	//             / \
	//            /   \
	//           /     \
	//  (-0.5,-0.5) 0 --- 1 (0.5,-0.5)
	//
	// 0 -> 1 -> 2 = sens inverse des aiguilles d'une montre vu de devant (+Z)
	// => visible quand la camera est devant, invisible de dos.
	static Mesh exemple1Triangle() {
		float[] positions = {
				-0.5f, -0.5f, 0f,   // sommet 0 : en bas a gauche
				 0.5f, -0.5f, 0f,   // sommet 1 : en bas a droite
				 0.0f,  0.5f, 0f,   // sommet 2 : en haut au milieu
		};
		float[] uvs = {
				0.0f, 0.0f,         // sommet 0 -> coin bas-gauche de l'image
				1.0f, 0.0f,         // sommet 1 -> coin bas-droit de l'image
				0.5f, 1.0f,         // sommet 2 -> milieu du haut de l'image
		};
		int[] indices = {
				0, 1, 2             // un seul triangle : relie les sommets 0, 1 et 2
		};
		return new Mesh(positions, uvs, indices);
	}

	// =========================================================================
	// EXEMPLE 2 : UN CARRE (= 2 triangles)
	// =========================================================================
	// Pas de "carre" en OpenGL : on le coupe en 2 triangles.
	//
	//   3 -------- 2
	//   |        / |
	//   |  B   /   |      triangle A = 0, 1, 2
	//   |    /     |      triangle B = 2, 3, 0
	//   |  /   A   |
	//   0 -------- 1
	//
	// 4 sommets seulement, mais 6 indices : les sommets 0 et 2 servent DEUX fois.
	// C'est tout l'interet des indices : on ne repete pas les sommets.
	// (C'est exactement ton viseur dans Main !)
	static Mesh exemple2Carre() {
		float[] positions = {
				-0.5f, -0.5f, 0f,   // 0 : bas gauche
				 0.5f, -0.5f, 0f,   // 1 : bas droite
				 0.5f,  0.5f, 0f,   // 2 : haut droite
				-0.5f,  0.5f, 0f,   // 3 : haut gauche
		};
		float[] uvs = {
				0, 0,               // 0 -> bas gauche de l'image
				1, 0,               // 1 -> bas droite
				1, 1,               // 2 -> haut droite
				0, 1,               // 3 -> haut gauche
		};
		int[] indices = {
				0, 1, 2,            // triangle A
				2, 3, 0             // triangle B
		};
		return new Mesh(positions, uvs, indices);
	}

	// =========================================================================
	// EXEMPLE 3 : LA FACE DU DESSUS D'UN BLOC
	// =========================================================================
	// Meme chose que le carre... mais couche a plat, en haut d'un bloc.
	// Le bloc est centre en (0,0,0) et fait 1 de cote, donc il va de -0.5 a +0.5.
	// Sa face du dessus est a y = +0.5.
	//
	// Vue DU DESSUS (on regarde vers le bas, -Z est "vers le haut de l'ecran") :
	//
	//         -Z
	//   3 -------- 2
	//   |          |
	//   |  dessus  |     0 -> 1 -> 2 -> 3 : sens inverse des aiguilles
	//   |          |     d'une montre vu d'en haut => visible d'en haut
	//   0 -------- 1
	//         +Z
	static Mesh exemple3FaceDessus() {
		float[] positions = {
				-0.5f, 0.5f,  0.5f, // 0
				 0.5f, 0.5f,  0.5f, // 1
				 0.5f, 0.5f, -0.5f, // 2
				-0.5f, 0.5f, -0.5f, // 3
		};
		float[] uvs = { 0, 0, 1, 0, 1, 1, 0, 1 };
		int[] indices = { 0, 1, 2, 2, 3, 0 };
		return new Mesh(positions, uvs, indices);
	}

	// =========================================================================
	// LES 6 FACES D'UN BLOC (des "patrons" a recopier)
	// =========================================================================
	// Chaque face = 4 coins, autour d'un bloc centre en (0,0,0).
	// Les coins sont deja dans le bon ordre (inverse des aiguilles d'une montre
	// vu de l'exterieur). Pour placer la face sur un bloc en (x,y,z), il suffit
	// d'ajouter x, y, z a chaque coin.
	// Les UV sont toujours les memes : (0,0) (1,0) (1,1) (0,1).
	static final float H = 0.5f; // demi-taille d'un bloc

	static final float[][] FACE_DESSUS = { { -H, H, H }, { H, H, H }, { H, H, -H }, { -H, H, -H } };
	static final float[][] FACE_DESSOUS = { { -H, -H, -H }, { H, -H, -H }, { H, -H, H }, { -H, -H, H } };
	static final float[][] FACE_SUD = { { -H, -H, H }, { H, -H, H }, { H, H, H }, { -H, H, H } };     // +Z
	static final float[][] FACE_NORD = { { H, -H, -H }, { -H, -H, -H }, { -H, H, -H }, { H, H, -H } }; // -Z
	static final float[][] FACE_EST = { { H, -H, H }, { H, -H, -H }, { H, H, -H }, { H, H, H } };     // +X
	static final float[][] FACE_OUEST = { { -H, -H, -H }, { -H, -H, H }, { -H, H, H }, { -H, H, -H } }; // -X

	/**
	 * LA fonction cle du meshing : ajoute UNE face a des listes qu'on remplit
	 * petit a petit.
	 *
	 * Le piege des indices : la 1ere face a ses sommets en 0,1,2,3 -> indices
	 * 0,1,2, 2,3,0. Mais la 2eme face a ses sommets en 4,5,6,7 ! Ses indices
	 * doivent donc etre 4,5,6, 6,7,4. D'ou "base" = le nombre de sommets DEJA
	 * presents avant d'ajouter cette face, qu'on ajoute a chaque indice.
	 */
	static void ajouterFace(List<Float> positions, List<Float> uvs, List<Integer> indices,
			float[][] coins, float x, float y, float z) {

		int base = positions.size() / 3; // nb de sommets deja dans la liste (3 floats par sommet)

		// les 4 coins, decales a la position du bloc
		for (float[] coin : coins) {
			positions.add(coin[0] + x);
			positions.add(coin[1] + y);
			positions.add(coin[2] + z);
		}

		// les UV : toujours l'image entiere sur la face
		uvs.add(0f); uvs.add(0f);
		uvs.add(1f); uvs.add(0f);
		uvs.add(1f); uvs.add(1f);
		uvs.add(0f); uvs.add(1f);

		// les 2 triangles, decales de "base"
		indices.add(base + 0); indices.add(base + 1); indices.add(base + 2);
		indices.add(base + 2); indices.add(base + 3); indices.add(base + 0);
	}

	// =========================================================================
	// EXEMPLE 4 : 3 FACES DU DESSUS COTE A COTE, DANS UN SEUL MESH
	// =========================================================================
	// Ici on n'ecrit plus les tableaux a la main : on BOUCLE et on appelle
	// ajouterFace. Resultat : 12 sommets, 18 indices, 1 seul mesh.
	// Affiche-le en fil de fer (F) : tu vois les 6 triangles.
	static Mesh exemple4RangeeDeFaces() {
		List<Float> positions = new ArrayList<>();
		List<Float> uvs = new ArrayList<>();
		List<Integer> indices = new ArrayList<>();

		for (int x = 0; x < 3; x++) {
			ajouterFace(positions, uvs, indices, FACE_DESSUS, x, 0, 0);
		}

		System.out.println("Exemple 4 : " + positions.size() / 3 + " sommets, " + indices.size() + " indices");
		return creerMesh(positions, uvs, indices);
	}

	// =========================================================================
	// EXEMPLE 5 : UN CUBE COMPLET = 6 FACES
	// =========================================================================
	// 6 faces x 4 sommets = 24 sommets, 6 faces x 6 indices = 36 indices.
	// (Oui, chaque coin du cube existe 3 fois : une fois par face qui le touche.
	// C'est normal : chaque face a ses propres UV.)
	static Mesh exemple5Cube() {
		List<Float> positions = new ArrayList<>();
		List<Float> uvs = new ArrayList<>();
		List<Integer> indices = new ArrayList<>();

		ajouterFace(positions, uvs, indices, FACE_DESSUS, 0, 0, 0);
		ajouterFace(positions, uvs, indices, FACE_DESSOUS, 0, 0, 0);
		ajouterFace(positions, uvs, indices, FACE_NORD, 0, 0, 0);
		ajouterFace(positions, uvs, indices, FACE_SUD, 0, 0, 0);
		ajouterFace(positions, uvs, indices, FACE_EST, 0, 0, 0);
		ajouterFace(positions, uvs, indices, FACE_OUEST, 0, 0, 0);

		System.out.println("Exemple 5 : " + positions.size() / 3 + " sommets, " + indices.size() + " indices");
		return creerMesh(positions, uvs, indices);
	}

	// =========================================================================
	// EXEMPLE 6 : UN MINI-CHUNK (c'est DEJA du meshing !)
	// =========================================================================
	// Un petit tableau 3x2x3 de blocs, avec un trou. Pour chaque bloc, pour
	// chaque direction : on ajoute la face SEULEMENT si le voisin est vide.
	// Les faces collees entre deux blocs ne sont jamais creees.
	// Compare dans la console : nb de faces creees vs (nb de blocs x 6).
	static Mesh exemple6MiniChunk() {
		int tx = 3, ty = 2, tz = 3;
		boolean[][][] blocs = new boolean[tx][ty][tz];
		for (int x = 0; x < tx; x++)
			for (int z = 0; z < tz; z++) {
				blocs[x][0][z] = true;         // une couche pleine en bas
			}
		blocs[1][1][1] = true;                 // un bloc pose au milieu, au-dessus
		blocs[0][0][0] = false;                // un trou dans un coin

		List<Float> positions = new ArrayList<>();
		List<Float> uvs = new ArrayList<>();
		List<Integer> indices = new ArrayList<>();
		int nbBlocs = 0;

		for (int x = 0; x < tx; x++)
			for (int y = 0; y < ty; y++)
				for (int z = 0; z < tz; z++) {
					if (!blocs[x][y][z]) continue; // de l'air : rien a dessiner
					nbBlocs++;

					// pour chaque direction : si le voisin est vide -> la face est visible
					if (estVide(blocs, x, y + 1, z)) ajouterFace(positions, uvs, indices, FACE_DESSUS, x, y, z);
					if (estVide(blocs, x, y - 1, z)) ajouterFace(positions, uvs, indices, FACE_DESSOUS, x, y, z);
					if (estVide(blocs, x, y, z - 1)) ajouterFace(positions, uvs, indices, FACE_NORD, x, y, z);
					if (estVide(blocs, x, y, z + 1)) ajouterFace(positions, uvs, indices, FACE_SUD, x, y, z);
					if (estVide(blocs, x + 1, y, z)) ajouterFace(positions, uvs, indices, FACE_EST, x, y, z);
					if (estVide(blocs, x - 1, y, z)) ajouterFace(positions, uvs, indices, FACE_OUEST, x, y, z);
				}

		int nbFaces = indices.size() / 6;
		System.out.println("Exemple 6 : " + nbBlocs + " blocs -> " + nbFaces + " faces creees (au lieu de "
				+ nbBlocs * 6 + " si on dessinait tous les cubes en entier)");
		return creerMesh(positions, uvs, indices);
	}

	/** Vide = en dehors du tableau, ou case a false. */
	static boolean estVide(boolean[][][] blocs, int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0 || x >= blocs.length || y >= blocs[0].length || z >= blocs[0][0].length)
			return true;
		return !blocs[x][y][z];
	}

	/** Convertit les listes (taille inconnue a l'avance) en tableaux pour Mesh. */
	static Mesh creerMesh(List<Float> positions, List<Float> uvs, List<Integer> indices) {
		float[] p = new float[positions.size()];
		for (int i = 0; i < p.length; i++) p[i] = positions.get(i);
		float[] u = new float[uvs.size()];
		for (int i = 0; i < u.length; i++) u[i] = uvs.get(i);
		int[] idx = new int[indices.size()];
		for (int i = 0; i < idx.length; i++) idx[i] = indices.get(i);
		return new Mesh(p, u, idx);
	}

	// =========================================================================
	// LE PROGRAMME : on affiche les 6 exemples cote a cote
	// =========================================================================
	static boolean filDeFer = false;
	static boolean culling = true;

	public static void main(String[] args) {
		Window window = new Window(1280, 720, "Test des meshes  |  fleches = tourner, F = fil de fer, C = culling");
		window.init();

		ShaderProgram shader = new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);
		shader.createUniform("projection");
		shader.createUniform("view");
		shader.createUniform("model");
		shader.createUniform("uTexture");

		Texture terre = new Texture("textures/block/dirt.png");

		// chaque mesh + la position ou on le pose dans la scene
		Mesh[] meshes = { exemple1Triangle(), exemple2Carre(), exemple3FaceDessus(), exemple4RangeeDeFaces(),
				exemple5Cube(), exemple6MiniChunk() };
		Vector3f[] places = { new Vector3f(0, 0, 0), new Vector3f(2, 0, 0), new Vector3f(4, 0, 0),
				new Vector3f(6, 0, 0), new Vector3f(10.5f, 0, 0), new Vector3f(13, -0.5f, -1) };

		glfwSetKeyCallback(window.getHandle(), (win, key, scancode, action, mods) -> {
			if (action != GLFW_PRESS) return;
			if (key == GLFW_KEY_ESCAPE) glfwSetWindowShouldClose(win, true);
			if (key == GLFW_KEY_F) filDeFer = !filDeFer;
			if (key == GLFW_KEY_C) culling = !culling;
		});

		float angle = 0;      // rotation de la camera autour de la scene
		float hauteur = 3;    // hauteur de la camera
		Vector3f centre = new Vector3f(7.5f, 0, 0);

		while (!window.shouldClose()) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			if (glfwGetKey(window.getHandle(), GLFW_KEY_LEFT) == GLFW_PRESS) angle -= 0.002f;
			if (glfwGetKey(window.getHandle(), GLFW_KEY_RIGHT) == GLFW_PRESS) angle += 0.002f;
			if (glfwGetKey(window.getHandle(), GLFW_KEY_UP) == GLFW_PRESS) hauteur += 0.01f;
			if (glfwGetKey(window.getHandle(), GLFW_KEY_DOWN) == GLFW_PRESS) hauteur -= 0.01f;

			if (culling) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE);
			glPolygonMode(GL_FRONT_AND_BACK, filDeFer ? GL_LINE : GL_FILL);

			// camera qui tourne autour du centre de la scene, a 11 de distance
			Vector3f oeil = new Vector3f(centre.x + (float) Math.sin(angle) * 11, hauteur,
					centre.z + (float) Math.cos(angle) * 11);
			Matrix4f view = new Matrix4f().lookAt(oeil, centre, new Vector3f(0, 1, 0));
			Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(70),
					(float) window.getWidth() / window.getHeight(), 0.1f, 100f);

			shader.bind();
			shader.setUniform("projection", projection);
			shader.setUniform("view", view);
			glActiveTexture(GL_TEXTURE0);
			terre.bind();
			shader.setUniform("uTexture", 0);

			// un draw call par mesh : chaque mesh est pose a sa place avec la matrice "model"
			for (int i = 0; i < meshes.length; i++) {
				shader.setUniform("model", new Matrix4f().translate(places[i]));
				meshes[i].render();
			}

			shader.unbind();
			window.update();
		}

		for (Mesh m : meshes) m.cleanup();
		terre.cleanup();
		shader.cleanup();
		window.cleanup();
	}
}
