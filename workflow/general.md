  #### General things to keep an eye on and correct

* Encapsulation
* Naming convention
* Modularity
<br>Need to continously watch code and re-structure when it starts to web with other classes. This was already done multiple times, once with the GamePanel class, and our **main()** classes.
* Documentation
<br>When our code is cleaned up, modular, and fairly feature-rich, We should work on a java doc to provide detailed explanations for methods used and class summary.
* Compatibility
<br>For example we're using OpenGL instead of Vulkan, even though Vulkan would give us a massive performance potential. Like Java, OpenGL keeps us compatible with many systems. Unfortunately we'll be violating this rule with our non-virtualized multi-keyboard support on Linux.
* Dynamic Assets
<br>When we're loading assets we should try to keep an eye on *how* we load those assets and if it would make sense for them to automatically be loaded when in the program directory. For example we're treating gamestates / maps as an asset and we use a class factory to detect and load these assets. yeah yeah security risk, I'm sure it is... I'm open to ideas, but I'm not wavering from having this feature.
