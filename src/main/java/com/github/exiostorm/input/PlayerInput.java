package com.github.exiostorm.input;

import com.github.exiostorm.main.GamePanel;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.exiostorm.main.EchoGame.gamePanel;
import static org.lwjgl.glfw.GLFW.*;

//TODO need to setup something eventually at some point for midi device support.
public class PlayerInput {
    private int playerId;
    private long window;  // Reference to the GLFW window
    private Map<Integer, Boolean> keyState;  // Stores the state of each key
    private double mouseX, mouseY;  // Stores mouse position
    private Map<Integer, Boolean> mouseButtonState; // Stores mouse button states
    private int gamepadId; // Gamepad ID, -1 if no gamepad

    public PlayerInput(int playerId, long window, int gamepadId) {
        this.playerId = playerId;
        this.window = window;
        this.gamepadId = gamepadId;
        keyState = new HashMap<>();
        mouseButtonState = new HashMap<>();
        setupKeyCallback();
        setupMouseCallback();
    }
    // Mouse callback setup
    // Set up the GLFW mouse callback to store mouse button states
    private void setupMouseCallback() {
        GLFW.glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (action == GLFW_PRESS) {
                mouseButtonState.put(button, true);
            } else if (action == GLFW_RELEASE) {
                mouseButtonState.put(button, false);
            }
        });

        // Set up cursor position callback to update mouse position
        GLFW.glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            this.mouseX = xpos;
            this.mouseY = ypos;
        });
    }

    public double[] getMousePosition() {
        //TODO change how this is retrieved, move the math for GamePanel to GamePanel. that way this code remains modular.
        return new double[]{ mouseX / gamePanel.SCALE, mouseY / gamePanel.SCALE }; // Return current mouse position
    }

    public boolean isMouseButtonPressed(int button) {
        return mouseButtonState.getOrDefault(button, false); // Return button state
    }
    // Method to check if a key is currently pressed (keyboard or gamepad)
    private boolean isKeyPressed(int key) {
        if (gamepadId!=-1) {
            // Check if the gamepad button is pressed
            if (glfwJoystickPresent(gamepadId)) {
                GLFWGamepadState state = GLFWGamepadState.calloc();
                if (glfwGetGamepadState(gamepadId, state)) {
                    return state.buttons(key) == GLFW_PRESS;
                }
            }
            return false;
        }
        // For keyboard
        return glfwGetKey(window, key) == GLFW_PRESS;
    }

    // Set up the GLFW key callback to store key states
    private void setupKeyCallback() {
        if (gamepadId==-1) {
            GLFW.glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
                // Update key state based on GLFW action
                if (action == GLFW_PRESS) {
                    keyState.put(key, true);
                } else if (action == GLFW.GLFW_RELEASE) {
                    keyState.put(key, false);
                }
            });
        }
    }

    // Update method to process inputs for this specific player
    public Integer[] update() {
        List<Integer> pressedKeys = new ArrayList<>();
        if (gamepadId!=-1 && glfwJoystickPresent(gamepadId)) {
            // Handle gamepad inputs
            GLFWGamepadState state = GLFWGamepadState.calloc();
            if (glfwGetGamepadState(gamepadId, state)) {
                for (int button = 0; button < state.buttons().limit(); button++) {
                    if (state.buttons(button) == GLFW_PRESS) {
                        pressedKeys.add(button);  // Add pressed gamepad button
                    }
                }
            }
        } else {
            // Handle keyboard inputs
            for (int key = GLFW_KEY_SPACE; key <= GLFW_KEY_LAST; key++) {
                if (glfwGetKey(window, key) == GLFW_PRESS) {
                    pressedKeys.add(key); // Collect all pressed keys
                }
            }
            for (int button = GLFW_MOUSE_BUTTON_1; button <= GLFW_MOUSE_BUTTON_LAST; button++) {
                if (isMouseButtonPressed(button)) {
                    pressedKeys.add(button); // Collect all pressed mouse buttons
                }
            }
        }
        return pressedKeys.toArray(new Integer[0]); // Return an array of pressed keys
    }
    // Method to assign a new gamepad to the player
    public void setGamepad(int gamepadId) {
        this.gamepadId = gamepadId;
    }
    public int getPlayerId() {
        return playerId; // Return the player ID
    }
}