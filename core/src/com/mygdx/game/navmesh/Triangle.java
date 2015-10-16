package com.mygdx.game.navmesh;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedNode;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * Created by Johannes Sjolund on 9/23/15.
 */
public class Triangle implements IndexedNode<Triangle> {

	public int triIndex;
	public int meshPartIndex;
	public Vector3 a;
	public Vector3 b;
	public Vector3 c;
	public Vector3 centroid;
	public Array<Connection<Triangle>> connections;
	public Array<Vector3> corners;

	public Triangle(Vector3 a, Vector3 b, Vector3 c, int triIndex, int meshPartIndex) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.triIndex = triIndex;
		this.meshPartIndex = meshPartIndex;
		this.centroid = new Vector3(a).add(b).add(c).scl(1f / 3f);
		this.connections = new Array<Connection<Triangle>>();

		corners = new Array<Vector3>();
		corners.ordered = true;
		corners.add(a);
		corners.add(b);
		corners.add(c);
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("Triangle{");
		sb.append("triIndex=").append(triIndex);
		sb.append(", meshPartIndex=").append(meshPartIndex);
		sb.append('}');
		return sb.toString();
	}

	@Override
	public int getIndex() {
		return triIndex;
	}

	@Override
	public Array<Connection<Triangle>> getConnections() {
		return connections;
	}
}

