package com.github.exiostorm.graphics;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

public class Shader {
	public static Shader shader;
	public String name;
	boolean enabled = false;
	final int ID;
	FloatBuffer buffer = null;
	Map<String, Integer> locationCache = new HashMap<String, Integer>();
	
	public Shader(String name, String vertex, String fragment) {
		this.name = name;
		this.ID = ShaderManager.loadShader(name, vertex, fragment);
		ShaderManager.registerShader(name, this);
	}
	
	public int getUniform(String name) {
		if (locationCache.containsKey(name))
			return locationCache.get(name);
		
		int result = glGetUniformLocation(ID, name);
		if (result == -1) {
			System.err.println("Uniform '" + name + "' not found or not used in the shader program.");
			System.err.println("Could not find uniform variable '" + name + "'!");
		}
		else
			locationCache.put(name, result);
		return result;
	}

	public void setUniform(String name, int value) {
		if (!enabled) enable();
		int location = getUniform(name);
		if (location != -1) {
			glUniform1i(getUniform(name), value);
		}
	}

	public void setUniform(String name, float value) {
		if (!enabled) enable();
		int location = getUniform(name);
		if (location != -1) {
			glUniform1f(getUniform(name), value);
		}
	}
	public void setUniform(String name, float[] value) {
		if (!enabled) enable();
		int location = getUniform(name);
		if (location != -1) {
			glUniform1fv(getUniform(name), value);
		}
	}

	public void setUniform(String name, Vector2f value) {
		if (!enabled) enable();
		int location = getUniform(name);
		if (location != -1) {
			glUniform2f(getUniform(name), value.x, value.y);
		}
	}

	public void setUniform(String name, Vector2f[] vectors) {
		if (!enabled) enable();
		int location = getUniform(name);
		if (location != -1) {
			float[] data = new float[vectors.length * 2];
			for (int i = 0; i < vectors.length; i++) {
				if (vectors[i] != null) {
					data[i * 2] = vectors[i].x;
					data[i * 2 + 1] = vectors[i].y;
				}
			}
			glUniform2fv(getUniform(name), data);
		}
	}

	public void setUniform(String name, Vector3f[] vectors) {
		if (!enabled) enable();
		int location = getUniform(name);
		if (location != -1) {
			float[] data = new float[vectors.length * 3];
			for (int i = 0; i < vectors.length; i++) {
				if (vectors[i] != null) {
					data[i * 3] = vectors[i].x;
					data[i * 3 + 1] = vectors[i].y;
					data[i * 3 + 2] = vectors[i].z;
				}
			}
			glUniform3fv(getUniform(name), data);
		}
	}

	public void setUniform(String name, Vector3f vector) {
		if (!enabled) enable();
		int location = getUniform(name);
		if (location != -1) {
			glUniform3f(getUniform(name), vector.x, vector.y, vector.z);
		}
	}

	public void setUniform(String name, Matrix4f matrix) {
		if (!enabled) enable();
		int location = getUniform(name);
		if (location != -1) {
			if (buffer == null) {
				buffer = BufferUtils.createFloatBuffer(16);
			}
			glUniformMatrix4fv(getUniform(name), false, matrix.get(buffer));
		}
	}

	public void enable() {
		glUseProgram(ID);
		enabled = true;
	}
	
	public void disable() {
		glUseProgram(0);
		enabled = false;
	}
	public int getID() {
		return ID;
	}
}
