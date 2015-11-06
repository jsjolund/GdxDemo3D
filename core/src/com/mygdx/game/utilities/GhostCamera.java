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

package com.mygdx.game.utilities;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

/**
 * @author jsjolund
 */
public class GhostCamera extends PerspectiveCamera {

	public final Vector3 targetPosition = new Vector3();
	public final Vector3 targetDirection = new Vector3();
	public final Vector3 targetUp = new Vector3();

	private final Vector3 tmp = new Vector3();


	public GhostCamera(float fieldOfViewY, float viewportWidth, float viewportHeight) {
		super(fieldOfViewY, viewportWidth, viewportHeight);
		targetPosition.set(super.position);
		targetDirection.set(super.direction);
		targetUp.set(super.up);
	}

	public void update(float alpha) {
		alpha = MathUtils.clamp(alpha, 0.1f, 0.9f);
		position.lerp(targetPosition, alpha);
		direction.lerp(targetDirection, alpha);
		up.lerp(targetUp, alpha);
		super.update();
	}

	public void snapToTarget() {
		position.set(targetPosition);
		direction.set(targetDirection);
		up.set(targetUp);
	}

	public void rotateAround(Vector3 point, Quaternion quat) {
		tmp.set(point).sub(targetPosition);
		targetPosition.add(tmp);
		quat.transform(targetDirection);
		quat.transform(targetUp);
		quat.transform(tmp);
		targetPosition.add(-tmp.x, -tmp.y, -tmp.z);
	}
}
