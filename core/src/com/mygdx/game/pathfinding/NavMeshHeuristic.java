/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.mygdx.game.pathfinding;

import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.math.Vector3;

/**
 * @author jsjolund
 */
public class NavMeshHeuristic implements Heuristic<Triangle> {

	@Override
	public float estimate(Triangle node, Triangle endNode) {
		float minDst = Float.MAX_VALUE;
		for (int i = 0; i < node.corners.size; i++) {
			Vector3 a = node.corners.get(i);
			for (int j = 0; j < endNode.corners.size; j++) {
				Vector3 b = endNode.corners.get(j);
				float dst = a.dst(b);
				if (dst < minDst) {
					minDst = dst;
				}
			}
		}
		return minDst;
	}
}
