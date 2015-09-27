package com.mygdx.game.navmesh;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;

/**
 * Created by Johannes Sjolund on 9/27/15.
 */
public class NavMeshGraph implements IndexedGraph<Triangle> {

	private ArrayMap<Triangle, Array<Connection<Triangle>>> map;

	public NavMeshGraph(ArrayMap<Triangle, Array<Connection<Triangle>>> map) {
		this.map = map;
	}

	@Override
	public int getNodeCount() {
		return map.size;
	}

	@Override
	public Array<Connection<Triangle>> getConnections(Triangle fromNode) {
		return map.getValueAt(fromNode.index);
	}

	public Triangle getTriangleFromIndex(int index) {
		return map.getKeyAt(index);
	}

}
