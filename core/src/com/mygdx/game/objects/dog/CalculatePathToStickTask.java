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
import com.mygdx.game.pathfinding.NavMesh;
import com.mygdx.game.pathfinding.Triangle;

/**
 * Action task that traces a path from the dog's current position to the stick.
 *
 * @author davebaol
 */
public class CalculatePathToStickTask extends LeafTask<DogCharacter> {

	private final static Vector3 TMP_V1 = new Vector3();
	private final static Vector3 TMP_V2 = new Vector3();

	public CalculatePathToStickTask() {
	}

	public void startAnimation(DogCharacter dog) {
	}

	@Override
	public void start() {
		getObject().stickThrown = false;
	}

	@Override
	public Status execute() {
		DogCharacter dog = getObject();

		NavMesh navMesh = GameScreen.screen.engine.getScene().navMesh;

		Vector3 stickPos = dog.human.stick.body.getWorldTransform().getTranslation(TMP_V1);
		Vector3 stickNavmeshPos = TMP_V2;
		Triangle stickNavmeshTri = navMesh.verticalRayTest(stickPos, TMP_V2, null);

		if (stickNavmeshTri == null) {
			stickNavmeshTri = navMesh.getClosestTriangle(stickPos, stickNavmeshPos, null);
		}

		return dog.followPath(stickNavmeshTri, stickNavmeshPos) ? Status.SUCCEEDED : Status.FAILED;
	}

	@Override
	protected Task<DogCharacter> copyTo(Task<DogCharacter> task) {
		return task;
	}

}
