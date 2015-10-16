package com.mygdx.game.navmesh;

import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btTriangleRaycastCallback;
import com.badlogic.gdx.utils.Disposable;

/**
 * Created by Johannes Sjolund on 9/19/15.
 */
public class NavMesh implements Disposable {

	public static final String tag = "NavMesh";

	public NavMeshGraph graph;
	private btBvhTriangleMeshShape collisionShape;
	private NavMeshRaycastCallback raycastCallback;
	private NavMeshHeuristic heuristic;
	private IndexedAStarPathFinder<Triangle> pathFinder;
	private Vector3 rayFrom = new Vector3();
	private Vector3 rayTo = new Vector3();

	public NavMesh(Model model, btBvhTriangleMeshShape collisionShape) {
		this.collisionShape = collisionShape;
		raycastCallback = new NavMeshRaycastCallback(rayFrom, rayTo);
		raycastCallback.setFlags(btTriangleRaycastCallback.EFlags.kF_FilterBackfaces);
		graph = new NavMeshGraph(model);
		pathFinder = new IndexedAStarPathFinder<Triangle>(graph);
		heuristic = new NavMeshHeuristic();
	}

	public boolean calculatePath(Triangle fromTri, Triangle toTri, NavMeshGraphPath out) {
		if (fromTri == null || toTri == null) {
			return false;
		}
		return pathFinder.searchConnectionPath(fromTri, toTri, heuristic, out);
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
			hitTriangle = graph.getTriangleFromMeshPart(raycastCallback.partId, raycastCallback.triangleIndex);
		}
		return hitTriangle;
	}

}
