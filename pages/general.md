  # General things to keep an eye on and correct
<p hidden style="text-align: center">If you can see this text, you are viewing the wrong page. please go to : https://exiostorm.github.io/EchoEngine/general.html</p>

#### Modularity
* Need to continously watch code and re-structure when it starts to web with other classes. This was already done multiple times, once with the GamePanel class, and our **main()** classes.
* Try to look at multiple ways of achieving the same goal, for example immediate mode rendering vs retained mode rendering both take in a file path to load an image, immediate mode was using a method to initialize our Texture with that path, and then had direct draw calls when that texture was used. Retained mode rendering isn't too much different besides having many extra internal steps, and needing extra methods for data management which is entirely unused when we used immediate mode. **(could still have data management, it just wouldn't effect the performance enough to bother with when we're already being so inefficient... perhaps this behaviour could be isolated to a different class that could work with both methods?)** try thinking of ways to bridge these differences and isolating functional behavior so that it's within the rendering class itself instead of webbing between multiple classes.
#### Documentation & legibility
* When our code is cleaned up, modular, and fairly feature-rich, We should work on a java doc to provide detailed explanations for methods used and class summary. Additionally, lets keep code open-source and no use of obfuscation tools.
#### Compatibility
* For example we're using OpenGL instead of Vulkan, even though Vulkan would give us a massive performance potential. Like Java, OpenGL keeps us compatible with many systems. Unfortunately we'll be violating this rule with our non-virtualized multi-keyboard support on Linux.
#### Dynamic Assets
* When we're loading assets we should try to keep an eye on *how* we load those assets and if it would make sense for them to automatically be loaded when in the program directory. For example we're treating gamestates / maps as an asset and we use a class factory to detect and load these assets. yeah yeah security risk, I'm sure it is... I'm open to ideas, but I'm not wavering from having this feature.

# General learning curve stuff~

#### Encapsulation
* hiding information from being accessed(for example say we have something that must stay the same value, we don't want anything accidentally broken by someone changing that value.)
* hiding system internals so our IDE doesn't fill with bloat when we look for a method inside a class
* and controlling how that data is managed.
#### Naming conventions
* Packages are all lowercase(all).
* Classes and Interfaces are all uppercase(UpperCamelCase).
* Methods are lowerCamelCase.
* Variables should be lowerCamelCase, while Constants should be ALL_UPPERCASE using underscores.

* Avoid "magic numbers", use Constants in this case instead. e.g. instead of `if (i < 10)`, use a constant `if (i < MAX_VALUE)`.
