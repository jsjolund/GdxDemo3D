package com.mygdx.game.pathfinding;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by Johannes Sjolund on 9/23/15.
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
		sb.append("fromNode=").append(fromNode);
		sb.append(", toNode=").append(toNode);
		sb.append(", rightVertex=").append(rightVertex);
		sb.append(", leftVertex=").append(leftVertex);
		sb.append('}');
		return sb.toString();
	}
}
