package com.github.exiostorm.graphics;


//TODO [0] 2025-04-12
// https://discord.com/channels/272761734820003841/1360240205625430097/1360240205625430097
// Confirmation that our solution is going to be quite unique from how other people handle this data.
// Although our solution might be complicated, hopefully it will allow our engine to be a LOT more flexible with managing things later.

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.function.Supplier;

public class Material {
    private String name;
    //TODO [0] change logic to immediately initialize these, then we check "isEmpty()" instead of != null. should be faster, and easier to maintain, and follows more general practices.
    private Map<String, Integer> uniform1IMap = null;
    private Map<String, Float> uniform1FMap = null;
    private Map<String, Vector2f> uniform2FMap = null;
    private Map<String, Vector3f> uniform3FMap = null;
    private Map<String, Matrix4f> uniformMatrix4FMap = null;
    private Map<String, Supplier<?>> supplierMap = null;

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
    public Material setMap(String uniform, Vector2f value) {
        if (uniform2FMap == null) uniform2FMap = new HashMap<>();
        uniform2FMap.put(uniform, value);
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
    public Material setMap(String uniform, Supplier<?> value) {
        if (supplierMap == null) supplierMap = new HashMap<>();
        supplierMap.put(uniform, value);
        return this;
    }
    //TODO [0] figure out a way to avoid checking if maps are populated every frame...
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
            for (Map.Entry<String, Vector2f> entry : uniform2FMap.entrySet()) {
                shader.setUniform(entry.getKey(), entry.getValue());
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
        //TODO [0] honestly, probably best practice NOT to use this...
        // good thought exercise though.
        if (supplierMap != null) {
            for (Map.Entry<String, Supplier<?>> entry : supplierMap.entrySet()) {
                Object value = entry.getValue().get();
                if (value instanceof Integer) {
                    shader.setUniform(entry.getKey(), (int) entry.getValue().get());
                } else if (value instanceof Float) {
                    shader.setUniform(entry.getKey(), (float) entry.getValue().get());
                } else if (value instanceof Vector3f) {
                    shader.setUniform(entry.getKey(), (Vector3f) entry.getValue().get());
                } else if (value instanceof Matrix4f) {
                    shader.setUniform(entry.getKey(), (Matrix4f) entry.getValue().get());
                }
            }
        }
    }
    public String getName() {
        return name;
    }
}
