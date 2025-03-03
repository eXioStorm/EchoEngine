package com.github.exiostorm.graphics;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

public class Window {
	private long window;
	private int width = 960;
	private int height = 540;
	private String name = "NULL";
	ByteBuffer icon = null;

	private boolean fullScreen;

	public Window() {
	}

	public void init() {
		glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GL_FALSE);
		window = glfwCreateWindow(width, height, name, fullScreen ? glfwGetPrimaryMonitor() : 0, 0);
		if (window == NULL) throw new RuntimeException("Failed to create Window");
		glfwSetWindowPos(window, (getMonitorWidth() - width) / 2, (getMonitorHeight() - height) / 2);
		glfwMakeContextCurrent(window);
		//glfwSwapInterval(1); //Vsync?
		glfwShowWindow(window);
	}

	public long getWindow() {
		return window;
	}

	public int getMonitorWidth() {
		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		return vidmode.width();
	}

	public int getMonitorHeight() {
		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		return vidmode.height();
	}

	public void setFullScreen(boolean full) {
		fullScreen = full;
		if (full) setSize(getMonitorWidth(), getMonitorHeight());
	}

	public boolean getFullScreen() {
		return fullScreen;
	}

	public boolean shouldClose() {
		return glfwWindowShouldClose(window);
	}

	public void pollEvents() {
		glfwPollEvents();
	}

	public void swapBuffers() {
		glfwSwapBuffers(window);
	}

	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void setIcon(ByteBuffer icon, int width, int height) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			GLFWImage.Buffer iconBuffer = GLFWImage.malloc(1, stack);
			iconBuffer.position(0)
					.width(width)
					.height(height)
					.pixels(icon);

			// Set the window icon
			glfwSetWindowIcon(window, iconBuffer);
		}
    }
}
