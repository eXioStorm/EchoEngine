package com.github.exiostorm.graphics;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import com.github.exiostorm.utils.ShaderUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

public class Shader {
	public static final int VERTEX_ATTRIB = 0;
	public static final int TCOORD_ATTRIB = 1;
	
	public static Shader shader;
	private boolean enabled = false;
	private final int ID;
	FloatBuffer buffer = null;
	private Map<String, Integer> locationCache = new HashMap<String, Integer>();

	//TODO going to use these maps to store default values for shaders, hopefully allowing us to use one shader for multiple behaviours.
	private Map<String, Integer> glUniform1iDefaults;
	private Map<String, Float> glUniform1fDefaults;
	private Map<String, Float[]> glUniform2fDefaults;
	//TODO confused on these ones, but will roll with it until I learn more.
	private Map<String, Vector3f> glUniform3fDefaults;
	private Map<String, Matrix4f> glUniformMatrix4fvDefaults;
	
	public Shader(String vertex, String fragment) {
		this.ID = ShaderUtils.load(vertex, fragment);
		this.glUniform1iDefaults = new HashMap<>();
		this.glUniform1fDefaults = new HashMap<>();
		this.glUniform2fDefaults = new HashMap<>();
		this.glUniform3fDefaults = new HashMap<>();
		this.glUniformMatrix4fvDefaults = new HashMap<>();
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
		if (glUniform1iDefaults.containsKey(name)) {
			glUniform1i(getUniform(name), value);
		} else {
			setDefaultUniform(name, value);
		}
		}
	}

	public void setUniform(String name, float value) {
		if (!enabled) enable();
		int location = getUniform(name);
		if (location != -1) {
			if (glUniform1fDefaults.containsKey(name)) {
				glUniform1f(getUniform(name), value);
			} else {
				setDefaultUniform(name, value);
			}
		}
	}

	public void setUniform(String name, float x, float y) {
		if (!enabled) enable();
		int location = getUniform(name);
		if (location != -1) {
			if (glUniform2fDefaults.containsKey(name)) {
				glUniform2f(getUniform(name), x, y);
			} else {
				setDefaultUniform(name, x, y);
			}
		}
	}

	public void setUniform(String name, Vector3f vector) {
		if (!enabled) enable();
		int location = getUniform(name);
		if (location != -1) {
			if (glUniform3fDefaults.containsKey(name)) {
				glUniform3f(getUniform(name), vector.x, vector.y, vector.z);
			} else {
				setDefaultUniform(name, vector);
			}
		}
	}

	public void setUniform(String name, Matrix4f matrix) {
		if (!enabled) enable();
		int location = getUniform(name);
		if (location != -1) {
			if (buffer == null) {
				buffer = BufferUtils.createFloatBuffer(16);
			}
			if (glUniformMatrix4fvDefaults.containsKey(name)) {
				glUniformMatrix4fv(getUniform(name), false, matrix.get(buffer));
			} else {
				setDefaultUniform(name, matrix);
			}
		}
	}
	//TODO might delete this one? don't see why we can't just use 1i
	// Method to set a boolean uniform
	public void setUniform(String name, boolean value) {
		if (!enabled) enable();
		int location = getUniform(name);
		if (location != -1) {
			glUniform1i(getUniform(name), value ? 1 : 0); // Convert boolean to int (1 for true, 0 for false)
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

	// Methods to set default values
	public void setDefaultUniform(String name, int value) {
		glUniform1iDefaults.put(name, value);
		setUniform(name, value); // Also set it immediately
	}

	public void setDefaultUniform(String name, float value) {
		glUniform1fDefaults.put(name, value);
		setUniform(name, value);
	}

	public void setDefaultUniform(String name, float x, float y) {
		glUniform2fDefaults.put(name, new Float[]{x, y});
		setUniform(name, x, y);
	}

	public void setDefaultUniform(String name, Vector3f vector) {
		glUniform3fDefaults.put(name, vector);
		setUniform(name, vector);
	}

	public void setDefaultUniform(String name, Matrix4f matrix) {
		glUniformMatrix4fvDefaults.put(name, matrix);
		setUniform(name, matrix);
	}

	public Map<String, Integer> getGlUniform1iDefaults() {
		return glUniform1iDefaults;
	}

	public Map<String, Float> getGlUniform1fDefaults() {
		return glUniform1fDefaults;
	}

	public Map<String, Float[]> getGlUniform2fDefaults() {
		return glUniform2fDefaults;
	}

	public Map<String, Vector3f> getGlUniform3fDefaults() {
		return glUniform3fDefaults;
	}

	public Map<String, Matrix4f> getGlUniformMatrix4fvDefaults() {
		return glUniformMatrix4fvDefaults;
	}

	public void resetAllUniforms() {
		for (Map.Entry<String, Integer> entry : glUniform1iDefaults.entrySet()) {
			setUniform(entry.getKey(), entry.getValue());
		}

		for (Map.Entry<String, Float> entry : glUniform1fDefaults.entrySet()) {
			setUniform(entry.getKey(), entry.getValue());
		}

		for (Map.Entry<String, Float[]> entry : glUniform2fDefaults.entrySet()) {
			Float[] vec = entry.getValue();
			setUniform(entry.getKey(), vec[0], vec[1]);
		}

		for (Map.Entry<String, Vector3f> entry : glUniform3fDefaults.entrySet()) {
			setUniform(entry.getKey(), entry.getValue());
		}

		for (Map.Entry<String, Matrix4f> entry : glUniformMatrix4fvDefaults.entrySet()) {
			setUniform(entry.getKey(), entry.getValue());
		}
	}
}
