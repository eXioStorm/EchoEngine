package com.github.exiostorm.renderer.archive;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;

import com.github.exiostorm.renderer.Shader;
import lombok.Setter;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;



import com.github.exiostorm.utils.BufferUtils;
import lombok.Getter;

import static org.lwjgl.opengl.GL11.*;

public class Texture3 {
	@Getter
	private String path;
	@Getter
	private int width;
	@Getter
	private int height;
	@Getter
	private int texture;
	@Getter
	@Setter
	private Shader shader = null;
	@Getter
	@Setter
	private boolean useShader = false;
    private int[] pixelData = null; // Store pixel data for transparency checks
	private boolean[] transparencyMap = null;
	private ByteBuffer bufferData = null;
	private Map<String, Object> uniforms = new HashMap<>();
	
	public Texture3(String path) {
		this.path = path;
		texture = load(path);
	}
	public Texture3(Texture3 texture){
		this.texture = texture.texture;
		this.width = texture.getWidth();
		this.height = texture.getHeight();
		this.path = texture.getPath();
	}
	//TODO make it so when we get our pixel data the first time we're able to decide to save it immediately without re-fetching
	//private int load(String path, boolean savePixels, boolean saveTransparency) {}
	private int load(String path) {
		int[] pixels = null;
		try {
			BufferedImage image = ImageIO.read(new FileInputStream(path));
			width = image.getWidth();
			height = image.getHeight();
			pixels = new int[width * height];
			image.getRGB(0, 0, width, height, pixels, 0, width);
		} catch (IOException e) {
			e.printStackTrace();
		}

		int[] data = new int[width * height];
		for (int i = 0; i < width * height; i++) {

			int a = (pixels[i] >> 24) & 0xFF; // Alpha
			int r = (pixels[i] >> 16) & 0xFF; // Red
			int g = (pixels[i] >> 8) & 0xFF;  // Green
			int b = (pixels[i]) & 0xFF;       // Blue

			data[i] = (a << 24) | (b << 16) | (g << 8) | r;
		}

		int result = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, result);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, BufferUtils.createIntBuffer(data));
		glBindTexture(GL_TEXTURE_2D, 0);
		return result;
	}

	//TODO might need to separate Texture from rendering for use with shaders. (don't want to tie the rendering logic for shaders here because it will also be used for rendering text and other things.)
	public void drawImmediate(float x, float y, float width, float height) {
		this.drawImmediate(x, y, width, height, this.shader, this.useShader);
	}
	public void drawImmediate(float x, float y, float width, float height, Shader shader) {
		this.drawImmediate(x, y, width, height, shader, true);
	}
	public void drawImmediate(float x, float y, float width, float height, boolean usingShader) {
		this.drawImmediate(x, y, width, height, this.shader, usingShader);
	}
	//TODO do something here to allow use of supplied shaders
	public void drawImmediate(float x, float y, float width, float height, Shader usedShader, boolean usingShader) {
		if (usingShader) {
			if (usedShader!=null) { usedShader.enable(); } // Activate texture unit 0
		}
		if (usedShader!=null) { applyUniforms(usedShader); }
		//glActiveTexture(GL_TEXTURE0);
		bind(); // Bind the texture

		glBegin(GL_QUADS);
		glTexCoord2f(0, 0); glVertex2f(x, y); // Top-left
		glTexCoord2f(1, 0); glVertex2f(x + width, y); // Top-right
		glTexCoord2f(1, 1); glVertex2f(x + width, y + height); // Bottom-right
		glTexCoord2f(0, 1); glVertex2f(x, y + height); // Bottom-left
		glEnd();

		unbind();
		if (usedShader!=null) { usedShader.disable(); }
	}
	public ByteBuffer fetchBufferData(boolean save) {
		if (bufferData != null) {
			return bufferData;
		} else {
			glBindTexture(GL_TEXTURE_2D, texture);

			ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(width * height * 4).order(java.nio.ByteOrder.nativeOrder());
			glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

			glBindTexture(GL_TEXTURE_2D, 0);
			if (save) { this.bufferData = buffer; }
			return buffer;
		}
	}
	public boolean[] getTransparencyMap(boolean save) {
		if (transparencyMap != null) {
			return transparencyMap;
		} else {
			ByteBuffer buffer = fetchBufferData(false); // Use common method
			int[] pixelData = new int[width * height];
			boolean[] transparencyMap = new boolean[width * height];
			for (int i = 0; i < pixelData.length; i++) {
				int alpha = (pixelData[i] >> 24) & 0xFF; // Extract alpha value
				transparencyMap[i] = alpha == 0; // True if fully transparent
			}
			return transparencyMap;
		}
	}
	public int[] getPixelData(boolean save) {
		if (pixelData != null) {
			return pixelData;
		} else {
			ByteBuffer buffer = fetchBufferData(false); // Fetch raw texture data

			int[] fullPixelData = new int[width * height];

			for (int i = 0; i < width * height; i++) {
				int r = buffer.get(i * 4) & 0xFF;     // Red
				int g = buffer.get(i * 4 + 1) & 0xFF; // Green
				int b = buffer.get(i * 4 + 2) & 0xFF; // Blue
				int a = buffer.get(i * 4 + 3) & 0xFF; // Alpha

				fullPixelData[i] = (a << 24) | (r << 16) | (g << 8) | b;
			}

			if (save) {
				this.pixelData = fullPixelData; // Cache pixel data
			}

			return fullPixelData;
		}
	}
	public void clearPixelData(){
		pixelData = null;
	}
	public void clearBufferData(){
		bufferData = null;
	}
	public void addUniform(String name, Object value) {
		uniforms.put(name, value);
	}
	public void removeUniform(String name) {
		uniforms.remove(name);
	}
	public void applyUniforms(Shader shader) {
		for (Map.Entry<String, Object> entry : uniforms.entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();

			if (value instanceof Integer) {
				shader.setUniform(name, (Integer) value);
			} else if (value instanceof Float) {
				shader.setUniform(name, (Float) value);
			} else if (value instanceof Vector3f) {
				shader.setUniform(name, (Vector3f) value);
			} else if (value instanceof Matrix4f) {
				shader.setUniform(name, (Matrix4f) value);
			} else {
				System.err.println("Unsupported uniform type for: " + name);
			}
		}
	}
	public BufferedImage toBufferedImage() {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		if (pixelData == null) {
			image.setRGB(0, 0, width, height, getPixelData(false), 0, width);
		} else {
			image.setRGB(0, 0, width, height, pixelData, 0, width);
		}

		return image;
	}
	public void bind() {
		glBindTexture(GL_TEXTURE_2D, texture);
	}
	
	public void unbind() {
		glBindTexture(GL_TEXTURE_2D, 0);
	}
}
