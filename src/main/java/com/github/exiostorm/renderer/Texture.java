package com.github.exiostorm.renderer;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.github.exiostorm.renderer.TextureManager.*;

public class Texture {
	@Getter
    private final String path;
	@Getter
    private int width;
	@Getter
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

	public Texture(String path) {
		this.path = path;
		getOrGenerateDimensions(this.width, this.height);
		this.transparencyMap = null;
		this.byteBuffer = null;
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
	//TODO move this method somewhere else... too many imports for using it here.
	private void getOrGenerateDimensions(int width, int height) {
		String jsonPath = this.path.substring(0, this.path.lastIndexOf('.')) + ".json";
		File jsonFile = new File(jsonPath);
		if (jsonFile.exists()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
				StringBuilder jsonContent = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					jsonContent.append(line);
				}
				JSONObject jsonObject = new JSONObject(jsonContent.toString());
				this.width = jsonObject.getInt("width");
				this.height = jsonObject.getInt("height");
				System.out.println("JSON values read. Width: " + this.width + ", Height: " + this.height);
			} catch (IOException e) {
				System.err.println("Error reading JSON file: " + e.getMessage());
				generateBufferedImage(this, true);
				this.width = this.bufferedImage.getWidth();
				this.height = this.bufferedImage.getHeight();
			}
		} else {
			generateBufferedImage(this, true);
			this.width = this.bufferedImage.getWidth();
			this.height = this.bufferedImage.getHeight();
		}
	}
}
