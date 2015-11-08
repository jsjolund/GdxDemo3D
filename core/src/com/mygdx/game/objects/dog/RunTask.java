package com.mygdx.game.objects.dog;

import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.objects.DogCharacter.DogSteerSettings;

/**
 * @author davebaol
 */
public class RunTask extends DogTaskBase {

	public RunTask () {
		super(0.2f);
	}

	@Override
	protected void startAnimation(DogCharacter dog) {
		dog.animations.animate("armature|move_run", -1, 1, dog.animationListener, 0.1f);

		dog.setMaxLinearSpeed(DogSteerSettings.maxLinearSpeed * DogSteerSettings.runMultiplier);
		dog.setMaxLinearAcceleration(DogSteerSettings.maxLinearAcceleration * DogSteerSettings.runMultiplier);

		dog.setMaxAngularSpeed(DogSteerSettings.maxAngularSpeed * DogSteerSettings.runMultiplier);
		dog.setMaxAngularAcceleration(DogSteerSettings.maxAngularAcceleration * DogSteerSettings.runMultiplier);

		if (dog.followPathSB != null) {
			dog.followPathSB.setDecelerationRadius(DogSteerSettings.decelerationRadius * DogSteerSettings.runMultiplier);
		}
	}

}
