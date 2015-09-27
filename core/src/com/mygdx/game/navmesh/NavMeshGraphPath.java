package com.mygdx.game.navmesh;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * Created by Johannes Sjolund on 9/26/15.
 */
public class NavMeshGraphPath extends DefaultGraphPath<Connection<Triangle>> {

	public Vector3 start = new Vector3();
	public Vector3 end = new Vector3();


	public Array<Vector3> getSmoothPath() {

//		Array<Vector3> path = new Array<Vector3>();

		for (int i = 0; i < getCount(); i++) {
			Edge e = (Edge) nodes.get(i);
		}

		return null;
	}

	public void setEndpoints(Vector3 start, Vector3 end) {
		this.start.set(start);
		this.end.set(end);
	}
}
