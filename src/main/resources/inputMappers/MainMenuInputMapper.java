import com.github.exiostorm.graphics.AtlasManager;
import com.github.exiostorm.graphics.gui.Cursor;
import com.github.exiostorm.graphics.gui.GUIElement;
import com.github.exiostorm.graphics.gui.Button;
import com.github.exiostorm.main.State;
import com.github.exiostorm.graphics.Texture;
import com.github.exiostorm.graphics.TextureManager;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static com.github.exiostorm.main.EchoGame.gamePanel;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN;
import static org.lwjgl.opengl.GL11.*;

public class MainMenuInputMapper implements State {
    Map<Integer, List<Integer>> allPlayerInputs;
    double[] mousePosition = {0,0}; //This is just used for tracking where the mouse has been.
    List<Integer> pressedKeys;
    private List<Texture> mouseTextures;

    static Cursor cursor;

    @Override
    public void init() {
        // Initialization logic for MainMenu, if any
        mouseTextures = new ArrayList<>();
        for (int i = 1; i<16;) {
            mouseTextures.add(TextureManager.addTexture("src/main/resources/HUD/mouse/mouse_" + i +".png"));
            AtlasManager.newAddToAtlas(gamePanel.getAtlas(), "general", "general", mouseTextures.get(i-1));
            i++;
        }
        // Create a Cursor instance
        cursor = new Cursor(mouseTextures, 0.001f);
        //TODO [1] 2025/04/05 need to implement logic for handling animations / frames for textures, we still need the logic for rendering the cursor
        //GLFW.glfwSetInputMode(gamePanel.getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
        System.out.println("MainMenuInputMapper initialized.");
    }

    @Override
    public void update() {
        cursor.update(gamePanel.getDeltaTime());
        checkMouseMoved();
        checkForInputs();
    }
    @Override
    public void render() {
        cursor.render((float)getMouse()[0],(float)getMouse()[1]);
    }
    private double[] getMouse(){
        return gamePanel.playerInputManager.getPlayer(0).getMousePosition();
    }
    private void checkMouseMoved() {
        if (!Arrays.equals(getMouse(), mousePosition)) {
            //JukeBox.play("menuoption", "effect", 1, true);
            /*System.out.println("Mouse position changed from :  X:" + mousePosition[0] + ", Y:" + mousePosition[1] +
                    ", to X: " + gamePanel.playerInputManager.getPlayer(allPlayerInputs.size()-1).getMousePosition()[0] + ", Y: " + gamePanel.playerInputManager.getPlayer(allPlayerInputs.size()-1).getMousePosition()[1]);*/
            mousePosition = getMouse();
            if (Objects.equals(gamePanel.getCurrentState()[0], "MainMenu")) {
                // Handle mouse hovering//TODO whoops. MainMenu dependency. need to move code to MainMenu somehow? or maybe not... might reuse our mappings across different states
                for (GUIElement element : gamePanel.guiElements) {
                    if (element instanceof Button) {
                        if (element.isMouseOver((float) getMouse()[0], (float) getMouse()[1])) {
                            // Optionally handle hover visuals or effects
                            ((Button) element).triggerHoverAction();
                        } else {
                            ((Button) element).stopHoverAction();
                        }
                    }
                }
            }
        }
    }
    private void checkForInputs(){
        allPlayerInputs = gamePanel.playerInputManager.update();
        for (Map.Entry<Integer, List<Integer>> entry : allPlayerInputs.entrySet()) {
            int playerId = entry.getKey(); // Get the player ID
            pressedKeys = entry.getValue(); // Get the pressed keys for this player
            for (Integer key : pressedKeys) {
                if(key >= 0 && key <= 7){
                    mousePosition = getMouse(); // Get the player's mouse position
                    //TODO whoops another MainMenu dependency. need to move code to MainMenu somehow? or maybe not... might reuse our mappings across different states
                    if (Objects.equals(gamePanel.getCurrentState()[0], "MainMenu")) {
                        for (GUIElement element : gamePanel.guiElements) {
                            if (element instanceof Button) {
                                if (element.isMouseOver((float) getMouse()[0], (float) getMouse()[1]) && key == 0) {
                                    // Handle click action
                                    ((Button) element).triggerClickAction();
                                }
                            }
                        }
                    }
                    //JukeBox.play("menuselect", "effect", 1, false);
                    /*System.out.println("Player " + (playerId+1) + " clicked mouse button: " + key +
                            " at position X: " + mousePosition[0] + ", Y: " + mousePosition[1]);*/
                } else {
                    // Process each key, and now we know which player pressed it
                    System.out.println("Player " + (playerId+1) + " pressed key: " + key);
                }
                // Check for specific key actions
                if (key == 96) { // Example: if key is '96'
                    // Set the game state using the state manager
                    if(!Objects.equals(gamePanel.getCurrentState()[0], "TestMenu") && Objects.equals(gamePanel.getCurrentState()[0], "MainMenu")) {
                        gamePanel.setCurrentState(gamePanel.stateManager.setState("TestMenu", gamePanel.gameStates, gamePanel.getCurrentState()));
                    } else if (!Objects.equals(gamePanel.getCurrentState()[0], "MainMenu") && Objects.equals(gamePanel.getCurrentState()[0], "TestMenu")) {
                        gamePanel.setCurrentState(gamePanel.stateManager.setState("MainMenu", gamePanel.gameStates, gamePanel.getCurrentState()));
                    }
                }
                // You can add more player-specific logic here based on the key and player
            }
        }
    }

    public static Cursor getCursor() {
        return cursor;
    }
}
