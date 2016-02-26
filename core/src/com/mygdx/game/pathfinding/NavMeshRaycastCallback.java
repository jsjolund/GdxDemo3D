/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.mygdx.game.pathfinding;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btTriangleRaycastCallback;
import com.badlogic.gdx.physics.bullet.linearmath.btVector3;
import com.badlogic.gdx.utils.Bits;

/**
 * @author jsjolund
 */
public class NavMeshRaycastCallback extends btTriangleRaycastCallback {

	public Vector3 hitNormalLocal = new Vector3();
	public float hitFraction = 1;
	public int partId = -1;
	public int triangleIndex = -1;

	private btVector3 tmpSetFrom = new btVector3();
	private btVector3 tmpSetTo = new btVector3();
	private Bits meshPartIndices;

	public NavMeshRaycastCallback(Vector3 from, Vector3 to) {
		super(from, to);
	}

	public void clearReport() {
		hitNormalLocal.setZero();
		hitFraction = 1;
		partId = -1;
		triangleIndex = -1;
	}

	@Override
	public float reportHit(Vector3 hitNormalLocal, float hitFraction, int partId, int triangleIndex) {
		if ((meshPartIndices == null || meshPartIndices.get(partId)) && hitFraction < this.hitFraction) {
			this.hitNormalLocal.set(hitNormalLocal);
			this.hitFraction = hitFraction;
			this.partId = partId;
			this.triangleIndex = triangleIndex;
			return hitFraction;
		}
		return 1;
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
	public void dispose() {
		tmpSetFrom.dispose();
		tmpSetTo.dispose();
		super.dispose();
	}

	public void setAllowedMeshPartIndices(Bits meshPartIndices) {
		this.meshPartIndices = meshPartIndices;
	}
}