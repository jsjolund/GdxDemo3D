package com.mygdx.game.navmesh;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btTriangleRaycastCallback;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Disposable;

/**
 * Created by Johannes Sjolund on 9/19/15.
 */
public class NavMesh implements Disposable {

	public static final String tag = "NavMesh";

	public NavMeshGraph graph;
	public NavMeshGraphPath debugPath;
	private btBvhTriangleMeshShape collisionShape;
	private NavMeshRaycastCallback raycastCallback;
	private NavMeshHeuristic heuristic;
	private IndexedAStarPathFinder<Triangle> pathFinder;

	private Vector3 rayFrom = new Vector3();
	private Vector3 rayTo = new Vector3();

	public NavMesh(Mesh mesh, btBvhTriangleMeshShape collisionShape) {
		this.collisionShape = collisionShape;
		raycastCallback = new NavMeshRaycastCallback(rayFrom, rayTo);
		raycastCallback.setFlags(
				btTriangleRaycastCallback.EFlags.kF_FilterBackfaces);

		ArrayMap<Triangle, Array<Connection<Triangle>>> map = NavMeshFactory.createConnectionMap(mesh);
		graph = new NavMeshGraph(map);
		pathFinder = new IndexedAStarPathFinder<Triangle>(graph);
		heuristic = new NavMeshHeuristic();
	}

	public void calculatePath(Triangle fromTri, Triangle toTri, Vector3 fromVec, Vector3 toVec) {
//		Gdx.app.debug(tag, String.format("From %s %s\n\tTo %s %s", fromVec, fromTri, toVec, toTri));
		NavMeshGraphPath path = new NavMeshGraphPath();
		pathFinder.searchConnectionPath(fromTri, toTri, heuristic, path);
		path.setEndpoints(fromVec, toVec);
		debugPath = path;
	}

	@Override
	public void dispose() {
		collisionShape.dispose();
		raycastCallback.dispose();
	}

	public Triangle rayTest(Ray ray, float maxDistance) {
		Triangle hitTriangle = null;

		rayFrom.set(ray.origin);
		rayTo.set(ray.direction).scl(maxDistance).add(rayFrom);
		raycastCallback.setHitFraction(1);
		raycastCallback.clearReport();
		raycastCallback.setFrom(rayFrom);
		raycastCallback.setTo(rayTo);
		collisionShape.performRaycast(raycastCallback, rayFrom, rayTo);

		if (raycastCallback.triangleIndex != -1) {
			hitTriangle = graph.getTriangleFromIndex(raycastCallback.triangleIndex);
//			Gdx.app.debug(tag, hitTriangle.toString());
		}
		return hitTriangle;
	}

}
