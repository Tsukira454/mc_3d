package mc3d;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

/**
 * Compile un vertex shader + un fragment shader et les lie en un "programme".
 * Gere aussi les uniforms (variables envoyees du Java vers les shaders, ex: les matrices).
 */
public class ShaderProgram {

    private final int programId;
    private final Map<String, Integer> uniforms = new HashMap<>();

    public ShaderProgram(String vertexSource, String fragmentSource) {
        int vs = compile(GL_VERTEX_SHADER, vertexSource);
        int fs = compile(GL_FRAGMENT_SHADER, fragmentSource);

        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Erreur de link du shader :\n" + glGetProgramInfoLog(programId));
        }
        // Une fois lies au programme, les shaders isoles ne servent plus
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private int compile(int type, String source) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);
        // Verification cruciale : sans ca, une faute dans le GLSL = ecran noir silencieux
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Erreur de compilation du shader :\n" + glGetShaderInfoLog(id));
        }
        return id;
    }

    /** Memorise l'emplacement d'un uniform pour pouvoir lui donner une valeur ensuite. */
    public void createUniform(String name) {
        int location = glGetUniformLocation(programId, name);
        if (location < 0) {
            System.err.println("Attention : uniform introuvable dans le shader : " + name);
        }
        uniforms.put(name, location);
    }

    /** Envoie une matrice 4x4 vers le shader. */
    public void setUniform(String name, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // JOML ecrit les 16 floats de la matrice dans un petit buffer natif temporaire
            glUniformMatrix4fv(uniforms.get(name), false, value.get(stack.mallocFloat(16)));
        }
    }

    /** Envoie un entier vers le shader (ex: le numero d'unite de texture d'un sampler). */
    public void setUniform(String name, int value) {
        glUniform1i(uniforms.get(name), value);
    }

    public void bind()    { glUseProgram(programId); }
    public void unbind()  { glUseProgram(0); }
    public void cleanup() { glDeleteProgram(programId); }
}
