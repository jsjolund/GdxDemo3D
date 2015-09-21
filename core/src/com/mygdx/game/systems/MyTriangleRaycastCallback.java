package com.mygdx.game.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btTriangleRaycastCallback;
import com.badlogic.gdx.physics.bullet.linearmath.btVector3;

/**
 * Created by Johannes Sjolund on 9/21/15.
 */
public class MyTriangleRaycastCallback extends btTriangleRaycastCallback {

	public static final String tag = "MyTriangleRaycastCallback";

	public Vector3 hitNormalLocal = null;
	public float hitFraction = -1;
	public int partId = -1;
	public int triangleIndex = -1;

	btVector3 tmpSetFrom = new btVector3();
	btVector3 tmpSetTo = new btVector3();

	public MyTriangleRaycastCallback(Vector3 from, Vector3 to) {
		super(from, to);
	}

	public void clear() {
		setHitFraction(1);
		hitNormalLocal = null;
		hitFraction = -1;
		partId = -1;
		triangleIndex = -1;
	}

	public void setFrom(Vector3 value) {
		tmpSetFrom.setValue(value.x, value.y, value.z);
		super.setFrom(tmpSetFrom);
	}


	public void setTo(Vector3 value) {
		tmpSetTo.setValue(value.x, value.y, value.z);
		super.setTo(tmpSetTo);
	}


	@Override
	public float reportHit(Vector3 hitNormalLocal, float hitFraction, int partId, int triangleIndex) {
		this.hitNormalLocal = hitNormalLocal;
		this.hitFraction = hitFraction;
		this.partId = partId;
		this.triangleIndex = triangleIndex;

		Gdx.app.debug(tag, String.format("hitNormalLocal=%s, hitFraction= %s, partId=%s, triangleIndex=%s",
				hitNormalLocal, hitFraction, partId, triangleIndex));

		return 0;
	}
}
