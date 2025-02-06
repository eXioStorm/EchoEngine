#### Retained Mode aka batch rendering
  * We've setup retained mode rendering via OpenGL. this should have huge performance benefits going forward, albeit at complexity costs.

#### Texture Atlas
  * in the pursuit of more performance we're constructing a system that will generate a texture atlas at startup(and we hope to save this atlas with some form of ID that matches with the textures we use so that this performance hit is only on first startup.)
  * currently I'm thinking we could have this setup in such a way where our changing textures are setup so that they must maintain the same dimensions so they have a reserved location in the atlas to make it simpler and more efficient when changing textures in game. [This saves us from expensively calculating texture positions on our atlas when we; for example change maps.]()
  * by keeping our texture dimensions the same we can then use glTexSubImage2D or similar methods to selectively modify atlas data to keep atlas changes efficient
  * we can also utilize having multiple atlas setups for things like tilemaps, which would give us atlases inside our atlases.
  * we should set up our texture swapping to queue these texture updates to be batched together and updated simultaneously, OR with limits to prevent stutters.
  * for in-game paint tools we can either use something like a 9-bit color map to reduce VRAM usage, or setup something like a limited-size atlas that's separate from our program textures. 
  * trying to research PBO(pixel buffer objects) / bindless textures, not sure how either of these things can be used with our setup.
#### Shaders
  * Still learning setting this up, will be super important for things like particles, highlighting, font rendering(changing font color), etc.
