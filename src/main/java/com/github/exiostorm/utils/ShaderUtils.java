package com.github.exiostorm.utils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
public class ShaderUtils {
	private ShaderUtils() {
	}
	
	public static int load(String vertPath, String fragPath) {
		String vert = ResourceLoader.loadAsString(vertPath);
		String frag = ResourceLoader.loadAsString(fragPath);
		return create(vert, frag);
	}
	
	public static int create(String vert, String frag) {
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
