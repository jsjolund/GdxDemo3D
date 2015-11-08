package com.mygdx.game.objects.dog;

import com.mygdx.game.objects.DogCharacter;

/**
 * @author davebaol
 */
public class StandTask extends DogTaskBase {

	public StandTask () {
	}

	@Override
	public void startAnimation(DogCharacter dog) {
		dog.animations.animate("armature|idle_stand", -1, 1, dog.animationListener, 0.1f);
	}

}
