package com.mygdx.game.navmesh;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedNode;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * Created by Johannes Sjolund on 9/23/15.
 */
public class Triangle implements IndexedNode<Triangle> {

	public int index;
	public Vector3 a;
	public Vector3 b;
	public Vector3 c;
	public Vector3 centroid;
	public Array<Connection<Triangle>> connections;

	public Triangle(Vector3 a, Vector3 b, Vector3 c, int index) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.index = index;
		this.centroid = new Vector3(a).add(b).add(c).scl(1f / 3f);
		this.connections = new Array<Connection<Triangle>>();
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("Triangle: ");
		sb.append("a=").append(a);
		sb.append(", b=").append(b);
		sb.append(", c=").append(c);
		sb.append(", centroid=").append(centroid);
		return sb.toString();
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public Array<Connection<Triangle>> getConnections() {
		return connections;
	}
}

