package com.mygdx.game.utilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btTriangleRaycastCallback;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Disposable;
import com.mygdx.game.systems.MyTriangleRaycastCallback;

/**
 * Created by Johannes Sjolund on 9/19/15.
 */
public class NavMesh implements IndexedGraph<Triangle>, Disposable {

	public static final String tag = "NavMesh";

	private final MyTriangleRaycastCallback triangleRaycastCallback;
	private final btBvhTriangleMeshShape collisionObject;

	public ArrayMap<Triangle, Array<Connection<Triangle>>> triangleMap;
	public Array<Vector3> pathDebug = new Array<Vector3>();
	private Vector3 rayFrom = new Vector3();
	private Vector3 rayTo = new Vector3();

	private IndexedAStarPathFinder<Triangle> pathFinder;
	private TriangleHeuristic heuristic;


	public NavMesh(Mesh mesh, btBvhTriangleMeshShape collisionObject) {
		this.collisionObject = collisionObject;
		triangleRaycastCallback = new MyTriangleRaycastCallback(rayFrom, rayTo);
		triangleRaycastCallback.setFlags(
				btTriangleRaycastCallback.EFlags.kF_FilterBackfaces);
		triangleMap = NavMeshFactory.createNavMeshConnections(mesh);
		pathFinder = new IndexedAStarPathFinder<Triangle>(this);
		heuristic = new TriangleHeuristic();
	}

	@Override
	public int getNodeCount() {
		return triangleMap.size;
	}

	@Override
	public Array<Connection<Triangle>> getConnections(Triangle fromNode) {
		return triangleMap.get(fromNode);
	}

	public void calculatePath(Triangle fromTri, Triangle toTri, Vector3 fromVec, Vector3 toVec) {
		Gdx.app.debug(tag, String.format("From %s %s\n\tTo %s %s", fromVec, fromTri, toVec, toTri));
		pathDebug.clear();
		pathDebug.add(fromVec);
		GraphPath<Triangle> path = new DefaultGraphPath<Triangle>();
		pathFinder.searchNodePath(fromTri, toTri, heuristic, path);
		for (Triangle tri : path) {
			pathDebug.add(tri.centroid);
		}
		pathDebug.add(toVec);
	}

	@Override
	public void dispose() {
		collisionObject.dispose();
		triangleRaycastCallback.dispose();
	}

	public Triangle rayTest(Ray ray, float maxDistance) {
		Triangle hitTriangle = null;

		rayFrom.set(ray.origin);
		rayTo.set(ray.direction).scl(maxDistance).add(rayFrom);
		triangleRaycastCallback.setHitFraction(1);
		triangleRaycastCallback.clearReport();
		triangleRaycastCallback.setFrom(rayFrom);
		triangleRaycastCallback.setTo(rayTo);
		collisionObject.performRaycast(triangleRaycastCallback, rayFrom, rayTo);

		if (triangleRaycastCallback.triangleIndex != -1) {
			hitTriangle = triangleMap.getKeyAt(triangleRaycastCallback.triangleIndex);
			Gdx.app.debug(tag, hitTriangle.toString());
		}
		return hitTriangle;
	}

	private class TriangleHeuristic implements Heuristic<Triangle> {
		@Override
		public float estimate(Triangle node, Triangle endNode) {
			return node.centroid.dst2(endNode.centroid);
		}
	}


}
