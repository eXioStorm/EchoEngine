package com.github.exiostorm.main;

import com.github.exiostorm.renderer.TextRenderer;

public class EchoGame {
    public static GamePanel gamePanel;
    public static void main(String[] args) {
        gamePanel = new GamePanel();  // Create an instance of GamePanel
        setupGameConfiguration();
        gamePanel.run();  // Run the game loop
    }
    private static void setupGameConfiguration(){
        //TODO change scale to a float?
        gamePanel.setResolution(360,240,2);
        gamePanel.setFPS(60);
        gamePanel.setName("Echo Engine");
        //TODO this needs to run after gamePanel.run()
        gamePanel.setIconPath("src/main/resources/HUD/icon.png");
        gamePanel.setStateDirectory("src/main/resources/gameStates/");
        gamePanel.setInputMappersDirectory("src/main/resources/inputMappers/");
        gamePanel.setDefaultState("MainMenu");
        gamePanel.setDefaultInputMapper("MainMenuInputMapper");
        gamePanel.scheduleAssets(() -> TextRenderer.loadFontsFromDirectory("src/main/resources/Fonts"));
        TextRenderer.setDefaultFont("arial");
    }
}
