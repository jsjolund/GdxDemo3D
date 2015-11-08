package com.mygdx.game.objects.dog;

import com.mygdx.game.objects.DogCharacter;

/**
 * @author davebaol
 */
public class LieDownTask extends DogTaskBase {

	public LieDownTask () {
	}

	@Override
	protected void startAnimation (DogCharacter dog) {
		dog.animations.animate("armature|idle_lie_down", -1, 1, dog.animationListener, 0.1f);
	}

}
