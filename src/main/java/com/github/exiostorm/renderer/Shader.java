package com.github.exiostorm.renderer;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.github.exiostorm.utils.ShaderUtils;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

public class Shader {
	public static final int VERTEX_ATTRIB = 0;
	public static final int TCOORD_ATTRIB = 1;
	
	public static Shader shader;
	
	private boolean enabled = false;
	@Getter
	private final int ID;
	FloatBuffer buffer = null;
	private Map<String, Integer> locationCache = new HashMap<String, Integer>();
	
	public Shader(String vertex, String fragment) {
		ID = ShaderUtils.load(vertex, fragment);
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
		glUniform1i(getUniform(name), value);
	}

	public void setUniform(String name, float value) {
		if (!enabled) enable();
		glUniform1f(getUniform(name), value);
	}

	public void setUniform(String name, float x, float y) {
		if (!enabled) enable();
		glUniform2f(getUniform(name), x, y);
	}

	public void setUniform(String name, Vector3f vector) {
		if (!enabled) enable();
		glUniform3f(getUniform(name), vector.x, vector.y, vector.z);
	}

	public void setUniform(String name, Matrix4f matrix) {
		if (!enabled) enable();//TODO										could be a memory problem here, we might want to permanently use this buffer instead of creating a new one every time?
		if (buffer==null) { buffer = BufferUtils.createFloatBuffer(16); }
		glUniformMatrix4fv(getUniform(name), false, matrix.get(buffer));
	}
	// New method to set a boolean uniform
	public void setUniform(String name, boolean value) {
		if (!enabled) enable();
		glUniform1i(getUniform(name), value ? 1 : 0); // Convert boolean to int (1 for true, 0 for false)
	}
	public void enable() {
		glUseProgram(ID);
		enabled = true;
	}
	
	public void disable() {
		glUseProgram(0);
		enabled = false;
	}
}
