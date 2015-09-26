package com.mygdx.game.utilities;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by Johannes Sjolund on 9/23/15.
 */

public class Edge implements Connection<Triangle> {

	public Vector3 edgeVertexA;
	public Vector3 edgeVertexB;

	public Triangle fromNode;
	public Triangle toNode;

	public Edge(Triangle fromNode, Triangle toNode,
				Vector3 edgeVertexA, Vector3 edgeVertexB) {
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.edgeVertexA = edgeVertexA;
		this.edgeVertexB = edgeVertexB;
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
}
