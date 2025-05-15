## this should manage how our rendering calls are batched.
##### Render alterations
* ~~Our method to add something to be rendered should also have a way to stretch the rendered texture. currently it sets up our quads using the texture dimensions, so this may or may not be complicated to incorporate.~~
    <br>we somewhat have something for this currently, not certain if it's correct though.
##### Layering
* I'm thinking that when we render entities, we should have them all lumped into an array that's sorted by the last entity that moved. this could make the entity layer more trivial to render on the correct layer.? in this scenario we then just render
entities by the tile layers, and then just manage where entities are in the world while keeping our tilemap layers. (though we should probably always keep the local player as the top layer for these entities?) basically our "layers" are these arrays.
need to research more about these layers, as people normally use z-layering, however I don't believe that fits our application because of things like floating platforms.
* need to figure out how to layer things like player equipments... we probably use some setup that layers the player's equipments in the same layer cycle.(causing the equipment to be rendered after everything the player is rendered after, as well as before.)
##### Design choices?
* for something like a moving background, for example clouds, we should create textures that can loop from their front and back. then to "animate" them all we would do is have 3 objects that slowly move in sync while the front is moving to the back once out of view. this is less of a mechanism of the batcher, and more of a reminder of the way to design things.
