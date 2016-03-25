/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/


package com.mygdx.game.debugdrawers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.objects.GameCharacter;
import com.mygdx.game.objects.HumanCharacter;
import com.mygdx.game.utilities.MyShapeRenderer;

/**
 * @author jsjolund
 */
public class ArmatureDebugDrawer {

	private final static Vector3 TMP_V1 = new Vector3();
	private final static Vector3 TMP_V2 = new Vector3();
	private final static Vector3 TMP_V3 = new Vector3();
	private final static Quaternion TMP_Q = new Quaternion();

	private MyShapeRenderer shapeRenderer;

	public void drawArmature(MyShapeRenderer shapeRenderer, GameCharacter character, String rootNodeId) {
		this.shapeRenderer = shapeRenderer;
		if (character == null) {
			return;
		}
		if (shapeRenderer.isDrawing()) {
			shapeRenderer.end();
		}

		shapeRenderer.begin(MyShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(Color.YELLOW);

		Node skeleton = character.modelInstance.getNode(rootNodeId);
		if (skeleton != null) {
			Vector3 modelPos = TMP_V1;
			Vector3 rootNodeGlobalPos = TMP_V2;
			Vector3 debugNodePos = TMP_V3;
			Quaternion modelRot = TMP_Q;
			
			character.modelInstance.transform.getTranslation(modelPos);
			character.modelInstance.transform.getRotation(modelRot);
			skeleton.globalTransform.getTranslation(rootNodeGlobalPos);
			drawArmatureNodes(skeleton, modelPos, modelRot, rootNodeGlobalPos, debugNodePos);
		}
		if (character instanceof HumanCharacter) {
			HumanCharacter human = (HumanCharacter) character;

			human.getBoneMidpointWorldPosition(HumanCharacter.HumanArmature.RIGHT_HAND.id, TMP_V1);
			drawVertex(TMP_V1, 0.05f, Color.RED);
			human.getBoneMidpointWorldPosition(HumanCharacter.HumanArmature.LEFT_HAND.id, TMP_V1);
			drawVertex(TMP_V1, 0.05f, Color.GREEN);
		}
		if (character instanceof DogCharacter) {
			DogCharacter dog = (DogCharacter) character;
  
			dog.getBoneMidpointWorldPosition(DogCharacter.DogArmature.HEAD.id, TMP_V1);
			drawVertex(TMP_V1, 0.05f, Color.RED);
			dog.getBoneDirection(DogCharacter.DogArmature.HEAD.id, TMP_V2);
			drawVertex(TMP_V1.add(TMP_V2.scl(0.5f)), 0.05f, Color.GREEN);
		}

		shapeRenderer.end();
	}



	private void drawArmatureNodes(Node currentNode, Vector3 modelPos,
								   Quaternion modelRot,
								   Vector3 parentNodePos, Vector3 currentNodePos) {
		currentNode.globalTransform.getTranslation(currentNodePos);
		modelRot.transform(currentNodePos);
		currentNodePos.add(modelPos);
		drawVertex(currentNodePos, 0.02f, Color.GREEN);
		shapeRenderer.setColor(Color.YELLOW);
		if (currentNode.hasParent()) {
			shapeRenderer.line(parentNodePos, currentNodePos);
		}
		if (currentNode.hasChildren()) {
			float x = currentNodePos.x;
			float y = currentNodePos.y;
			float z = currentNodePos.z;
			for (Node child : currentNode.getChildren()) {
				drawArmatureNodes(child, modelPos, modelRot, currentNodePos, parentNodePos);
				currentNodePos.set(x, y, z);
			}
		}
	}


	private void drawVertex(Vector3 pos, float size, Color color) {
		float offset = size / 2;
		shapeRenderer.setColor(color);
		shapeRenderer.box(pos.x - offset, pos.y - offset, pos.z + offset, size, size, size);
	}

}

