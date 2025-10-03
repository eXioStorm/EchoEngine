<p hidden>If you can see this text, you are viewing the wrong page. please go to : https://exiostorm.github.io/EchoEngine/renderer.html#animator</p>
animator? spriteAnimator? idfk man. will work on naming conventions later.

* For animating our textures we'll have yet another setup where our gimp plugin saves a data / json file with our relative file paths, frame numbers, frame lengths, etc.
* we'll create a new map with our frame, texture, length, etc.
* in this class we'll have a method to update that can optionally select the frame we're on.?
* something is missing... we need to have different animation possibilities such as when the player walks, jumps, swims, etc... I suppose when the player is in any of these states we're technically rendering a different sprite batch...
* yeah in that scenario we'd probably have something within the player object that switches the animators around. possibly even in a map even. if (player.isWalking) playerAnimator = animatorMap.get(walking); or having it in our input handling. however which way.
* I'm not sure how it's going to be setup for the time per frame bit, but if we make it automatic, then we also need to be able to have it managed externally as well.
* we also need to be able to share the animator if needed, for things like grass for example. and we need to make sure that it is capable of being independent too. so we'll probably have some sort of identifier for the animator.
* lets try to think of how it should work when shared...
* batchRender.add(animator.update(null), 100, 200)?
* **have update return a texture**?
* need functional operations to pause, resume, stop? example when we need this : Player jumps, we want to stop the idle animation so when we start it next time it resets to the first frame. same for when we switch from the jumping animation. currentAnimator.stop(); currentAnimator = animatorMap.get(jumping);
