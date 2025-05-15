### [Retained Mode]() aka batch rendering
  * We've setup retained mode rendering via OpenGL. this should have huge performance benefits going forward, albeit at complexity costs.

### [Texture Atlas]()
  * save to / load from json file to save layout of generated atlas.
  * currently I'm thinking we could have this setup in such a way where our changing textures are setup so that they must maintain the same dimensions so they have a rserved location in the atlas to make it simpler and more efficient when changing textures in game. [This saves us from expensively calculating texture positions on our atlas when we; for example change maps.]()(though maps might get put in another texture slot in case number of unique tiles changes)
<br>by keeping our texture dimensions the same we can then use glTexSubImage2D or similar methods to selectively modify atlas data to keep atlas changes efficient</br>
  * texture swapping should also follow a batching system because of efficiency.
  * for in-game paint tools we can either use something like a 9-bit color map to reduce VRAM usage, or setup something like a limited-size atlas that's separate from our program textures. 
  * need to figure out how layering works. need to have layers that are permanent like UI elements, while also allowing things like players being able to walk behind things and things like certain tiles belong to different layers so we can have players walk behind things if the tile they're walking on is a different layer. (some of this is done via 3d positions, thought GUI and other things need yet another layer to separate from FBO post-effects)
  * trying to research PBO(pixel buffer objects) / bindless textures, not sure how either of these things can be used with our setup. (still haven't learned anything about these...)
  * I'm not sure if we could automate it by checking if any files are modified later than a certain time, but it would be good to save this expensive atlas generation somehow so it only runs once(or not at all via a pre-configured setup when we distribute. then being once per modification.)
  * [Complex Atlas setup]() - Everything needs to be categorized, create atlas categories and sub-atlases, then pack them into a primary atlas.
    * [Sub-Atlas]() - MultiValuedMap<String, MultiValuedMap<String, Map<Texture, Rectangle>>> subAtlases 
      * **// Category -> (SubAtlas Name -> (Texture -> Placement))**
    * [Primary-Atlas]() - Map<String, String> primaryAtlas
      *  **// Category -> Active SubAtlas**
      * This will be used to save our data for what is where so we know how to swap data on the buffer.
### [Shaders]()
  * We have Shaders, Materials, and FBOs setup. now just need to figure out management of layers and whatnot... researching other projects and reading some books while I try to understand how this is supposed to be structured.
