package mc3d;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;

import org.joml.Vector3f;

/**
 * Petit menu de debug : affiche les infos (FPS, position, orientation) dans la
 * BARRE DE TITRE de la fenetre. Simple, zero rendu de texte.
 *
 * ===> POUR TOUT RETIRER : supprime ce fichier + les 2 lignes marquees "DEBUG" dans Main.
 */
public class Debug {

	private final Window window;
	private final Player camera;

	private int frames = 0;      // images comptees depuis la derniere seconde
	private int fps = 0;         // dernier FPS fige
	private double lastTime = glfwGetTime();

	public Debug(Window window, Player camera) {
		this.window = window;
		this.camera = camera;
	}

	/** A appeler une fois par image (dans la boucle de jeu). */
	public void update() {
		// Compte les images ; toutes les 1 seconde, on fige la valeur du FPS
		frames++;
		double now = glfwGetTime();
		if (now - lastTime >= 1.0) {
			fps = frames;
			frames = 0;
			lastTime = now;
		}

		Vector3f p = camera.getPosition();
		String titre = String.format(
				"mc_3d   |   FPS: %d   |   XYZ: %.1f / %.1f / %.1f   |   yaw: %.0f   pitch: %.0f",
				fps, p.x, p.y, p.z, camera.getYaw(), camera.getPitch());
		glfwSetWindowTitle(window.getHandle(), titre);
	}
}
