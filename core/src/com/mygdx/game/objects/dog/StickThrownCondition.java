package com.mygdx.game.objects.dog;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.mygdx.game.objects.DogCharacter;

/**
 * @author davebaol
 */
public class StickThrownCondition extends LeafTask<DogCharacter> {

	public StickThrownCondition () {
	}

	@Override
	public void start() {
	}

	@Override
	public void run () {
		if (getObject().stickThrown) {
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
