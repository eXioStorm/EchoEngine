package com.github.exiostorm.graphics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

//TODO [0] 2025-04-12
// https://discord.com/channels/272761734820003841/1360240205625430097/1360240205625430097
// Confirmation that our solution is going to be quite unique from how other people handle this data.
// Although our solution might be complicated, hopefully it will allow our engine to be a LOT more flexible with managing things later.

public class ShaderManager {
    // TODO Map of Objects, will change Object to another class name... will need a class to represent our shader uniform objects similar to how Materials are used in other projects.
    Map<String, Object> consumerObjectMap = new HashMap<>();
    //TODO this might just be to contain all of our uniform names for easy iteration over our map? no idea.
    ArrayList<String> uniforms = new ArrayList<>();

    public Consumer<Shader> createConsumer() {

        return null;
    }
}
