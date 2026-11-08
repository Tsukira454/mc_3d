package mc3d;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.lwjgl.opengl.GL;

/**
 * Gere la fenetre GLFW et le contexte OpenGL.
 * Responsabilites : ouvrir/fermer la fenetre, le double-buffering, le depth test.
 */
public class Window {

    private long handle;      // identifiant natif de la fenetre
    private int width;
    private int height;
    private final String title;

    public Window(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public void init() {
        if (!glfwInit()) {
            throw new IllegalStateException("Impossible d'initialiser GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);     // cachee le temps de la preparer
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        // On exige OpenGL 3.3 "core" (le moderne, avec shaders obligatoires)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        handle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (handle == NULL) {
            throw new RuntimeException("Impossible de creer la fenetre GLFW");
        }

        // Si la fenetre est redimensionnee, on adapte la zone de dessin (viewport)
        glfwSetFramebufferSizeCallback(handle, (win, w, h) -> {
            this.width = w;
            this.height = h;
            glViewport(0, 0, w, h);
        });

        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1);          // v-sync (limite ~60 fps, evite de chauffer le GPU)
        glfwShowWindow(handle);

        GL.createCapabilities();       // active OpenGL cote Java (obligatoire avant tout glXxx)

        glClearColor(0.45f, 0.65f, 0.95f, 1.0f); // fond bleu ciel
        glEnable(GL_DEPTH_TEST);       // ESSENTIEL en 3D : cache ce qui est derriere
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    /** A appeler en fin de chaque image : affiche le rendu + traite les evenements. */
    public void update() {
        glfwSwapBuffers(handle);
        glfwPollEvents();
    }

    public void cleanup() {
        glfwDestroyWindow(handle);
        glfwTerminate();
    }

    public long getHandle() { return handle; }
    public int getWidth()   { return width; }
    public int getHeight()  { return height; }
}
