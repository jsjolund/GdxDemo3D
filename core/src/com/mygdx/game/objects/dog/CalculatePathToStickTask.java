/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.mygdx.game.objects.dog;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.GameEngine;
import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.pathfinding.Triangle;

/**
 * Action task that traces a path from the dog's current position to the stick.
 * 
 * @author davebaol
 */
public class CalculatePathToStickTask extends LeafTask<DogCharacter> {

	public CalculatePathToStickTask () {
	}

	public void startAnimation(DogCharacter dog) {
	}

	@Override
	public void start() {
		getObject().stickThrown = false;
	}

	@Override
	public Status execute () {
		DogCharacter dog = getObject();

		// Calculate random point
		// TODO: We should find a path to the stick instead
		Triangle randomTri = GameEngine.engine.getScene().navMesh.getRandomTriangle();
		Vector3 randomPoint = new Vector3();
		randomTri.getRandomPoint(randomPoint);
		
		return  dog.followPath(randomTri, randomPoint) ? Status.SUCCEEDED : Status.FAILED;
	}

	@Override
	protected Task<DogCharacter> copyTo (Task<DogCharacter> task) {
		return task;
	}

}
