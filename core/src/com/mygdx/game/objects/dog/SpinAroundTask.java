package com.mygdx.game.objects.dog;

import com.mygdx.game.objects.DogCharacter;

/**
 * @author davebaol
 */
public class SpinAroundTask extends DogActionBase {

	public SpinAroundTask () {
	}

	@Override
	public void startAnimation(DogCharacter dog) {
		dog.animations.animate("armature|idle_search", -1, 1, dog.animationListener, 0.1f);
	}

}
