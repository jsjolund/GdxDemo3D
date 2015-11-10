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

package com.mygdx.game.pathfinding;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.math.Vector3;

/**
 * @author jsjolund
 */
public class Edge implements Connection<Triangle> {

	public Vector3 rightVertex;
	public Vector3 leftVertex;

	public Triangle fromNode;
	public Triangle toNode;

	public Edge(Triangle fromNode, Triangle toNode,
				Vector3 rightVertex, Vector3 leftVertex) {
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.rightVertex = rightVertex;
		this.leftVertex = leftVertex;
	}

	@Override
	public float getCost() {
		return 1;
	}

	@Override
	public Triangle getFromNode() {
		return fromNode;
	}

	@Override
	public Triangle getToNode() {
		return toNode;
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("Edge{");
		sb.append("fromNode=").append(fromNode.triIndex);
		sb.append(", toNode=").append(toNode == null ? "null" : toNode.triIndex);
		sb.append(", rightVertex=").append(rightVertex);
		sb.append(", leftVertex=").append(leftVertex);
		sb.append('}');
		return sb.toString();
	}
}
