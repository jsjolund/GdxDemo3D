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
import com.mygdx.game.GameScreen;
import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.objects.HumanCharacter;
import com.mygdx.game.pathfinding.Triangle;

/**
 * Action task that traces a path from the dog's current position to its owner (actually, 1 meter in front of him).
 * 
 * @author davebaol
 */
public class CalculatePathToHumanTask extends LeafTask<DogCharacter> {

	private static final Vector3 TMP_VEC_1 = new Vector3();
	private static final Vector3 TMP_VEC_2 = new Vector3();
	private static final Vector3 TMP_VEC_3 = new Vector3();

	public CalculatePathToHumanTask() {
	}

	@Override
	public Status execute() {
		DogCharacter dog = getObject();
		HumanCharacter human = dog.human;

		Vector3 humanDirection = human.getDirection(TMP_VEC_1);
		Vector3 humanPosition = human.getGroundPosition(TMP_VEC_2);
		float targetDogDistanceToHuman = 2;
		Vector3 dogTargetPoint = TMP_VEC_3;

		Triangle dogTargetTri = GameScreen.screen.engine.getScene().navMesh.getClosestValidPointAt(
				humanPosition,
				humanDirection,
				targetDogDistanceToHuman,
				dogTargetPoint, human.visibleOnLayers);

		return  dog.followPath(dogTargetTri, dogTargetPoint) ? Status.SUCCEEDED : Status.FAILED;
	}

	@Override
	protected Task<DogCharacter> copyTo(Task<DogCharacter> task) {
		return task;
	}

}
