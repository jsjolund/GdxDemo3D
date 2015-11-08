package com.mygdx.game.objects.dog;

import com.mygdx.game.objects.DogCharacter;

/**
 * @author davebaol
 */
public class SitTask extends DogTaskBase {

	public SitTask () {
	}

	@Override
	public void startAnimation(DogCharacter dog) {
		dog.animations.animate("armature|idle_sit", -1, 1, dog.animationListener, 0.1f);
	}
}
