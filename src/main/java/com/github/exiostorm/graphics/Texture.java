package com.github.exiostorm.graphics;

import lombok.Getter;
import lombok.Setter;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static com.github.exiostorm.graphics.TextureManager.*;

public class Texture {
	@Getter
    private final String path;
    private int width;
    private int height;
	@Getter
	@Setter
	private Shader shader = null;
	@Getter
	@Setter
	private boolean[] transparencyMap;
	@Getter
	@Setter
	private ByteBuffer byteBuffer;
	@Getter
	@Setter
	private BufferedImage bufferedImage;
	private String lombokString = "Placeholder to suppress lombok message : ";
	private String packge = "com.github.exiostorm.graphics.Texture.";

	/**
	 * Set as package private, so we force the use of TextureManager. Do not use directly.
	 * @param path path of the image file.
	 */
	Texture(String path) {
		this.path = path;
		this.transparencyMap = null;
		this.byteBuffer = null;
		getOrGenerateDimensions(this);
	}
	public BufferedImage getBufferedImage(boolean save) {
        if (this.bufferedImage == null) {
            return generateBufferedImage(this, save);
        }
        return bufferedImage;
    }
	public boolean[] getTransparencyMap(boolean save) {
        if (this.transparencyMap == null) {
            return generateTransparencyMap(this, save);
        }
        return this.transparencyMap;
    }
	public ByteBuffer getByteBuffer(byte saveFlag) {
		if (this.byteBuffer == null) {
			return generateByteBuffer(this, saveFlag);
		}
		return byteBuffer;
	}
	//TODO getters/setters
	public int getWidth() {
		if (this.width < 0) { System.out.println(lombokString + packge + "getWidth()"); }
		return this.width;
	}
	public void setWidth(int width) {
		if (this.width < 0) { System.out.println(lombokString + packge + "setWidth()"); }
		this.width = width;
	}
	public int getHeight() {
		if (this.height < 0) { System.out.println(lombokString + packge + "getHeight()"); }
		return this.height;
	}
	public void setHeight(int height) {
		if (this.height < 0) { System.out.println(lombokString + packge + "setHeight()"); }
		this.height = height;
	}
}
