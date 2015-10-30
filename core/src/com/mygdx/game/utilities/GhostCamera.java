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

package com.mygdx.game.utilities;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

/**
 * @author jsjolund
 */
public class GhostCamera extends PerspectiveCamera {

	public final Vector3 position = new Vector3();
	public final Vector3 direction = new Vector3();
	public final Vector3 up = new Vector3();
	private final Vector3 tmp = new Vector3();


	public GhostCamera(float fieldOfViewY, float viewportWidth, float viewportHeight) {
		super(fieldOfViewY, viewportWidth, viewportHeight);
		position.set(super.position);
		direction.set(super.direction);
		up.set(super.up);
	}

	public void update(float deltaTime, float alpha) {
		alpha *= deltaTime;
		super.position.lerp(position, alpha);
		super.direction.lerp(direction, alpha);
		super.up.lerp(up, alpha);
		super.update();
	}

	public void rotateAround(Vector3 point, Quaternion quat) {
		tmp.set(point).sub(position);
		position.add(tmp);
		quat.transform(direction);
		quat.transform(up);
		quat.transform(tmp);
		position.add(-tmp.x, -tmp.y, -tmp.z);
	}
}
