import com.github.exiostorm.audio.JukeBox;
import com.github.exiostorm.graphics.gui.Button;
import com.github.exiostorm.graphics.gui.GUIElement;

import com.github.exiostorm.main.State;
import com.github.exiostorm.graphics.*;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.github.exiostorm.main.EchoGame.gamePanel;
import static java.lang.Thread.sleep;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class MainMenu implements State {
    private Texture backgroundTexture;
    private Texture testTexture;
    private Texture patrickTexture;
    private FrameBuffer fbo;
    //TODO [0] need logic that passes the gamePanel from EchoGame when states are created so we can have easier reference to it that's independent from referencing EchoGame.
    BatchRenderer renderer = gamePanel.getRenderer();
    Shader exampleShader = gamePanel.getShader();
    Shader lightShader;
    Shader testShader;
    List<GUIElement> guiElements = gamePanel.guiElements;

    private int frameTester = 0;
    private boolean brighttester = false;


    @Override
    public void init() {
        //System.out.println("GLSL Version: " + glGetString(GL_SHADING_LANGUAGE_VERSION));
        initAudio();
        //TODO something here to select UIHandler? <- huh?
        initTextures();//TODO
        lightShader = new Shader("light",  "src/main/resources/Shaders/lights_vertex.glsl", "src/main/resources/Shaders/lights_fragment.glsl");
        //testShader = lightShader;
        testShader = exampleShader;
        //gamePanel.setShader(testShader);
        //initShaders();
        //new 2025/04/05
        initInterfaces();
        initializeMaterials();
        panelAtlas();
        initFBO();
    }

    @Override
    public void update() {
    }


    @Override
    public void render() {



        /*
        backgroundTexture.drawImmediate(0, 0, backgroundTexture.getWidth(), backgroundTexture.getHeight());

        //guiElements.getLast().render();
        //guiElements.getFirst().render();

        // Render all GUI elements


        // Render text on top of the background
        TextRenderer.renderText("Echo Engine Project", "arial", 0xfcf803, 0.9f, 0.65f * GamePanel.WIDTH, 0.1f * GamePanel.HEIGHT, 0.5f);  // Larger title font
        TextRenderer.renderText(String.valueOf(12345),"arial", 0xa134eb, 1.0f, 0.0f * GamePanel.WIDTH, 0.2f * GamePanel.HEIGHT, 0.5f);  // Larger title font
        TextRenderer.renderText(String.valueOf(frameTester++),"Inkfree", 0xfcf803, 1.0f, 1.5f * GamePanel.WIDTH, 1.8f * GamePanel.HEIGHT, 0.5f);  // Larger title font
        TextRenderer.renderText("test : \"ΩΩΩΩΩΩΩΩΩΩΩΩΩΩΩΩΩΩ\"<- shouldn't be empty","Inkfree", 0.0f * GamePanel.WIDTH, 0.3f * GamePanel.HEIGHT, 0.5f);  // Smaller menu font
        TextRenderer.renderText("Default font test!",0.0f * GamePanel.WIDTH, 0.4f * GamePanel.HEIGHT, 0.5f);
         */
        //TODO [!] start FBO here~
        renderer.begin(fbo);
        //renderer.begin();

        //TODO [!][20250805] weird material bleed bug if material isn't default? need to investigate for FBO.
        renderer.draw(backgroundTexture, gamePanel.getAtlas(), 0, 0, 0, exampleShader, ShaderManager.getMaterialFromMap("DEFAULT"));
        //renderer.draw(testTexture, 10, 10, exampleShader, false);
        //renderer.draw(patrickTexture, 200, 140, exampleShader, false);


        //TODO [0] update how this works so it's more similar to the other lines above and below?
        for (GUIElement element : guiElements) {
            element.render();
        }
        //renderer.draw(patrickTexture, gamePanel.getAtlas(), 20, 60, 0.000000000000000000000000000000000000000000001f, exampleShader, null);


        //graphics.draw(backgroundTexture, 0, 0, exampleShader, false);
        renderer.end();
        renderer.begin();
        //TODO [!]
        //renderer.draw(fbo, 0,0,0, lightShader, ShaderManager.getMaterialFromMap("lights"));
        renderer.draw(fbo, 0,0,0, testShader, ShaderManager.getMaterialFromMap("DEFAULT"));
        renderer.end();
        //exampleShader.enable();
        //exampleShader.setUniform("isFixedFunction", true); // For fixed-function
        //exampleShader.disable();



    }
    private void initShaders() {
        exampleShader = new Shader("test", "src/main/resources/Shaders/test_vertex.glsl", "src/main/resources/Shaders/test_fragment.glsl");
        /*
        exampleShader = new Shader("src/main/resources/Shaders/example_vertex.glsl", "src/main/resources/Shaders/example_fragment.glsl");
        exampleShader.enable();
        exampleShader.setUniform("textureSampler", 0); // Bind sampler2D to texture unit 0
        exampleShader.setUniform("brightness", 1.5f);  // Brighter texture
        exampleShader.setUniform("overlayColor", new Vector3f(0.1f, 0.8f, 0.2f));  // tint
        exampleShader.setUniform("isFixedFunction", true); // For fixed-function
        exampleShader.disable();*/
    }
    private void initTextures(){
        //TODO in the future we will have a method somewhere, maybe the TextureManager, to both add Textures, and create TextureAtlas? maybe...
        backgroundTexture = TextureManager.addTexture("src/main/resources/Backgrounds/storm2.png"); // Replace with your path
        testTexture = TextureManager.addTexture("src/main/resources/Backgrounds/test3.png");
        patrickTexture = TextureManager.addTexture("src/main/resources/HUD/funnybutton.png");
    }
    private void initAudio(){
        JukeBox.load("src/main/resources/SFX/menuoption.ogg", "buttons", "menuoption");
        JukeBox.load("src/main/resources/SFX/menuselect.ogg", "buttons", "menuselect");
    }
    private void initInterfaces(){
        // Create the button and add it to the GUI elements

        Button squareButton = new Button(gamePanel.WIDTH-testTexture.getWidth(), (float) (gamePanel.HEIGHT - testTexture.getHeight()) / 2, testTexture);
        squareButton.getTexture().setShader(exampleShader);
        squareButton.setOnHoverAction(button -> {
            if (!squareButton.isHovered()) {
                squareButton.hovered = true;
                JukeBox.play("menuoption", "effect", 1, true);
                //TODO [0] need to get materials setup~
                squareButton.setShaderMaterial(ShaderManager.getMaterialFromMap("GUIHIGHLIGHT"));
                System.out.println("Hovering over button: " + button);
            }
        });
        squareButton.setUnHoverAction(button -> {
            if (squareButton.isHovered()) {
                squareButton.hovered = false;
                JukeBox.play("menuoption", "effect", 1, true);
                //TODO 2025-04-12 got distracted and blamed our button logic for a flaw with our rendering logic. ignore the every frame triggers until we get more render logic done~
                squareButton.setShaderMaterial(null);
                //exampleShader.enable();
                //exampleShader.setUniform("brightness", 1.0f);
                //exampleShader.disable();
                System.out.println("Stopped hovering over button: " + button);
            }
        });
        squareButton.setOnClickAction(button -> {
            if ((System.currentTimeMillis()-squareButton.getLastPressed()) >= 1000L) {
                squareButton.setLastPressed(System.currentTimeMillis());
                JukeBox.play("menuselect", "effect", 1, false);
                //exampleShader.setUniform("brightness", (float)Math.random());
                //if (testShader==lightShader) {
                if (brighttester) {
                    //System.out.println("switching back to normal shader");
                    System.out.println("switching back to default mat value");
                    ShaderManager.setDefaultMaterial(ShaderManager.getMaterialFromMap("DEFAULT").setMap("brightness", 1.0f));
                    brighttester = false;
                    //renderer.checkShaderStatus(testShader);
                    //testShader = exampleShader;
                } else {
                    //System.out.println("switching back to light shader");
                    //renderer.checkShaderStatus(testShader);
                    //testShader = lightShader;
                    System.out.println("switching back to dimmed mat value");
                    ShaderManager.setDefaultMaterial(ShaderManager.getMaterialFromMap("DEFAULT").setMap("brightness", 0.8f));
                    brighttester = true;
                    //renderer.checkShaderStatus(testShader);
                }
                System.out.println("Clicked button : " + button + ", at : " + gamePanel.playerInputManager.getPlayer(0).getMousePosition()[0] + "x" + gamePanel.playerInputManager.getPlayer(0).getMousePosition()[1]);
            }
        });
        guiElements.add(squareButton);

        Button patrickButton = new Button((float) (gamePanel.WIDTH - patrickTexture.getHeight()) / 3, (float) (gamePanel.HEIGHT - patrickTexture.getHeight()) / 2, patrickTexture);
        patrickButton.getTexture().setShader(exampleShader);
        patrickButton.setOnHoverAction(button -> {
            if (!patrickButton.isHovered()) {
                JukeBox.play("menuoption", "effect", 1, true);
                patrickButton.setShaderMaterial(ShaderManager.getMaterialFromMap("GUIHIGHLIGHT"));
                System.out.println("Hovering over button: " + button);
            }
        });
        patrickButton.setUnHoverAction(button -> {
            if (patrickButton.isHovered()) {
                JukeBox.play("menuoption", "effect", 1, true);
                patrickButton.setShaderMaterial(null);
                /*
                exampleShader.enable();
                exampleShader.setUniform("brightness", 1.0f);
                exampleShader.disable();
                 */
                System.out.println("Stopped hovering over button: " + button);
            }
        });
        patrickButton.setOnClickAction(button -> {
            if ((System.currentTimeMillis()-patrickButton.getLastPressed()) >= 500L) {
                patrickButton.setLastPressed(System.currentTimeMillis());
                JukeBox.play("menuselect", "effect", 1, false);
                //TODO bug has something to do with this being the last rendered?
                if (brighttester) {
                    //System.out.println("switching back to normal shader");
                    System.out.println("switching back to default mat value");
                    //ShaderManager.setDefaultMaterial(ShaderManager.getMaterialFromMap("DEFAULT").setMap("brightness", 1.0f));
                    brighttester = false;
                    //renderer.checkShaderStatus(testShader);
                    testShader = exampleShader;
                } else {
                    //System.out.println("switching back to light shader");
                    //renderer.checkShaderStatus(testShader);
                    testShader = lightShader;
                    System.out.println("switching back to dimmed mat value");
                    //ShaderManager.setDefaultMaterial(ShaderManager.getMaterialFromMap("DEFAULT").setMap("brightness", 0.8f));
                    brighttester = true;
                    //renderer.checkShaderStatus(testShader);
                }
                //System.out.println("huh?");
                System.out.println("Clicked button : " + button + ", at : " + gamePanel.playerInputManager.getPlayer(0).getMousePosition()[0] + "x" + gamePanel.playerInputManager.getPlayer(0).getMousePosition()[1]);
            }
        });
        guiElements.add(patrickButton);

    }
    public void panelAtlas(){
        //TODO [0] bad logic here, need to fix.
        boolean recalculateAtlases = AtlasManager.addToAtlas(gamePanel.getAtlas(), "general", "general", backgroundTexture) ||
                AtlasManager.addToAtlas(gamePanel.getAtlas(), "general", "general", testTexture) ||
                AtlasManager.addToAtlas(gamePanel.getAtlas(), "general", "general", patrickTexture);
        if (!recalculateAtlases) AtlasManager.finalizeAtlasMaps(gamePanel.getAtlas());
        //TODO [0] can have logic to check if we need to reupload.
        AtlasManager.saveAtlasToGPU(gamePanel.getAtlas());
    }
    public void initFBO() {
        fbo = new FrameBuffer(GL_TEXTURE0);
    }
    /*public void createAtlas(String path) throws InterruptedException {
        long start = System.currentTimeMillis();
        File jsonFile = new File(path);
        if (!jsonFile.exists()) {
            atlas1 = AtlasManager.atlas(path);
            AtlasManager.addToAtlas(atlas1, "test1", "test1", backgroundTexture);
            AtlasManager.addToAtlas(atlas1, "test2", "test2", testTexture);
            AtlasManager.addToAtlas(atlas1, "test3", "test3", patrickTexture);
            AtlasManager.finalizeAtlasMaps(atlas1);
            try {
                AtlasManager.saveToJson(atlas1, path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("LOADING ATLAS FROM FILE!");
            try {
                atlas1 = AtlasManager.createAtlasFromFile(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        AtlasManager.saveAtlasToGPU(atlas1);
        renderer = new BatchRenderer();
    }*/
    public void initializeMaterials() {
        ShaderManager.setDefaultMaterial(ShaderManager.newMaterial("DEFAULT").setMap("brightness", 1.0f));
        ShaderManager.newMaterial("GUIHIGHLIGHT").setMap("brightness", 0.5f);
        ShaderManager.newMaterial("lights");/*
                .setMap("screenSize", new Vector2f(gamePanel.WIDTH, gamePanel.HEIGHT))
                .setMap("numLights", 3)
                .setMap("ambientLight", 0.2f);
        Vector2f[] lightPositions = new Vector2f[10];
        Vector3f[] lightColors = new Vector3f[10];
        float[] lightIntensities = new float[10];
        float[] lightRadii = new float[10];
        // Configure a campfire light
        lightPositions[0] = new Vector2f(0, 0); // Campfire position
        lightColors[0] = new Vector3f(1.0f, 0.7f, 0.3f); // Warm orange color
        lightIntensities[0] = 1.0f;
        lightRadii[0] = 0.2f;
        // Add a blue torch
        lightPositions[1] = new Vector2f(500, 400);
        lightColors[1] = new Vector3f(0.2f, 0.4f, 1.0f); // Blue color
        lightIntensities[1] = 1.2f;
        lightRadii[1] = 200.0f;
        // Add a green lantern
        lightPositions[2] = new Vector2f(100, 500);
        lightColors[2] = new Vector3f(0.1f, 0.8f, 0.2f); // Green color
        lightIntensities[2] = 1.0f;
        lightRadii[2] = 150.0f;
        ShaderManager.getMaterialFromMap("lights")
                .setMap("lightPositions", lightPositions)
                .setMap("lightColors", lightColors)
                .setMap("lightIntensities", lightIntensities)
                .setMap("lightRadii", lightRadii);*/
    }
}