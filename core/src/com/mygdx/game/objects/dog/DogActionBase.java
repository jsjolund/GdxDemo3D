package com.mygdx.game.objects.dog;

import com.mygdx.game.objects.DogCharacter;

/**
 * @author davebaol
 */
public abstract class DogActionBase extends DogTaskBase {

	public DogActionBase () {
	}

	@Override
	public void start() {
		DogCharacter dog = getObject();
		dog.currentAnimationFinished = false;
		startAnimation(dog);
	}

	@Override
	public void run () {
		DogCharacter dog = getObject();
		if (dog.currentAnimationFinished) {
			success();
		}
		else {
			updateAnimation(dog);
			running();
		}
	}

}
