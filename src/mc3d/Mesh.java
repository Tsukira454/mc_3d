package mc3d;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Un maillage : des sommets (positions + couleurs) relies par des indices,
 * le tout stocke sur la carte graphique (1 VAO + des VBOs + 1 EBO pour les indices).
 */
public class Mesh {

    private final int vaoId;
    private final List<Integer> buffers = new ArrayList<>(); // pour tout liberer a la fin
    private final int vertexCount;

    public Mesh(float[] positions, float[] uvs, int[] indices) {
        vertexCount = indices.length;

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        storeFloatAttribute(0, 3, positions); // attribut 0 = position (x, y, z)
        storeFloatAttribute(1, 2, uvs);    // attribut 1 = couleur  (r, g, b)

        // EBO : la liste des indices (quels sommets relier pour faire les triangles)
        int ebo = glGenBuffers();
        buffers.add(ebo);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glBindVertexArray(0);
    }

    /** Cree un VBO, y copie les donnees, et explique au VAO comment les lire. */
    private void storeFloatAttribute(int location, int size, float[] data) {
        int vbo = glGenBuffers();
        buffers.add(vbo);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        glVertexAttribPointer(location, size, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(location);
    }

    public void render() {
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void cleanup() {
        for (int b : buffers) {
            glDeleteBuffers(b);
        }
        glDeleteVertexArrays(vaoId);
    }
}
