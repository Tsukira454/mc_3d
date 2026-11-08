package mc3d;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.joml.Math;
import org.joml.Matrix4f;

/**
 * Point d'entree du jeu. Cree la fenetre, le shader, le monde (des blocs) et
 * une player, puis lance la boucle de jeu.
 */
public class Main {

	// ---- Les shaders, ecrits en GLSL, stockes en texte (text blocks Java 25) ----

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

	public static void main(String[] args) {
		Window window = new Window(1280, 720, "mc_3d");
		window.init();

		ShaderProgram shader = new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);
		shader.createUniform("projection");
		shader.createUniform("view");
		shader.createUniform("model");
		shader.createUniform("uTexture");

		Maps world = new Maps();
		world.GenerateMaps();

		Player player = new Player();
		Debug debug = new Debug(window, player); // DEBUG
		double[] xpos = new double[1];
		double[] ypos = new double[1];
		double lastxpos = 0;
		double lastypos = 0;
		double deltax;
		double deltay;
		int lastAction=0;
		boolean firstRun = true;
		// glEnable(GL_CULL_FACE);
		glfwSetInputMode(window.getHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);

		float[] pos = { -1,-1,0,  1,-1,0,  1,1,0,  -1,1,0 };
		float[] uv  = { 0,0, 1,0, 1,1, 0,1 };
		int[]  idx  = { 0,1,2, 2,3,0 };
		Mesh crosshairMesh = new Mesh(pos, uv, idx);
		Texture crosshairTex = new Texture("textures/hud/crosshair.png"); // PNG à fond transparent
		
		// =================== BOUCLE DE JEU ===================
		while (!window.shouldClose()) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			// Fermer avec Echap
			if (glfwGetKey(window.getHandle(), GLFW_KEY_ESCAPE) == GLFW_PRESS) {
				glfwSetWindowShouldClose(window.getHandle(), true);
			}
			if (glfwGetKey(window.getHandle(), GLFW_KEY_A) == GLFW_PRESS)
				player.move(Direction.Left, world);
			if (glfwGetKey(window.getHandle(), GLFW_KEY_S) == GLFW_PRESS)
				player.move(Direction.Backward, world);
			if (glfwGetKey(window.getHandle(), GLFW_KEY_W) == GLFW_PRESS)
				player.move(Direction.Forward, world);
			if (glfwGetKey(window.getHandle(), GLFW_KEY_D) == GLFW_PRESS)
				player.move(Direction.Right, world);
			if (glfwGetKey(window.getHandle(), GLFW_KEY_SPACE) == GLFW_PRESS)
				player.move(Direction.Up, world);
			if (glfwGetKey(window.getHandle(), GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS)
				player.move(Direction.Down, world);
			if (glfwGetKey(window.getHandle(), GLFW_KEY_E) == GLFW_PRESS)
				player.changeBlock();
			if (glfwGetKey(window.getHandle(), GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS)
				player.run();
			if (glfwGetKey(window.getHandle(), GLFW_KEY_LEFT_SHIFT) != GLFW_PRESS)
				player.stopRun();
			lastAction+=1;
			if(((glfwGetMouseButton(window.getHandle(), GLFW_MOUSE_BUTTON_LEFT) != GLFW_PRESS)) && (glfwGetMouseButton(window.getHandle(), GLFW_MOUSE_BUTTON_RIGHT) != GLFW_PRESS)) {
				lastAction=50;
			}
			if(lastAction>=30) {
				if (glfwGetMouseButton(window.getHandle(), GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
					player.destroy(world);
					world.update=true;
					lastAction=0;
				}
				if (glfwGetMouseButton(window.getHandle(), GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS) {
					player.place(world);
					world.update=true;
					lastAction=0;
				}
			}


			// souris : rotation de la player
			glfwGetCursorPos(window.getHandle(), xpos, ypos);
			if (firstRun) {
				lastxpos = xpos[0];
				lastypos = ypos[0];
			} else if (lastxpos != xpos[0] || lastypos != ypos[0]) {
				deltax = xpos[0] - lastxpos;
				deltay = ypos[0] - lastypos;
				player.setPitch(deltay);
				player.setYaw(deltax);
				lastxpos = xpos[0];
				lastypos = ypos[0];
			}

			shader.bind();
			shader.setUniform("projection", player.getProjectionMatrix(window.getWidth(), window.getHeight()));
			shader.setUniform("view", player.getViewMatrix());

			player.applyGravity(world);

			// --- Dessin de tous les blocs ---
			world.RenderWorld(shader);

			debug.update(); // DEBUG
			// --- HUD ---
			glDisable(GL_DEPTH_TEST);   // toujours au-dessus de la 3D
			glDisable(GL_CULL_FACE);    // au cas où le quad serait culé
			glEnable(GL_BLEND);         // ⬅️ la transparence du PNG
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

			Matrix4f identite = new Matrix4f();
			shader.setUniform("projection", identite);
			shader.setUniform("view", identite);
			float aspect = (float) window.getWidth() / window.getHeight();
			shader.setUniform("model", new Matrix4f().scale(0.03f / aspect, 0.03f, 1f)); // taille + aspect

			glActiveTexture(GL_TEXTURE0);
			crosshairTex.bind();
			shader.setUniform("uTexture", 0);
			crosshairMesh.render();

			// on remet l'état pour le prochain frame 3D
			glDisable(GL_BLEND);
			glEnable(GL_CULL_FACE);
			glEnable(GL_DEPTH_TEST);
			shader.unbind();
			window.update();

			if (firstRun) {
				firstRun = false;
			}
			world.update=false;
		}

		// =================== NETTOYAGE ===================
		world.cleanup();
		shader.cleanup();
		window.cleanup();
	}
}
