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
    Map<String, Integer> uniform1IMap = null;
    Map<String, Float> uniform1FMap = null;
    Map<String, Float[]> uniform2FMap = null;
    Map<String, Vector3f> uniform3FMap = null;
    Map<String, Matrix4f> uniformMatrix4FMap = null;
    public void Material() {
    }
    public void setMap(String uniform, Integer value) {
        if (uniform1IMap == null) uniform1IMap = new HashMap<>();
        uniform1IMap.put(uniform, value);
    }
    public void setMap(String uniform, Float value) {
        if (uniform1FMap == null) uniform1FMap = new HashMap<>();
        uniform1FMap.put(uniform, value);
    }
    public void setMap(String uniform, Float value1, Float value2) {
        if (uniform2FMap == null) uniform2FMap = new HashMap<>();
        uniform2FMap.put(uniform, new Float[]{value1, value2});
    }
    public void setMap(String uniform, Vector3f value) {
        if (uniform3FMap == null) uniform3FMap = new HashMap<>();
        uniform3FMap.put(uniform, value);
    }
    public void setMap(String uniform, Matrix4f value) {
        if (uniformMatrix4FMap == null) uniformMatrix4FMap = new HashMap<>();
        uniformMatrix4FMap.put(uniform, value);
    }
}
