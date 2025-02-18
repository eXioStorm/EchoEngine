package com.github.exiostorm.utils;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.HashMap;

public class ResourceLoader {
	private ResourceLoader() {
	}

	public static String loadAsString(String file) {
		StringBuilder result = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String buffer = "";
			while ((buffer = reader.readLine()) != null) {
				result.append(buffer + '\n');
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result.toString();
	}
	//TODO now that we have this working method, we need to do like we did for FontRenderer and create a "core", and different types. this one here loads our results to a map, we need a version that will grab one instance.
	//TODO for grabbing single classes, we'll probably search for the first result matching a naming convention.
	// Method for loading compiled and uncompiled classes with generics
	public static <T> void loadResources(String stateDirectory, Map<String, T> map, DynamicFactory<T> factory) {
		File assetsDirFile = new File(stateDirectory);
		if (!assetsDirFile.exists() || !assetsDirFile.isDirectory()) {
			System.err.println("Assets directory not found: " + stateDirectory);
			return;
		}

		// Map to track which classes we've processed (avoiding duplicates)
		Map<String, File> processedClasses = new HashMap<>();

		File[] files = assetsDirFile.listFiles();
		if (files == null) return;

		try {
			URL dirURL = assetsDirFile.toURI().toURL();
			URLClassLoader classLoader = new URLClassLoader(new URL[]{dirURL}, ResourceLoader.class.getClassLoader());

			for (File file : files) {
				if (file.isDirectory()) continue;
				if (file.getName().endsWith(".java")) {
					// Ensure we prioritize .java files
					File classFile = getOrCompileJavaFile(file);
					if (classFile != null) {
						processedClasses.put(file.getName().replace(".java", ""), classFile);
					}
				}
			}

			// Now check for any remaining .class files that were not already processed
			for (File file : files) {
				if (file.getName().endsWith(".class")) {
					String className = file.getName().replace(".class", "");
					if (!processedClasses.containsKey(className)) {
						processedClasses.put(className, file);
					}
				}
			}

			// Register and load classes
			for (Map.Entry<String, File> entry : processedClasses.entrySet()) {
				String className = entry.getKey();
				System.out.println("Trying to load: " + className);

				factory.register(className, () -> {
					try {
						Class<?> clazz = classLoader.loadClass(className);
						return (T) clazz.getDeclaredConstructor().newInstance();
					} catch (Exception e) {
						System.err.println("Failed to create an instance of: " + className);
						e.printStackTrace();
						return null;
					}
				});

				T instance = factory.createInstance(className);
				if (instance != null) {
					System.out.println("Saving \"" + className + "\" to map");
					map.put(className, instance);
				} else {
					System.out.println("Instance \"" + className + "\" is null, unable to save to map.");
				}
			}

			classLoader.close();
		} catch (Exception e) {
			System.err.println("Error loading external classes from: " + stateDirectory);
			e.printStackTrace();
		}
	}

	// Method to manually add a class to the provided map using a DynamicFactory
	public <T> void addToMap(Map<String, T> map, String key, Class<T> clazz, DynamicFactory<T> factory) {
		if (map != null && key != null && factory != null && clazz != null) {
			T instance = factory.createInstance(key); // Create an instance using the factory
			if (instance != null) {
				map.put(key, instance); // Add the key-value pair to the map
				System.out.println("Added " + key + " to the map.");
			} else {
				System.out.println("Failed to create an instance for " + key);
			}
		} else {
			System.out.println("Invalid key, class, or factory. Cannot add to the map.");
		}
	}
	// Ensures .java files are compiled only if necessary
	private static File getOrCompileJavaFile(File javaFile) {
		File classFile = new File(javaFile.getParent(), javaFile.getName().replace(".java", ".class"));

		if (classFile.exists() && classFile.lastModified() > javaFile.lastModified()) {
			System.out.println("Skipping compilation for up-to-date file: " + javaFile.getName());
			return classFile;
		}

		System.out.println("Compiling: " + javaFile.getName());
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			System.err.println("No Java compiler available. Ensure you are running with a JDK, not a JRE.");
			return null;
		}

		int result = compiler.run(null, null, null, javaFile.getPath());
		if (result != 0) {
			System.err.println("Failed to compile: " + javaFile.getName());
			return null;
		}

		return classFile;
	}
}
