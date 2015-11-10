package com.mygdx.game.objects.dog;

import com.mygdx.game.objects.DogCharacter;

/**
 * @author davebaol
 */
public class CalculatePathToStickTask extends DogActionBase {

	public CalculatePathToStickTask () {
	}

	public void startAnimation(DogCharacter dog) {
	}

	@Override
	public void start() {
		getObject().stickThrown = false;
	}

	@Override
	public void run () {
		// TODO: calculate destination and path 
		
		success();
	}

}
