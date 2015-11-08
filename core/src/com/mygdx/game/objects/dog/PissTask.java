package com.mygdx.game.objects.dog;

import com.mygdx.game.objects.DogCharacter;

/**
 * @author davebaol
 */
public class PissTask extends DogActionBase {

	public PissTask () {
	}

	public void startAnimation(DogCharacter dog) {
		dog.animations.animate("armature|action_piss", -1, 1, dog.animationListener, 0.1f);
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
