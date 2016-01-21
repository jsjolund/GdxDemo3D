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

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * A {@code Triangle} is a node of the {@code NavMeshGraph}.
 * 
 * @author jsjolund
 */
public class Triangle {

	public int triIndex;
	public int meshPartIndex;
	public Vector3 a;
	public Vector3 b;
	public Vector3 c;
	public Vector3 centroid;
	public Array<Connection<Triangle>> connections;

	public Triangle(Vector3 a, Vector3 b, Vector3 c, int triIndex, int meshPartIndex) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.triIndex = triIndex;
		this.meshPartIndex = meshPartIndex;
		this.centroid = new Vector3(a).add(b).add(c).scl(1f / 3f);
		this.connections = new Array<Connection<Triangle>>();
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("Triangle{");
		sb.append("triIndex=").append(triIndex);
		sb.append(", meshPartIndex=").append(meshPartIndex);
		sb.append('}');
		return sb.toString();
	}

	public int getIndex() {
		return triIndex;
	}

	public Array<Connection<Triangle>> getConnections() {
		return connections;
	}

	/**
	 * Calculates the angle in radians between a reference vector and the (plane) normal of the triangle.
	 *
	 * @param reference
	 * @return
	 */
	public float getAngle(Vector3 reference) {
		float x = reference.x;
		float y = reference.y;
		float z = reference.z;
		Vector3 normal = reference;
		normal.set(a).sub(b).crs(b.x - c.x, b.y - c.y, b.z - c.z).nor();
		float angle = (float) Math.acos(normal.dot(x, y, z) / (normal.len() * Math.sqrt(x * x + y * y + z * z)));
		reference.set(x, y, z);
		return angle;
	}

	/**
	 * Calculates a random point in this triangle.
	 *
	 * @param out Output vector
	 * @return Output for chaining
	 */
	public Vector3 getRandomPoint(Vector3 out) {
		final float sr1 = (float) Math.sqrt(MathUtils.random());
		final float r2 = MathUtils.random();
		final float k1 = 1 - sr1;
		final float k2 = sr1 * (1 - r2);
		final float k3 = sr1 * r2;
		out.x = k1 * a.x + k2 * b.x + k3 * c.x;
		out.y = k1 * a.y + k2 * b.y + k3 * c.y;
		out.z = k1 * a.z + k2 * b.z + k3 * c.z;
		return out;
	}

	/**
	 * Calculates the area of the triangle.
	 *
	 * @return
	 */
	public float area() {
		final float abx = b.x - a.x;
		final float aby = b.y - a.y;
		final float abz = b.z - a.z;
		final float acx = c.x - a.x;
		final float acy = c.y - a.y;
		final float acz = c.z - a.z;
		final float r = aby * acz - abz * acy;
		final float s = abz * acx - abx * acz;
		final float t = abx * acy - aby * acx;
		return 0.5f * (float) Math.sqrt(r * r + s * s + t * t);
	}
}

