package com.github.exiostorm.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class BufferUtils {
	private BufferUtils() {
	}

	public static ByteBuffer createByteBuffer(byte[] array) {
		ByteBuffer result = ByteBuffer.allocateDirect(array.length).order(ByteOrder.nativeOrder());
		result.put(array).flip();
		return result;
	}

	public static FloatBuffer createFloatBuffer(float[] array) {
		FloatBuffer result = ByteBuffer.allocateDirect(array.length << 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
		result.put(array).flip();
		return result;
	}

	public static IntBuffer createIntBuffer(int[] array) {
		IntBuffer result = ByteBuffer.allocateDirect(array.length << 2).order(ByteOrder.nativeOrder()).asIntBuffer();
		result.put(array).flip();
		return result;
	}
	public static BufferedImage floatToBufferedImage(float[] floatData, int width, int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int index = (y * width + x) * 3;
				int r = (int) (floatData[index] * 255);
				int g = (int) (floatData[index + 1] * 255);
				int b = (int) (floatData[index + 2] * 255);
				int argb = (255 << 24) | (r << 16) | (g << 8) | b;
				image.setRGB(x, y, argb);
			}
		}
		return image;
	}
	public static ByteBuffer imageToByteBuffer(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int rgb = image.getRGB(x, y);
				buffer.put((byte) ((rgb >> 16) & 0xFF)); // Red
				buffer.put((byte) ((rgb >> 8) & 0xFF));  // Green
				buffer.put((byte) (rgb & 0xFF));         // Blue
				buffer.put((byte) ((rgb >> 24) & 0xFF)); // Alpha
			}
		}
		buffer.flip();
		return buffer;
	}
	public static boolean saveImage(BufferedImage image, File outputFile) {
		try {
			// Ensure parent directories exist
			File parentDir = outputFile.getParentFile();
			if (parentDir != null && !parentDir.exists()) {
				parentDir.mkdirs(); // Create directory if necessary
			}

			// Save the image
			return ImageIO.write(image, "PNG", outputFile);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}