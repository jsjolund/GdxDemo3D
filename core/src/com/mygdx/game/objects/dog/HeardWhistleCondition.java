package com.mygdx.game.objects.dog;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.mygdx.game.objects.DogCharacter;

/**
 * @author davebaol
 */
public class HeardWhistleCondition extends LeafTask<DogCharacter> {

	public HeardWhistleCondition () {
	}

	@Override
	public void start() {
	}

	@Override
	public void run () {
		if (getObject().heardWhistle) {
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
