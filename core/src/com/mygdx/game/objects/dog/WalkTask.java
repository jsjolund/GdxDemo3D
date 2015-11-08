package com.mygdx.game.objects.dog;

import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.objects.DogCharacter.DogSteerSettings;

/**
 * @author davebaol
 */
public class WalkTask extends DogTaskBase {

	public WalkTask () {
		super(0.7f);
	}

	@Override
	public void startAnimation(DogCharacter dog) {
		dog.animations.animate("armature|move_walk", -1, 1, dog.animationListener, 0.1f);

		dog.setMaxLinearSpeed(DogSteerSettings.maxLinearSpeed);
		dog.setMaxLinearAcceleration(DogSteerSettings.maxLinearAcceleration);

		dog.setMaxAngularSpeed(DogSteerSettings.maxAngularSpeed);
		dog.setMaxAngularAcceleration(DogSteerSettings.maxAngularAcceleration);

		if (dog.followPathSB != null) {
			dog.followPathSB.setDecelerationRadius(DogSteerSettings.decelerationRadius);
		}
	}

}
