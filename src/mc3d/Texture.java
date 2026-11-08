package mc3d;

import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.opengl.GL11.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

/** Une image chargee sur la carte graphique. A charger UNE fois, puis reutiliser. */
public class Texture {
    int id;

    public Texture(String chemin) {
        stbi_set_flip_vertically_on_load(true);
        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);

        ByteBuffer pixels = stbi_load(chemin, w, h, channels, 4);
        // Garde-fou : si le chemin est faux, stb renvoie null -> message clair au lieu d'un crash obscur
        if (pixels == null) {
            throw new RuntimeException("Impossible de charger la texture : " + chemin
                    + " (" + stbi_failure_reason() + ")");
        }

        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w.get(0), h.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        stbi_image_free(pixels);
    }

    public void bind()    { glBindTexture(GL_TEXTURE_2D, id); }
    public void cleanup() { glDeleteTextures(id); }
}
