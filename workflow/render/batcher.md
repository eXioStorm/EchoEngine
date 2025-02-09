* this should manage how our rendering calls are batched.
* I'm thinking that when we render entities, we should have them all lumped into an array that's sorted by the last entity that moved. this could make the entity layer more trivial to render on the correct layer.? in this scenario we then just render
entities by the tile layers, and then just manage where entities are in the world while keeping our tilemap layers.
