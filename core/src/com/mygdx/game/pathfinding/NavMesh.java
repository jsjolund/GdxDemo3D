/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/


package com.mygdx.game.pathfinding;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btTriangleIndexVertexArray;
import com.badlogic.gdx.physics.bullet.collision.btTriangleRaycastCallback;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Bits;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.FloatArray;

/**
 * @author jsjolund
 */
public class NavMesh implements Disposable {

	public static final String tag = "NavMesh";
	private final Vector3 rayFrom = new Vector3();
	private final Vector3 rayTo = new Vector3();
	private final FloatArray triAreasTmp = new FloatArray();
	private final Array<Triangle> trisTmp = new Array<Triangle>();
	private final Bits allMeshPartsTmp = new Bits();
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

	/**
	 * Get the triangle which this ray intersects. Returns null if no triangle is intersected.
	 *
	 * @param ray
	 * @param distance
	 * @param allowedMeshParts
	 * @return
	 */
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

	/**
	 * Calculate a path of triangles from a start triangle to the triangle which is intersected by a ray.
	 *
	 * @param fromTri
	 * @param fromPoint
	 * @param toRay
	 * @param allowedMeshParts
	 * @param distance
	 * @param path
	 * @return
	 */
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

	/**
	 * Get a random triangle anywhere on the navigation mesh.
	 * The probability distribution is even in world space, as opposed to triangle index,
	 * meaning large triangles will be chosen more often than small ones.
	 */
	public Triangle getRandomTriangle() {
		allMeshPartsTmp.clear();
		for (int i = 0; i < graph.getMeshPartCount(); i++) {
			allMeshPartsTmp.set(i);
		}
		return getRandomTriangle(allMeshPartsTmp);
	}

	/**
	 * Get a random triangle on the navigation mesh, on any of the allowed mesh parts.
	 * The probability distribution is even in world space, as opposed to triangle index,
	 * meaning large triangles will be chosen more often than small ones.
	 * <p/>
	 * Example usage, to get a random point on the second navigation mesh part:
	 * allowedMeshParts.clear();
	 * allowedMeshParts.set(1);
	 * Triangle randomTri = navmesh.getRandomTriangle(allowedMeshParts);
	 * Vector3 randomPoint = new Vector3();
	 * randomTri.getRandomPoint(randomPoint);
	 *
	 * @param allowedMeshParts Bits representing allowed mesh part indices.
	 * @return A random triangle.
	 */
	public Triangle getRandomTriangle(Bits allowedMeshParts) {
		triAreasTmp.clear();
		triAreasTmp.ordered = true;
		trisTmp.clear();
		trisTmp.ordered = true;

		// To get a uniform distribution over the triangles in the mesh parts
		// we must take areas of the triangles into account.
		for (int mpIndex = 0; mpIndex < graph.getMeshPartCount(); mpIndex++) {
			if (allowedMeshParts.get(mpIndex)) {
				for (int triIndex = 0; triIndex < graph.getTriangleCount(mpIndex); triIndex++) {
					Triangle tri = graph.getTriangleFromMeshPart(mpIndex, triIndex);
					float integratedArea = 0;
					if (triAreasTmp.size > 0) {
						integratedArea = triAreasTmp.get(triAreasTmp.size - 1);
					}
					triAreasTmp.add(integratedArea + tri.area());
					trisTmp.add(tri);
				}
			}
		}
		if (triAreasTmp.size == 0) {
			return null;
		}
		float r = MathUtils.random(0f, triAreasTmp.get(triAreasTmp.size - 1));
		int i;
		for (i = 0; i < triAreasTmp.size; i++) {
			if (r <= triAreasTmp.get(i)) {
				break;
			}
		}
		return trisTmp.get(i);
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
