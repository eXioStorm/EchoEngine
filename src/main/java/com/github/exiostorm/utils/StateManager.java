package com.github.exiostorm.utils;

import com.github.exiostorm.main.State;

import java.util.Map;

public class StateManager<T extends State> {
    // No need for hardcoded maps or state names in the class
    // These will be passed dynamically

    public void loadStates(String stateDirectory, Map<String, T> stateMap, DynamicFactory<T> factory) {
        if (stateMap == null || factory == null || stateDirectory == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        System.out.println("Attempting to load Resources from : "+stateDirectory);
        ResourceLoader.loadResources(stateDirectory, stateMap, factory);
    }

    public String setState(String stateName, Map<String, T> stateMap, String[] currentStateHolder) {
        // currentStateHolder is an array, so it can act like a mutable reference
        currentStateHolder[0] = stateName;
        if (stateMap.containsKey(stateName)) {
            stateMap.get(stateName).init(); // Initialize the state
            System.out.println(currentStateHolder[1]+" set to: " + stateName); // Logging state change
            return stateName;
        } else {
            System.err.println(currentStateHolder[1]+" not found: " + stateName);
            return currentStateHolder[0];
        }
    }

    public void update(Map<String, T> stateMap, String[] currentStateHolder) {
        if (stateMap.containsKey(currentStateHolder[0])) {
            stateMap.get(currentStateHolder[0]).update();
        }
    }

    public void render(Map<String, T> stateMap, String[] currentStateHolder) {
        if (stateMap.containsKey(currentStateHolder[0])) {
            stateMap.get(currentStateHolder[0]).render();
        }
    }
}
