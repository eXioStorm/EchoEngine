import com.github.exiostorm.audio.JukeBox;
import com.github.exiostorm.graphics.gui.Button;
import com.github.exiostorm.graphics.gui.GUIElement;

import com.github.exiostorm.main.State;
import com.github.exiostorm.graphics.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.github.exiostorm.main.EchoGame.gamePanel;
import static java.lang.Thread.sleep;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class MainMenu implements State {
    private Texture backgroundTexture;
    private Texture testTexture;
    private Texture patrickTexture;
    //TODO [0] need logic that passes the gamePanel from EchoGame when states are created so we can have easier reference to it that's independent from referencing EchoGame.
    BatchRenderer renderer = gamePanel.getRenderer();
    Shader exampleShader = gamePanel.getShader();
    TextureAtlas atlas1 = gamePanel.getAtlas();
    List<GUIElement> guiElements = gamePanel.guiElements;

    private int frameTester = 0;


    @Override
    public void init() {
        //System.out.println("GLSL Version: " + glGetString(GL_SHADING_LANGUAGE_VERSION));
        initAudio();
        //TODO something here to select UIHandler? <- huh?
        initTextures();//TODO
        //initShaders();
        //new 2025/04/05
        initInterfaces();
        panelAtlas();

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

        //renderer.begin();

        renderer.draw(backgroundTexture, 0, 0, exampleShader, null);
        //renderer.draw(testTexture, 10, 10, exampleShader, false);
        //renderer.draw(patrickTexture, 200, 140, exampleShader, false);

        for (GUIElement element : guiElements) {
            element.render();
        }

        //graphics.draw(backgroundTexture, 0, 0, exampleShader, false);

        //renderer.end();


        //exampleShader.enable();
        //exampleShader.setUniform("isFixedFunction", true); // For fixed-function
        //exampleShader.disable();



    }
    private void initShaders() {
        exampleShader = new Shader("src/main/resources/Shaders/test_vertex.glsl", "src/main/resources/Shaders/test_fragment.glsl");
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
                squareButton.setShaderModifier(shader -> shader.setUniform("brightness", (float)Math.random()));
                System.out.println("Hovering over button: " + button);
            }
        });
        squareButton.setUnHoverAction(button -> {
            if (squareButton.isHovered()) {
                squareButton.hovered = false;
                JukeBox.play("menuoption", "effect", 1, true);
                //TODO 2025-04-12 got distracted and blamed our button logic for a flaw with our rendering logic. ignore the every frame triggers until we get more render logic done~
                squareButton.setShaderModifier(null);
                exampleShader.enable();
                exampleShader.setUniform("brightness", 1.0f);
                exampleShader.disable();
                System.out.println("Stopped hovering over button: " + button);
            }
        });
        squareButton.setOnClickAction(button -> {
            JukeBox.play("menuselect", "effect", 1, false);
            //exampleShader.setUniform("brightness", (float)Math.random());
            System.out.println("Clicked button : " + button +", at : "+gamePanel.playerInputManager.getPlayer(0).getMousePosition()[0]+"x"+gamePanel.playerInputManager.getPlayer(0).getMousePosition()[1]);
        });
        guiElements.add(squareButton);

        Button patrickButton = new Button((float) (gamePanel.WIDTH - patrickTexture.getHeight()) / 3, (float) (gamePanel.HEIGHT - patrickTexture.getHeight()) / 2, patrickTexture);
        patrickButton.getTexture().setShader(exampleShader);
        patrickButton.setOnHoverAction(button -> {
            if (!patrickButton.isHovered()) {
                JukeBox.play("menuoption", "effect", 1, true);
                patrickButton.setShaderModifier(shader -> shader.setUniform("brightness", (float)Math.random()));
                System.out.println("Hovering over button: " + button);
            }
        });
        patrickButton.setUnHoverAction(button -> {
            if (patrickButton.isHovered()) {
                JukeBox.play("menuoption", "effect", 1, true);
                patrickButton.setShaderModifier(null);
                exampleShader.enable();
                exampleShader.setUniform("brightness", 1.0f);
                exampleShader.disable();
                System.out.println("Stopped hovering over button: " + button);
            }
        });
        patrickButton.setOnClickAction(button -> {
            JukeBox.play("menuselect", "effect", 1, false);
            System.out.println("Clicked button : " + button +", at : "+gamePanel.playerInputManager.getPlayer(0).getMousePosition()[0]+"x"+gamePanel.playerInputManager.getPlayer(0).getMousePosition()[1]);
        });
        guiElements.add(patrickButton);

    }
    public void panelAtlas(){
        //TODO [0] bad logic here, need to fix.
        boolean recalculateAtlases = AtlasManager.addToAtlas(atlas1, "general", "general", backgroundTexture) ||
                AtlasManager.addToAtlas(atlas1, "general", "general", testTexture) ||
                AtlasManager.addToAtlas(atlas1, "general", "general", patrickTexture);
        if (!recalculateAtlases) AtlasManager.finalizeAtlasMaps(atlas1);
        //TODO [0] can have logic to check if we need to reupload.
        AtlasManager.saveAtlasToGPU(atlas1);
    }
    public void createAtlas(String path) throws InterruptedException {
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
        renderer = new BatchRenderer(atlas1, exampleShader);
    }
}