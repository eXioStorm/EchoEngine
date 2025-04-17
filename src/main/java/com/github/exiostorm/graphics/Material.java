package com.github.exiostorm.graphics;


//TODO [0] 2025-04-12
// https://discord.com/channels/272761734820003841/1360240205625430097/1360240205625430097
// Confirmation that our solution is going to be quite unique from how other people handle this data.
// Although our solution might be complicated, hopefully it will allow our engine to be a LOT more flexible with managing things later.

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class Material {
    private String name;
    private Map<String, Integer> uniform1IMap = null;
    private Map<String, Float> uniform1FMap = null;
    private Map<String, Float[]> uniform2FMap = null;
    private Map<String, Vector3f> uniform3FMap = null;
    private Map<String, Matrix4f> uniformMatrix4FMap = null;

    /**
     * Don't directly create Materials from here, use the ShaderManager.
     * @param name
     */
    Material(String name) {
        this.name = name;
    }
    public Material setMap(String uniform, Integer value) {
        if (uniform1IMap == null) uniform1IMap = new HashMap<>();
        uniform1IMap.put(uniform, value);
        return this;
    }
    public Material setMap(String uniform, Float value) {
        if (uniform1FMap == null) uniform1FMap = new HashMap<>();
        uniform1FMap.put(uniform, value);
        return this;
    }
    public Material setMap(String uniform, Float value1, Float value2) {
        if (uniform2FMap == null) uniform2FMap = new HashMap<>();
        uniform2FMap.put(uniform, new Float[]{value1, value2});
        return this;
    }
    public Material setMap(String uniform, Vector3f value) {
        if (uniform3FMap == null) uniform3FMap = new HashMap<>();
        uniform3FMap.put(uniform, value);
        return this;
    }
    public Material setMap(String uniform, Matrix4f value) {
        if (uniformMatrix4FMap == null) uniformMatrix4FMap = new HashMap<>();
        uniformMatrix4FMap.put(uniform, value);
        return this;
    }
    public void applyUniforms(Shader shader) {
        if (uniform1IMap != null) {
            for (Map.Entry<String, Integer> entry : uniform1IMap.entrySet()) {
                shader.setUniform(entry.getKey(), entry.getValue());
            }
        }
        if (uniform1FMap != null) {
            for (Map.Entry<String, Float> entry : uniform1FMap.entrySet()) {
                shader.setUniform(entry.getKey(), entry.getValue());
            }
        }
        if (uniform2FMap != null) {
            for (Map.Entry<String, Float[]> entry : uniform2FMap.entrySet()) {
                shader.setUniform(entry.getKey(), entry.getValue()[0], entry.getValue()[1]);
            }
        }
        if (uniform3FMap != null) {
            for (Map.Entry<String, Vector3f> entry : uniform3FMap.entrySet()) {
                shader.setUniform(entry.getKey(), entry.getValue());
            }
        }
        if (uniformMatrix4FMap != null) {
            for (Map.Entry<String, Matrix4f> entry : uniformMatrix4FMap.entrySet()) {
                shader.setUniform(entry.getKey(), entry.getValue());
            }
        }
    }
    public String getName() {
        return name;
    }
}
