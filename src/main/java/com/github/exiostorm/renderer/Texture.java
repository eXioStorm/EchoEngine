package com.github.exiostorm.renderer;

import com.github.exiostorm.utils.BufferUtils;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import static com.github.exiostorm.renderer.TextureGenerator.*;
import static org.lwjgl.opengl.GL11.*;

public class Texture {
	@Getter
    private final String path;
	@Getter
    private final int width;
	@Getter
    private final int height;
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

	public Texture(String path) {
		this.path = path;
		//TODO When we load an image it automatically saves a buffered image, it would be useful to delete it like a streamable resource? idk
		generateBufferedImage(this, true);
		this.width = this.bufferedImage.getWidth();
		this.height = this.bufferedImage.getHeight();
		this.transparencyMap = null;
		this.byteBuffer = null;
		TextureGenerator.addTexture(this);
		//TODO when we first load an image we need to save our width and height, so perhaps in our TextureAtlas we will set bufferedImage to false after we add it there.
		// this.bufferedImage = null;
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
}
