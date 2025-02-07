animator? spriteAnimator? idfk man. will work on naming conventions later.

For animating our textures we'll have yet another setup where our gimp plugin saves a data / json file with our relative file paths, frame numbers, frame lengths, etc.
we'll create a new map with our frame, texture, length, etc.
in this class we'll have a method to update that can optionally select the frame we're on.?
something is missing... we need to have different animation possibilities such as when the player walks, jumps, swims, etc... I suppose when the player is in any of these states we're technically rendering a different sprite batch...
yeah in that scenario we'd probably have something within the player object that switches the animators around. possibly even in a map even. if (player.isWalking) playerAnimator = animatorMap.get(walking); or having it in our input handling. however which way.
