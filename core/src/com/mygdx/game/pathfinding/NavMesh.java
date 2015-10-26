package com.mygdx.game.pathfinding;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btTriangleIndexVertexArray;
import com.badlogic.gdx.physics.bullet.collision.btTriangleRaycastCallback;
import com.badlogic.gdx.utils.Bits;
import com.badlogic.gdx.utils.Disposable;

/**
 * Created by Johannes Sjolund on 9/19/15.
 */
public class NavMesh implements Disposable {

	public static final String tag = "NavMesh";
	private final Vector3 rayFrom = new Vector3();
	private final Vector3 rayTo = new Vector3();
	public NavMeshGraph graph;
	private btBvhTriangleMeshShape collisionShape;
	private NavMeshRaycastCallback raycastCallback;
	private NavMeshHeuristic heuristic;
	private IndexedAStarPathFinder<Triangle> pathFinder;

	public NavMesh(Model model) {
		btTriangleIndexVertexArray vertexArray = new btTriangleIndexVertexArray(model.meshParts);
		collisionShape = new btBvhTriangleMeshShape(vertexArray, true);
		raycastCallback = new NavMeshRaycastCallback(rayFrom, rayTo);
		raycastCallback.setFlags(btTriangleRaycastCallback.EFlags.kF_FilterBackfaces);
		graph = new NavMeshGraph(model);
		pathFinder = new IndexedAStarPathFinder<Triangle>(graph);
		heuristic = new NavMeshHeuristic();
	}

	public btCollisionShape getShape() {
		return collisionShape;
	}


	@Override
	public void dispose() {
		collisionShape.dispose();
		raycastCallback.dispose();
	}

	public Triangle rayTest(Ray ray, float distance, Bits allowedMeshParts) {
		Triangle hitTriangle = null;

		rayFrom.set(ray.origin);
		rayTo.set(ray.direction).scl(distance).add(rayFrom);
		raycastCallback.setHitFraction(1);
		raycastCallback.clearReport();
		raycastCallback.setFrom(rayFrom);
		raycastCallback.setTo(rayTo);
		raycastCallback.setAllowedMeshPartIndices(allowedMeshParts);
		collisionShape.performRaycast(raycastCallback, rayFrom, rayTo);

		if (raycastCallback.triangleIndex != -1) {
			hitTriangle = graph.getTriangleFromMeshPart(raycastCallback.partId, raycastCallback.triangleIndex);
		}
		return hitTriangle;
	}

	public boolean getPath(Triangle fromTri, Vector3 fromPoint, Ray toRay, Bits allowedMeshParts,
						   float distance, NavMeshGraphPath path) {
		Triangle toTri = rayTest(toRay, distance, allowedMeshParts);
		if (toTri == null) {
			Gdx.app.debug(tag, "To triangle not found.");
			return false;
		}
		Vector3 toPoint = new Vector3();
		Intersector.intersectRayTriangle(toRay, toTri.a, toTri.b, toTri.c, toPoint);

		path.clear();
		if (pathFinder.searchConnectionPath(fromTri, toTri, heuristic, path)) {
			path.start = new Vector3(fromPoint);
			path.end = new Vector3(toPoint);
			path.startTri = fromTri;
			return true;
		}
		Gdx.app.debug(tag, "Path not found.");
		return false;
	}


//	public boolean getPath(Ray fromRay, Ray toRay, Bits allowedMeshParts,
//						   float distance, NavMeshGraphPath path) {
//
//		Triangle toTri = rayTest(toRay, distance, allowedMeshParts);
//		if (toTri == null) {
//			Gdx.app.debug(tag, "To triangle not found.");
//			return false;
//		}
//		Vector3 toPoint = new Vector3();
//		Intersector.intersectRayTriangle(toRay, toTri.a, toTri.b, toTri.c, toPoint);
//
//		Triangle fromTri = rayTest(fromRay, distance, null);
//		if (fromTri == null) {
//			Gdx.app.debug(tag, "From triangle not found.");
//			return false;
//		}
//		Vector3 fromPoint = new Vector3();
//		Intersector.intersectRayTriangle(fromRay, fromTri.a, fromTri.b, fromTri.c, fromPoint);
//
//		path.clear();
//		path.setStartEnd(fromPoint, toPoint);
//
//		if (!calculatePath(fromTri, toTri, path)) {
//			Gdx.app.debug(tag, "Path not found.");
//			return false;
//		}
//
//
//		return true;
//	}

}
