package com.mygdx.game.utilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.Graph;
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
public class NavMesh implements Graph, Disposable {

	public static final String tag = "NavMesh";

	private final MyTriangleRaycastCallback triangleRaycastCallback;
	private final btBvhTriangleMeshShape collisionObject;

	public ArrayMap<Triangle, Array<Connection>> triangleMap;

	private Vector3 rayFrom = new Vector3();
	private Vector3 rayTo = new Vector3();

	public Array<Vector3> pathDebug = new Array<Vector3>();

	public NavMesh(Mesh mesh, btBvhTriangleMeshShape collisionObject) {
		this.collisionObject = collisionObject;
		triangleRaycastCallback = new MyTriangleRaycastCallback(rayFrom, rayTo);
		triangleRaycastCallback.setFlags(btTriangleRaycastCallback.EFlags.kF_FilterBackfaces);
		triangleMap = NavMeshFactory.createNavMeshConnections(mesh);

	}

	public void calculatePath(Vector3 fromVec, Triangle fromTri, Vector3 toVec, Triangle toTri) {
		Gdx.app.debug(tag, String.format("From %s %s\n\tTo %s %s", fromVec, fromTri, toVec, toTri));
		pathDebug.clear();
		pathDebug.add(fromVec);
		pathDebug.add(fromTri.centroid);
		pathDebug.add(toTri.centroid);
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

	@Override
	public Array<Connection> getConnections(Object fromNode) {
		return triangleMap.get((Triangle) fromNode);
	}


}
