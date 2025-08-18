package com.github.exiostorm.main;

import com.github.exiostorm.graphics.archive.TextRenderer;

public class EchoGame {
    public static GamePanel gamePanel;
    public static void main(String[] args) {
        gamePanel = new GamePanel();  // Create an instance of GamePanel
        setupGameConfiguration();
        gamePanel.run();  // Run the game loop
    }
    //TODO Improper directories once project is built, need better build or directory path.
    private static void setupGameConfiguration(){
        gamePanel.setName("Echo Engine");
        gamePanel.setIconPath("src/main/resources/HUD/icon.png");
        gamePanel.setAtlasPath("src/main/resources/atlas/atlas.json");
        //TODO [!] need to setup a default material json config for this shader~ (instead of leaving it for initialization)
        gamePanel.setShaderVertexPath("src/main/resources/Shaders/test_vertex.glsl");
        gamePanel.setShaderFragmentPath("src/main/resources/Shaders/test_fragment.glsl");
        //TODO change scale to a float?
        gamePanel.setResolution(360,240,2);
        gamePanel.setFPS(60);
        //TODO this needs to run after gamePanel.run()
        gamePanel.setStateDirectory("src/main/resources/gameStates/");
        gamePanel.setInputMappersDirectory("src/main/resources/inputMappers/");
        gamePanel.setDefaultState("MainMenu");
        gamePanel.setDefaultInputMapper("MainMenuInputMapper");
        //TODO old immediate mode rendering method, needs updated.
        gamePanel.scheduleAssets(() -> TextRenderer.loadFontsFromDirectory("src/main/resources/Fonts"));
        TextRenderer.setDefaultFont("arial");
    }
}
