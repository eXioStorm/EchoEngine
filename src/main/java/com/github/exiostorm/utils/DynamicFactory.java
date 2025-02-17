package com.github.exiostorm.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DynamicFactory<T> {
    private final Map<String, Supplier<T>> registry = new HashMap<>();

    // Method to register a class with its name and corresponding supplier
    public void register(String name, Supplier<T> supplier) {
        //TODO our MainMenu is correctly being put here,
        registry.put(name, supplier);
    }

    // Method to create an instance based on the name
    public T create(String name) {
        Supplier<T> supplier = registry.get(name);
        if (supplier != null) {
            return supplier.get(); // Create the instance using the supplier
        }
        throw new IllegalArgumentException("No registered class for name: " + name);
    }
    public T createInstance(String className) {
        Supplier<T> constructor = registry.get(className);
        if (constructor != null) {
            System.out.println("instance \""+className+"\" created");
            return constructor.get();
        }
        return null; // Return null if the instance could not be created
    }

    // Static method to create a new DynamicFactory instance
    public static <T> DynamicFactory<T> fromClass(Class<T> baseClass) {
        return new DynamicFactory<>();
    }
}
