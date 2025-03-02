import com.github.exiostorm.audio.JukeBox;
import com.github.exiostorm.gui.Button;
import com.github.exiostorm.main.GamePanel;

import com.github.exiostorm.main.State;
import com.github.exiostorm.renderer.*;

import static com.github.exiostorm.main.EchoGame.gamePanel;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class MainMenu implements State {
    TextureManager textureManager = GamePanel.getTextureManager();
    BatchRenderer renderer;
    private Texture backgroundTexture;
    Shader exampleShader;
    private Texture testTexture;
    private Texture patrickTexture;
    TextureAtlasOld atlas;


    private int frameTester = 0;


    @Override
    public void init() {
        //System.out.println("GLSL Version: " + glGetString(GL_SHADING_LANGUAGE_VERSION));
        initAudio();
        //TODO something here to select UIHandler? <- huh?
        initTextures();//TODO
        initShaders();
        test();
        //initInterfaces();


    }

    @Override
    public void update() {
    }


    @Override
    public void render() {

        // Set up orthographic projection for 2D rendering
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, GamePanel.WIDTH, GamePanel.HEIGHT, 0, -1, 1); // 2D orthographic projection
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

        renderer.begin();
        /*
        for (GUIElement element : guiElements) {
            element.render();
        }*/

        renderer.draw(backgroundTexture, 0, 0, exampleShader, false);
        renderer.draw(testTexture, 10, 10, exampleShader, false);
        renderer.draw(patrickTexture, 200, 140, exampleShader, false);

        //renderer.draw(backgroundTexture, 0, 0, exampleShader, false);

        renderer.end();


        //exampleShader.enable();
        //exampleShader.setUniform("isFixedFunction", true); // For fixed-function
        //exampleShader.disable();


        // Disable blending after text rendering
        glDisable(GL_BLEND);
        // something with plane 0 / 3d rendering
        glEnable(GL_DEPTH_TEST);
        // Disable textures when done
        glDisable(GL_TEXTURE_2D);
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
        backgroundTexture = textureManager.addTexture("src/main/resources/Backgrounds/storm2.png"); // Replace with your path
        testTexture = textureManager.addTexture("src/main/resources/Backgrounds/test3.png");
        patrickTexture = textureManager.addTexture("src/main/resources/HUD/funnybutton.png");
    }
    private void initAudio(){
        JukeBox.load("src/main/resources/SFX/menuoption.ogg", "buttons", "menuoption");
        JukeBox.load("src/main/resources/SFX/menuselect.ogg", "buttons", "menuselect");
    }
    private void initInterfaces(){
        // Create the button and add it to the GUI elements
        Button squareButton = new Button(GamePanel.WIDTH-testTexture.getWidth(), (float) (GamePanel.HEIGHT - testTexture.getHeight()) / 2, testTexture);
        squareButton.getTexture().setShader(exampleShader);
        squareButton.setOnHoverAction(button -> {
            if (!squareButton.isHovered()) {
                JukeBox.play("menuoption", "effect", 1, true);
                squareButton.setUseShader(true);
                System.out.println("Hovering over button: " + button);
            }
        });
        squareButton.setUnHoverAction(button -> {
            if (squareButton.isHovered()) {
                JukeBox.play("menuoption", "effect", 1, true);
                squareButton.setUseShader(false);
                System.out.println("Stopped hovering over button: " + button);
            }
        });
        squareButton.setOnClickAction(button -> {
            JukeBox.play("menuselect", "effect", 1, false);
            System.out.println("Clicked button : " + button+", at : "+squareButton.getMousePosition()[0]+"x"+squareButton.getMousePosition()[1]);
        });
        gamePanel.guiElements.add(squareButton);
        Button patrickButton = new Button((float) (GamePanel.WIDTH - patrickTexture.getHeight()) / 3, (float) (GamePanel.HEIGHT - patrickTexture.getHeight()) / 2, patrickTexture);
        patrickButton.getTexture().setShader(exampleShader);
        patrickButton.setOnHoverAction(button -> {
            if (!patrickButton.isHovered()) {
                JukeBox.play("menuoption", "effect", 1, true);
                patrickButton.setUseShader(true);
                System.out.println("Hovering over button: " + button);
            }
        });
        patrickButton.setUnHoverAction(button -> {
            if (patrickButton.isHovered()) {
                JukeBox.play("menuoption", "effect", 1, true);
                patrickButton.setUseShader(false);
                System.out.println("Stopped hovering over button: " + button);
            }
        });
        patrickButton.setOnClickAction(button -> {
            JukeBox.play("menuselect", "effect", 1, false);
            System.out.println("Clicked button : " + button+", at : "+patrickButton.getMousePosition()[0]+"x"+patrickButton.getMousePosition()[1]);
        });
        gamePanel.guiElements.add(patrickButton);
    }
    public void test(){
        atlas = new TextureAtlasOld();
        long start = System.currentTimeMillis();
        atlas.addTexture(backgroundTexture, 0, (byte) 0);
        System.out.println("Time it took to add first texture to our atlas : "+(System.currentTimeMillis()-start));
        atlas.addTexture(testTexture, 0, (byte) 0);
        System.out.println("Time it took to add second texture to our atlas : "+(System.currentTimeMillis()-start));
        atlas.addTexture(patrickTexture, 0, (byte) 0);
        System.out.println("Time it took to add third texture to our atlas : "+(System.currentTimeMillis()-start));

        atlas.finalizeAtlas();
        System.out.println("Time it took to finalize our atlas : "+(System.currentTimeMillis()-start));
        //TODO somehow check if these affect used memory(they'd have to, but double check.)
        //testTexture.setBufferedImage(null);
        //patrickTexture.setBufferedImage(null);
        //backgroundTexture.setBufferedImage(null);
        renderer = new BatchRenderer(atlas, exampleShader);
    }
}