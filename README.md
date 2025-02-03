[discord-invite]: https://discord.gg/invite/eXioStorm#6069
[FAQ]: https://img.shields.io/badge/Wiki-FAQ-blue.svg
[![Discord](https://cdn.discordapp.com/attachments/509290751478464525/574644069607800843/myDiscord.png)][discord-invite]
[![Discord](https://discordapp.com/api/guilds/431382619595210752/widget.png)][discord-invite]

[ ![FAQ] ](https://github.com/eXioStorm/demimorph)
# Echo Engine
an attempt to create a platformer with extensive modding capabilities.

## Our Core Values!
* [Modularity]() - We MUST try to keep our packages as independant as possible from one another to make it easy to upgrade features later on. When we are unable to isolate our classes/packages, we must try to then isolate the input / output of these packages to be simple so we don't create dependency webs.
* [Legibility]() - Processes must be made as humanely legible as possible from the top level, significant effort must be made writing docs / comments when we are unable to abstract to something humanely legible. 
* [Dynamic Handling of Assets]() - Things will be designed in such a way that doesn't require re-compilation of source code to add common assets to the program; such as creating new game maps, character skills/equipment/designs etc, adding new sound tracks or sound effects, etc. (even better if this can even happen during runtime)
* [Decentralization]() - Taking some lessons from Minecraft, and overall the entire online scene, the design of this system will be such that makes it very difficult to eliminate cheating, with as much data being stored locally when playing with others. Of course we don't want people to ruin other's fun, at the same time we don't want bad actors to exploit any that are impressionable. This may involve processes that limit your influence on other's data, while protecting individual's data such as inventory / character data isolated from servers? No gambling. period. If access restrictions are meant to protect the perceived value of virtual assets in an online service, then any claim in your TOS or policies stating that these assets have no monetary value is self-contradictory—because by enforcing these restrictions, you have artificially assigned value to them.

## workflow progress
* [Maven]() - get Maven properly setup so we can use git more easily.
* ~~[Audio]() - Positional audio that's easy to use.~~
  * ~~add categories~~
  * ~~add a sound type that can overlap itself~~
  * need streaming capabilities to conserve memory when playing long background soundtracks.
* [Rendering]() - what you will see on the screen. (or wont see)
  * ~~render window~~
  * Retained mode OpenGL rendering to keep things well supported across different devices. (mostly done, working on texture atlas management)
  * get Shaders working
  * Buttons & other UI elements. Buttons need to interact with shaders for simple graphical effects such as changing brightness to indicate activation / hovering.
* [Physics]() - things like collisions...
  * thinking of handling certain collisions like : register pixels adjacent to transparent pixels in an array for movement collisions, and all opaque pixels for things like click "collisions".
* [Inputs]() - Handles all user inputs, gaming controller controls, keyboard buttons, mouse buttons, mouse position, basically any device which you would plug in to your computer to interact with the game.
  * MIDI input support so that players may use instruments in-game to play sound fonts 
  * need to capture the device ID for inputs so that multiple separate keyboards may be used. this feature is going to be indefinitely post-poned. requires very extensive knowledge and security planning. "aiming to create a multi-user, multi-input setup on a single machine without full OS virtualization."
    all because Operating Systems have only been designed to communicate the keypress and nothing else. Working around the OS control is very complicated. and a potential security risk.
    HOWEVER! it may be feasible to make this a linux-only feature, as the limitation is mainly specific to other OS that are reluctant to entertain this old request.(I'm far from the first to ask for this.)
* [TileMap Manager]() - handles retrieving information about the tilemap (game map) note: maps should have their own method which extends this method to add unique information to the map which is specific to the game. such as enemy spawns or BGM.
* [Entity Handler]() - ???
  * Player
  * NPC
  * Objects
* [Game Loop]() - The code that runs x times per second (main loop)
  * Multi-Threading : We want to make sure that our core application mechanics are never affected by resource lag.
* [Artificial Intelligence]() - handling logic for NPCs, could even use LLMs to experiment with.
* [Event Handler]() - handles things such as your keyboard layout / what should happen if x happens?
  * Add a textfield for sending text to the client(can be used later to also send it to multiplayer chat)
* [Event Scheduler]() - Schedules when the game should do something, in events where it is required that this runs with the main game loop. (such as timed delays?) for example, something happens and needs to do something else in 30 seconds, this will run every so often to check if 30 seconds has elapsed since then, and once it finally has, it will do something. (adjust how frequently this method will run)
* [RPG System?]() - Handle things such as abilities, items, inventories, worn equipment / items, etc?
  * writable assets such as written books, item tags, monster naming, etc.
  * Posters / drawable assets,
  * music sheets? playable music assets,
* [Communications]() - Planning to use Matrix API for multiplayer communications, then we have a decentralized chat system where people can implement their own censoring and logging tools. and can also stay connected while offline, for example from their smartphones. (if possible, try to make it difficult for servers to censor chat in any way, such as making chat handle via peer-to-peer)
* []() -

* [Networking]() - anything which interacts with somebody or something else over the internet.
  * Add multiplayer, either from peer to peer, or from setting up a server. (probably a server setup?)
## Other ToDos
* []() -

## Dependencies
* I'm using [LWJGL](https://www.lwjgl.org/) (BSD-3-Clause license) to run this project, so far I'm utilizing the following libraries:
  * [OpenAL](https://github.com/LWJGL/lwjgl3-wiki/wiki/2.1.-OpenAL) - BSD-3-Clause license
  * [stb](http://www.java-gaming.org/index.php?topic=36153.0) - page gone? might not be using this anymore, instead probably using LWJGL
  * [minimal-json](https://github.com/ralfstx/minimal-json) - apache 2 license
  * [Gson](https://github.com/google/gson) - noted as duplicate? apache 2 license
  * [Guava](https://github.com/google/guava) - apache 2 license
  * [joml](https://github.com/JOML-CI/JOML) - MIT license

## special thanks:
[Mike S.](https://github.com/foreignguymike) Thanks for making the original game code which I eviscerated in order to start learning how to make my own. Also check out his [youtube channel](https://www.youtube.com/channel/UC_IV37n-uBpRp64hQIwywWQ).
