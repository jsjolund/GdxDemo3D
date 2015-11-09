package com.mygdx.game.objects.dog;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.mygdx.game.objects.DogCharacter;

/**
 * @author davebaol
 */
public class HumanWantToPlayCondition extends LeafTask<DogCharacter> {

	public HumanWantToPlayCondition () {
	}

	@Override
	public void start() {
	}

	@Override
	public void run () {
		if (getObject().humanWantToPlay) {
			success();
		}
		else {
			fail();
		}
	}

	@Override
	protected Task<DogCharacter> copyTo (Task<DogCharacter> task) {
		return task;
	}

}
