package com.github.exiostorm.graphics;

import com.github.exiostorm.utils.ResourceLoader;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glDeleteShader;

//TODO [0] 2025-04-12
// https://discord.com/channels/272761734820003841/1360240205625430097/1360240205625430097
// Confirmation that our solution is going to be quite unique from how other people handle this data.
// Although our solution might be complicated, hopefully it will allow our engine to be a LOT more flexible with managing shaders later. (such as preventing needing to re-write special use cases.)

//TODO with so little code here this class feels redundant...
public class ShaderManager {
    private static Material defaultMaterial = null;
    private static Map<String, Material> materialMap = new HashMap<>();
    private static Map<String, Shader> shaderMap = new HashMap<>();

    public static Material newMaterial(String name) {
        Material material = new Material(name);
        materialMap.put(name, material);
        return material;
    }

    /**
     * Convenience method to directly retrieve a specific Material from the map
     * @param name name of the material to retrieve from the map.
     * @return returns the specific material if it exists.
     */
    public static Material getMaterialFromMap(String name) {
        return materialMap.get(name);
    }
    public Map<String, Material> getMaterialMap() {
        return materialMap;
    }
    public static Shader getShaderFromMap(String name) {
        return shaderMap.get(name);
    }
    public Map<String, Shader> getShaderMap() {
        return shaderMap;
    }
    public static void registerShader(String name, Shader shader) {
        shaderMap.put(name, shader);
    }
    public static Material getDefaultMaterial() {
        return defaultMaterial;
    }
    public static void setDefaultMaterial(Material material) {
        defaultMaterial = material;
    }

    public static int loadShader(String name, String vertPath, String fragPath) {
        String vert = ResourceLoader.loadAsString(vertPath);
        String frag = ResourceLoader.loadAsString(fragPath);
        return createShader(name, vert, frag);
    }

    public static int createShader(String name, String vert, String frag) {
        int program = glCreateProgram();
        int vertID = glCreateShader(GL_VERTEX_SHADER);
        int fragID = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(vertID, vert);
        glShaderSource(fragID, frag);

        glCompileShader(vertID);
        if (glGetShaderi(vertID, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Vertex Shader Compilation Error: " + glGetShaderInfoLog(vertID, 1024));
            System.err.println(glGetShaderInfoLog(vertID));
            return -1;
        }

        glCompileShader(fragID);
        if (glGetShaderi(fragID, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Fragment Shader Compilation Error: " + glGetShaderInfoLog(fragID, 1024));
            System.err.println(glGetShaderInfoLog(fragID));
            return -1;
        }

        glAttachShader(program, vertID);
        glAttachShader(program, fragID);
        glLinkProgram(program);
        glValidateProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            int logLength = glGetProgrami(program, GL_INFO_LOG_LENGTH);
            if (logLength > 0) {
                String log = glGetProgramInfoLog(program, logLength);
                System.err.println("Program Linking Error: " + log);
            } else {
                System.err.println("Program Linking Error: No details available.");
            }
        }

        glDeleteShader(vertID);
        glDeleteShader(fragID);

        return program;
    }
}
