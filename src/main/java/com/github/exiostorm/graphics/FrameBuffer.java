package com.github.exiostorm.graphics;

import static org.lwjgl.opengl.GL30.*;

import static com.github.exiostorm.main.EchoGame.gamePanel;

public class FrameBuffer {
    private int fboID;
    private int textureID;
    private int textureUnit;
    private int depthRboID;
    private int width, height, scale;

    public FrameBuffer(int textureUnit) {
        this.textureUnit = textureUnit;
        this.width = gamePanel.WIDTH;
        this.height = gamePanel.HEIGHT;
        this.scale = gamePanel.SCALE;
        initialize();
    }
    private void initialize() {
        // Create framebuffer
        fboID = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);

        // Create texture attachment
        textureID = glGenTextures();
        glActiveTexture(textureUnit); // Activate the correct texture unit
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Create the texture storage
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width * scale, height * scale, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        // Attach texture to framebuffer
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureID, 0);

        // Create and attach depth buffer
        depthRboID = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthRboID);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width * scale, height * scale);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRboID);

        // Check if framebuffer is complete
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("ERROR: Framebuffer is not complete! Status: " + status);
        }

        // Unbind everything
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
        //glViewport(0, 0, width * scale, height * scale);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        //glViewport(0, 0, width * scale, height * scale); // Reset viewport to window size
    }

    public int getTextureID() {
        return textureID;
    }

    public void cleanup() {
        glDeleteRenderbuffers(depthRboID);
        glDeleteFramebuffers(fboID);
        glDeleteTextures(textureID);
    }
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
    public int getTextureUnit() {
        return textureUnit;
    }
}
