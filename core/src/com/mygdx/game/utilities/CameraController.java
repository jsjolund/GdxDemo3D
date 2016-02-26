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

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.mygdx.game.settings.GameSettings;

/**
 * @author jsjolund
 */
public class CameraController {

	private final GhostCamera camera;

	// World data
	private final Plane worldGroundPlane = new Plane(Vector3.Y, 0);
	private final Vector3 worldDragCurrent = new Vector3();
	private final Vector3 worldDragLast = new Vector3();
	private final Vector3 worldGroundTarget = new Vector3();
	// Temporary
	private final Ray ray = new Ray();
	private final Vector3 tmp1 = new Vector3();
	private final Vector3 tmp2 = new Vector3();
	private final Quaternion deltaRotation = new Quaternion();
	private float zoom = GameSettings.CAMERA_MAX_ZOOM;
	private BoundingBox worldBoundingBox;
	
	private Matrix4 followTarget;

	public CameraController(GhostCamera camera) {
		this.camera = camera;
	}

	public void setWorldBoundingBox(BoundingBox worldBoundingBox) {
		this.worldBoundingBox = new BoundingBox(worldBoundingBox);
		Vector3 min = new Vector3();
		Vector3 max = new Vector3();
		// Set height of bounding box to zero (y dimension)
		this.worldBoundingBox.getMax(max).y = 0;
		this.worldBoundingBox.getMin(min).y = 0;
		this.worldBoundingBox.set(min, max);

		ray.set(camera.targetPosition, camera.targetDirection);
		if (!Intersector.intersectRayBounds(ray, this.worldBoundingBox, worldGroundTarget)) {
			// TODO: What happens if the center of camera is not aimed at bounding box?
			// Probably move the camera until it is...
		}

	}
	
	public void setFollowTarget(Matrix4 followTarget) {
		this.followTarget = followTarget;
	}

	public void processDragPan(Ray dragCurrentRay, Ray lastDragProcessedRay) {
		followTarget = null;
		// TODO:
		// Can probably be optimized, but simply storing worldDragLast.set(worldDragCurrent)
		// caused jitter for some reason.
		Intersector.intersectRayPlane(dragCurrentRay, worldGroundPlane, worldDragCurrent);
		Intersector.intersectRayPlane(lastDragProcessedRay, worldGroundPlane, worldDragLast);
		tmp1.set(worldDragLast).sub(worldDragCurrent);
		tmp1.y = 0;

		ray.origin.set(camera.targetPosition).add(tmp1);
		ray.direction.set(camera.targetDirection);
		if (Intersector.intersectRayBoundsFast(ray, worldBoundingBox)) {
			camera.targetPosition.add(tmp1);
			worldGroundTarget.add(tmp1);
		}
	}

	public void processDragRotation(Vector2 cursorDelta) {
		tmp1.set(camera.targetDirection).crs(camera.targetUp).nor();
		deltaRotation.setEulerAngles(cursorDelta.x, cursorDelta.y * tmp1.x, cursorDelta.y * tmp1.z);
		camera.rotateAround(worldGroundTarget, deltaRotation);
	}

	public void processZoom(float amount) {
		zoom += GameSettings.CAMERA_ZOOM_STEP * amount;
		zoom = MathUtils.clamp(zoom, GameSettings.CAMERA_MIN_ZOOM, GameSettings.CAMERA_MAX_ZOOM);
		camera.targetPosition.set(camera.targetDirection).nor().scl(-zoom).add(worldGroundTarget);
	}

	public void processKeyboardPan(Vector2 keysMoveDirection, float deltaTime) {
		followTarget = null;
		tmp1.set(camera.targetDirection).crs(camera.targetUp).scl(keysMoveDirection.x);
		tmp1.add(tmp2.set(camera.targetDirection).scl(keysMoveDirection.y));
		tmp1.y = 0;
		tmp1.nor().scl(deltaTime * GameSettings.CAMERA_MAX_PAN_VELOCITY);

		ray.origin.set(camera.targetPosition).add(tmp1);
		ray.direction.set(camera.targetDirection);
		if (Intersector.intersectRayBoundsFast(ray, worldBoundingBox)) {
			camera.targetPosition.add(tmp1);
			worldGroundTarget.add(tmp1);
		}
	}

	public void processTouchDownLeft(Ray ray) {
		Intersector.intersectRayPlane(ray, worldGroundPlane, worldDragCurrent);
		worldDragLast.set(worldDragCurrent);
	}

	public void processTouchDownRight() {
//		ray.set(camera.position, camera.direction);
//		Intersector.intersectRayPlane(ray, worldGroundPlane, worldGroundTarget);
	}

	public void update() {
		if (followTarget != null) {
			camera.targetPosition.add(tmp1.set(followTarget.getTranslation(tmp2)).sub(worldGroundTarget));
			worldGroundTarget.set(tmp2);
		}
	}

}
