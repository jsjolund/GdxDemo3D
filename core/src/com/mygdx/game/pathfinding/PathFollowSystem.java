package com.mygdx.game.pathfinding;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.mygdx.game.components.PathFindingComponent;
import com.mygdx.game.components.PhysicsComponent;
import com.mygdx.game.components.RagdollComponent;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.systems.PhysicsSystem;
import com.mygdx.game.utilities.Observer;

/**
 * Created by user on 8/30/15.
 */
public class PathFollowSystem extends IteratingSystem implements Observer {

	public static final String tag = "PathFollowSystem";

	private final Matrix4 matrix = new Matrix4();
	private final Vector3 goalDirection = new Vector3();
	private final Vector3 newVelocity = new Vector3();
	private final Vector3 surfaceHitPoint = new Vector3();

	private final ComponentMapper<PathFindingComponent> pathCmps = ComponentMapper.getFor(PathFindingComponent.class);
	private final ComponentMapper<PhysicsComponent> phyCmps = ComponentMapper.getFor(PhysicsComponent.class);
	private final ComponentMapper<RagdollComponent> ragCmp = ComponentMapper.getFor(RagdollComponent.class);

	private final PhysicsSystem phySys;

	private NavMesh navMesh;
	private Entity selectedEntity;
	private int selectedLayer;

	public PathFollowSystem(PhysicsSystem phySys) {
		super(Family.all(PathFindingComponent.class).one(PhysicsComponent.class, RagdollComponent.class).get());
		this.phySys = phySys;
	}

	@Override
	protected void processEntity(Entity entity, float deltaTime) {
		PathFindingComponent pathCmp = pathCmps.get(entity);
		PhysicsComponent phyCmp = phyCmps.get(entity);
		if (phyCmp == null) {
			phyCmp = ragCmp.get(entity);
		}

		if (pathCmp.currentGoal == null && pathCmp.path.size == 0) {
			pathCmp.goalReached = true;
			return;
		}

		if (pathCmp.currentGoal != null) {
			float yVelocity = phyCmp.body.getLinearVelocity().y;

			phyCmp.body.getWorldTransform(matrix);
			matrix.getTranslation(pathCmp.currentPosition);

			float xzDst = Vector2.dst2(pathCmp.currentGoal.x, pathCmp.currentGoal.z,
					pathCmp.currentPosition.x, pathCmp.currentPosition.z);

			if (xzDst < 0.1f) {

				// set new goal if not empty
				pathCmp.currentGoal = null;
				if (pathCmp.path.size > 0) {
					pathCmp.currentGoal = pathCmp.path.pop();
				} else {
					phyCmp.body.setLinearVelocity(newVelocity.set(0, yVelocity, 0));
					phyCmp.body.setAngularVelocity(Vector3.Zero);
				}

			} else {
				matrix.idt();
				goalDirection.set(pathCmp.currentPosition).sub(pathCmp.currentGoal).scl(-1, 0, 1).nor();
				matrix.setToLookAt(goalDirection, Vector3.Y);
				matrix.setTranslation(pathCmp.currentPosition);
				phyCmp.body.setWorldTransform(matrix);

				newVelocity.set(goalDirection.scl(1, 0, -1)).scl(pathCmp.moveSpeed);
				pathCmp.goalReached = false;

				newVelocity.y = yVelocity;
				phyCmp.body.setLinearVelocity(newVelocity);
			}
		}
	}

	public void setNavMesh(NavMesh navMesh) {
		this.navMesh = navMesh;
	}

	public void checkNavMeshClick(Ray pickRay) {
		if (selectedEntity == null) {
			return;
		}
		PathFindingComponent pathCmp = pathCmps.get(selectedEntity);
		if (pathCmp == null) {
			return;
		}
		// Check if player clicked navigation mesh
		if ((phySys.rayTest(pickRay, surfaceHitPoint, PhysicsSystem.NAVMESH_FLAG,
				PhysicsSystem.NAVMESH_FLAG, GameSettings.CAMERA_PICK_RAY_DST)) == null) {
			return;
		}
		Gdx.app.debug(tag, "Clicked navmesh " + surfaceHitPoint);

		// Check which navmesh triangle was hit
		Triangle hitTriangle = navMesh.rayTest(pickRay, GameSettings.CAMERA_PICK_RAY_DST, selectedLayer);
		if (hitTriangle == null) {
			return;
		}
		Intersector.intersectRayTriangle(pickRay, hitTriangle.a, hitTriangle.b, hitTriangle.c, surfaceHitPoint);
		// Check which triangle the entity is currently standing on
		pathCmp.posGroundRay.origin.set(pathCmp.currentPosition);
		Triangle posTriangle = navMesh.rayTest(pathCmp.posGroundRay,
				GameSettings.CAMERA_PICK_RAY_DST, Integer.MAX_VALUE);

		if (posTriangle == null) {
				/*
				 Player was likely on the edge of the navmesh, and triangle raycast
				 was unable to find which triangle the player stands on.
				 TODO: remove this
				 The path data structure should keep track of which triangle the
				 player is currently on so ray test on current position is not
				 necessary...
				 */
			Gdx.app.debug(tag, "Checking neighbouring positions");
			while (true) {
				float offs = 0.2f;
				pathCmp.posGroundRay.origin.set(pathCmp.currentPosition).sub(-offs, 0, 0);
				if ((posTriangle = navMesh.rayTest(pathCmp.posGroundRay,
						GameSettings.CAMERA_PICK_RAY_DST, Integer.MAX_VALUE)) != null) {
					break;
				}
				pathCmp.posGroundRay.origin.set(pathCmp.currentPosition).sub(offs, 0, 0);
				if ((posTriangle = navMesh.rayTest(pathCmp.posGroundRay,
						GameSettings.CAMERA_PICK_RAY_DST, Integer.MAX_VALUE)) != null) {
					break;
				}
				pathCmp.posGroundRay.origin.set(pathCmp.currentPosition).sub(0, 0, -offs);
				if ((posTriangle = navMesh.rayTest(pathCmp.posGroundRay,
						GameSettings.CAMERA_PICK_RAY_DST, Integer.MAX_VALUE)) != null) {
					break;
				}
				pathCmp.posGroundRay.origin.set(pathCmp.currentPosition).sub(0, 0, offs);
				posTriangle = navMesh.rayTest(pathCmp.posGroundRay,
						GameSettings.CAMERA_PICK_RAY_DST, Integer.MAX_VALUE);
				break;
			}

		}
		Vector3 posPoint = new Vector3(pathCmp.currentPosition).sub(0, GameSettings
				.CHAR_CAPSULE_Y_HALFEXT, 0);
		pathCmp.clearPath();
		boolean pathFound = navMesh.calculatePath(posTriangle, hitTriangle, pathCmp.trianglePath);
		pathCmp.trianglePath.setStartEnd(posPoint, surfaceHitPoint);
		pathCmp.setPath(pathCmp.trianglePath.getSmoothPath());

		if (!pathFound) {
			Gdx.app.debug(tag, "Path not found");
		}

	}

	@Override
	public void notifyEntitySelected(Entity entity) {
		this.selectedEntity = entity;
	}

	@Override
	public void notifyLayerSelected(int layer) {
		this.selectedLayer = layer;
	}
}
