package com.github.exiostorm.main;

import com.github.exiostorm.audio.JukeBox;
import com.github.exiostorm.graphics.*;
import com.github.exiostorm.graphics.gui.GUIElement;
import com.github.exiostorm.input.PlayerInputManager;
import com.github.exiostorm.input.PlayerInput;
import com.github.exiostorm.utils.DynamicFactory;
import com.github.exiostorm.utils.StateManager;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.github.exiostorm.main.EchoGame.gamePanel;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

//TODO setup GUI / Buttons / Settings, make some BGM, and add volume control to a settings menu. add mouse clicks to input.
//TODO add default inputmapper/gamestate so we can default to them if nothing is set instead of crashing.
//TODO will rename this from GamePanel to just Panel, or ProgramPanel, etc. don't need to directly tie it to only "gaming".
public class GamePanel {
    private ExecutorService executorService;
    private TextureAtlas atlas;
    private String atlasPath = "src/main/resources/atlas/atlas.json";
    private Shader shader;
    private String shaderVertexPath = "src/main/resources/shaders/vertex.glsl";
    private String shaderFragmentPath = "src/main/resources/shaders/fragment.glsl";
    private BatchRenderer renderer;
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    private String Name = "Application";
    private String DefaultState = "MainMenu";
    private String DefaultMapper = "MainMenu";
    private String iconPath = null;
    private String stateDirectory = null;
    private String inputmappersDirectory = null;
    private String currentState[] = { DefaultState, "gameStates" };
    private String currentMapper[] = { DefaultMapper, "inputMappers" };
    private Window window;
    public int WIDTH = 320;
    public int HEIGHT = 240;
    public static int SCALE = 1;
    public static int FPS = 30;
    private long targetTime = 1000; //don't know if we still need this, will do homework on what targetTime is.
    private float deltaTime;
    public StateManager<State> stateManager;
    public PlayerInputManager playerInputManager = new PlayerInputManager();
    // List to store all method references
    private List<Runnable> scheduledAssetLoading = new ArrayList<>();
    public Map<String, State> gameStates = new HashMap<>();
    public Map<String, State> inputMappers = new HashMap<>();
    //TODO[0] perhaps setup our atlas / renderer here somewhere? need it moved out of button class...
    public List<GUIElement> guiElements = new ArrayList<>();
    public GamePanel() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }
    }

    public void run() {
        init();
        long start;
        long elapsed;
        long wait;
        //TODO this counts the number of threads we can create and use, will later use for multithreading.
        System.out.println("HOW MANY THREADS WE HAVE : " + NUM_THREADS);
        executorService = Executors.newFixedThreadPool(NUM_THREADS - 1);
        // Main game loop
        //TODO important TODO~
        //TODO need to finish setting things up with executorService so we can have some things running faster than our frame rate and don't experience unnecessary lag.
        while (!window.shouldClose()) {
            start = System.nanoTime();
            update();  // Update the game logic
            render();  // Render the game
            window.swapBuffers(); // Swap the color buffers
            window.pollEvents();  // Poll for window events
            elapsed = System.nanoTime() - start;
            deltaTime = (elapsed / 1000000000f); // Convert to seconds

            wait = (targetTime / FPS) - elapsed / 1000000;
            if(wait < 0) wait = 5;

            try {
                Thread.sleep(wait);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Closing Game Window");
        glfwTerminate();
        shutdown();
    }

    private void init() {
        stateManager = new StateManager();
        window = new Window();
        window.setName(Name);
        window.setSize(WIDTH * SCALE, HEIGHT * SCALE);
        window.setFullScreen(false);
        window.init();
        playerInputManager.addPlayerInput(new PlayerInput(1, window.getWindow(), -1));
        JukeBox.Init();
        glfwMakeContextCurrent(window.getWindow());
        // Enable VSync (optional, can help with frame synchronization)
        //glfwSwapInterval(1);
        GL.createCapabilities();
        //Icon can only be set after Capabilities are created.
        setIcon();
        // Set the OpenGL viewport size
        //TODO might want to change the scale some other way... don't know exactly how or why just yet.
        glViewport(0, 0, WIDTH * SCALE, HEIGHT * SCALE);
        //start loading in our assets, such as our fonts.
        runScheduledAssets();
        // Print OpenGL version for debugging purposes
        System.out.println("OpenGL: " + glGetString(GL_VERSION));
        //TODO new code 2025/04/05
        shader = new Shader(shaderVertexPath, shaderFragmentPath);
        atlas = AtlasManager.atlas(atlasPath);
        renderer = new BatchRenderer(atlas, shader);

        // Load game states from assets directory (compiled or uncompiled)
        stateManager.loadStates(stateDirectory, gameStates, DynamicFactory.fromClass(State.class));
        stateManager.loadStates(inputmappersDirectory, inputMappers, DynamicFactory.fromClass(State.class));

        // Set initial state (e.g., MainMenu)
        currentMapper[0] = stateManager.setState(DefaultMapper, inputMappers, currentMapper);
        currentState[0] = stateManager.setState(DefaultState, gameStates, currentState);
    }

    public void update() {
        // Add your game logic and updates here
        stateManager.update(gameStates, currentState);
        stateManager.update(inputMappers, currentMapper);
    }

    private void render() {
        // Set up orthographic projection for 2D rendering
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, gamePanel.WIDTH, gamePanel.HEIGHT, 0, -1, 1); // 2D orthographic projection
        glMatrixMode(GL_MODELVIEW);
        // Clear the screen
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        // draw everything on plane 0?
        glDisable(GL_DEPTH_TEST);
        // Enable textures
        glEnable(GL_TEXTURE_2D);
        // Enable blending for transparent text rendering
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        renderer.begin();
        stateManager.render(gameStates, currentState);
        stateManager.render(inputMappers, currentMapper);
        renderer.end();
        // Disable blending after text rendering
        glDisable(GL_BLEND);
        // something with plane 0 / 3d rendering
        glEnable(GL_DEPTH_TEST);
        // Disable textures when done
        glDisable(GL_TEXTURE_2D);
    }

    private void shutdown() {
        JukeBox.clearHard();
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            System.out.println("Executor Service Shutdown Properly");
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    private void setIcon(){
        Texture icon = TextureManager.addTexture(iconPath);
        System.out.println(icon.getWidth()+"x"+icon.getHeight()+" icon dimensions");
        window.setIcon(icon.getByteBuffer((byte) 0), icon.getWidth(), icon.getHeight());
    }
    public void setIconPath(String path){
        iconPath = path;
    }
    public void setResolution(int width, int height, int scale){
        WIDTH = width;
        HEIGHT = height;
        SCALE = scale;
    }
    public void setName(String name){
        Name = name;
    }
    public void setDefaultState(String defaultState){
        DefaultState = defaultState;
    }
    public void setDefaultInputMapper(String defaultMappers){
        DefaultMapper = defaultMappers;
    }
    public void setFPS(int fps){
        FPS = fps;
    }
    public void setStateDirectory(String statesDirectory) {
        stateDirectory = statesDirectory;
    }
    public void setInputMappersDirectory(String mappersDirectory) {
        inputmappersDirectory = mappersDirectory;
    }
    // Method to add external methods to the list
    public void scheduleAssets(Runnable method) {
        scheduledAssetLoading.add(method);
    }
    // Method to run all scheduled methods at a specific time
    public void runScheduledAssets() {
        for (Runnable method : scheduledAssetLoading) {
            method.run();
        }
        // Clear the list once all methods have been executed
        scheduledAssetLoading.clear();
    }
    // Getters and Setters for currentState and currentMapper
    public String[] getCurrentState() {
        return currentState;
    }
    public void setCurrentState(String newState){
        currentState[0] = newState;
    }
    public String getCurrentMapper() {
        return currentMapper[0];
    }
    public Window getWindow() {
        return window;
    }

    public float getDeltaTime() {
        return deltaTime;
    }
    public String getAtlasPath() {
        return atlasPath;
    }

    public void setAtlasPath(String atlasPath) {
        this.atlasPath = atlasPath;
    }

    public String getShaderVertexPath() {
        return shaderVertexPath;
    }

    public void setShaderVertexPath(String shaderVertexPath) {
        this.shaderVertexPath = shaderVertexPath;
    }

    public String getShaderFragmentPath() {
        return shaderFragmentPath;
    }

    public void setShaderFragmentPath(String shaderFragmentPath) {
        this.shaderFragmentPath = shaderFragmentPath;
    }

    public BatchRenderer getRenderer() {
        return renderer;
    }

    public Shader getShader() {
        return shader;
    }

    public TextureAtlas getAtlas() {
        return atlas;
    }
}
