package com.github.exiostorm.graphics;

import static org.lwjgl.opengl.GL30.*;

import static com.github.exiostorm.main.EchoGame.gamePanel;

public class FrameBuffer {
    private int fboID;
    private int textureID;
    private int depthRboID;
    private int width, height;

    public FrameBuffer() {
        this.width = gamePanel.WIDTH;
        this.height = gamePanel.HEIGHT;
        initialize();
    }
    private void initialize() {
        // Create framebuffer
        fboID = glGenFramebuffers();
        depthRboID = glGenRenderbuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);

        // Create texture attachment
        textureID = glGenTextures();
        //TODO need to change this for configurable texture slot.
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Attach texture to framebuffer
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureID, 0);

        // Check if framebuffer is complete
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("ERROR: Framebuffer is not complete!");
        }

        glBindRenderbuffer(GL_RENDERBUFFER, depthRboID);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRboID);

        // Unbind
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
        //glViewport(0, 0, width, height);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        //glViewport(0, 0, width, height); // Reset viewport to window size
    }

    public int getTextureID() {
        return textureID;
    }

    public void cleanup() {
        glDeleteFramebuffers(fboID);
        glDeleteTextures(textureID);
    }
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
