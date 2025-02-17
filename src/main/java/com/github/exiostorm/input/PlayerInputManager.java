package com.github.exiostorm.input;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerInputManager {
    //private List<PlayerInput> playerInputs; // List to store PlayerInput instances
    private Map<Integer, PlayerInput> playerInputMap; // Store PlayerInput by playerId
    private int playerCount;

    public PlayerInputManager() {
        //playerInputs = new ArrayList<>(); // Initialize the list
        playerInputMap = new HashMap<>(); // Initialize the map
        playerCount = 0;
    }

    // Method to add a new player
    public void addPlayerInput(PlayerInput playerInput) {
        //playerInputs.add(playerInput); // Add the PlayerInput instance to the list
        //TODO double check that playerCount++ works correctly
        playerInputMap.put(playerCount++, playerInput); // Add the PlayerInput instance to the map with playerId as the key
    }

    // Method to update all players and retrieve their pressed keys
    public Map<Integer, List<Integer>> update() {
        Map<Integer, List<Integer>> inputMap = new HashMap<>(); // Map to store playerId and their pressed keys

        // Update each PlayerInput and collect pressed keys
        for (Map.Entry<Integer, PlayerInput> entry : playerInputMap.entrySet()) {
            PlayerInput playerInput = entry.getValue();
            Integer[] keys = playerInput.update(); // Get pressed keys for the current player
            List<Integer> pressedKeys = List.of(keys); // Convert the array to a List

            // Map the playerId to the pressed keys
            inputMap.put(entry.getKey(), pressedKeys);
        }

        return inputMap; // Return the map of playerId and pressed keys
    }

    // Optionally, you can add a method to retrieve a specific player's input
    public List<Integer> getPlayerInput(int playerId) {
        PlayerInput playerInput = playerInputMap.get(playerId); // Get the PlayerInput by playerId
        if (playerInput != null) {
            return List.of(playerInput.update()); // Return the pressed keys for the specific player
        }
        return new ArrayList<>(); // Return an empty list if the player ID is not found
    }
    public PlayerInput getPlayer(int playerId) {
        return playerInputMap.get(playerId); // Get the PlayerInput by playerId, or return null if not found
    }
}

