/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/


package com.mygdx.game.debugdrawers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Bits;
import com.mygdx.game.objects.GameCharacter;
import com.mygdx.game.pathfinding.*;
import com.mygdx.game.steerers.FollowPathSteerer;
import com.mygdx.game.utilities.MyShapeRenderer;

/**
 * @author jsjolund
 */
public class NavMeshDebugDrawer {

	private final Vector3 tmp1 = new Vector3();
	private final Vector3 tmp2 = new Vector3();
	private final Vector3 tmp3 = new Vector3();
	private final Vector3 tmp4 = new Vector3();
	private final Matrix4 tmpMatrix = new Matrix4();
	private final Quaternion tmpQuat = new Quaternion();
	private Bits visibleLayers;
	private MyShapeRenderer shapeRenderer;
	private NavMesh navMesh;

	private boolean triangleIsVisible(Triangle t) {
		return (visibleLayers.nextSetBit(t.meshPartIndex) != -1);
	}

	private void drawVertex(Vector3 pos, float size, Color color) {
		shapeRenderer.setColor(color);
		float offset = size / 2;
		shapeRenderer.box(pos.x - offset, pos.y - offset, pos.z + offset, size, size, size);
	}

	private void drawNavMeshTriangles() {
		shapeRenderer.set(MyShapeRenderer.ShapeType.Line);
		for (int i = 0; i < navMesh.graph.getNodeCount(); i++) {
			Triangle t = navMesh.graph.getTriangleFromGraphIndex(i);
			if (triangleIsVisible(t)) {
				drawTriangle(t, Color.LIGHT_GRAY, 1);
			}
		}
	}

	private void drawNavMeshIndices(SpriteBatch spriteBatch, Camera camera, BitmapFont font) {
		// TODO: Get rid of all the transform matrix setting
		if (spriteBatch.isDrawing()) {
			spriteBatch.end();
		}
		spriteBatch.begin();
		spriteBatch.setProjectionMatrix(camera.combined);
		for (int i = 0; i < navMesh.graph.getNodeCount(); i++) {
			Triangle t = navMesh.graph.getTriangleFromGraphIndex(i);
			if (triangleIsVisible(t)) {
				tmpMatrix.set(camera.view).inv().getRotation(tmpQuat);
				tmpMatrix.setToTranslation(t.centroid).rotate(tmpQuat);
				spriteBatch.setTransformMatrix(tmpMatrix);
				font.draw(spriteBatch, Integer.toString(t.triIndex), 0, 0);
			}
		}
		spriteBatch.end();
	}

	private void drawTriangle(Triangle tri, Color color, float alpha) {
		shapeRenderer.setColor(color.r, color.g, color.b, alpha);
		shapeRenderer.triangle(tri.a, tri.b, tri.c);
	}

	private void drawPathTriangles(NavMeshGraphPath navMeshGraphPath, Triangle currentTriangle) {
		shapeRenderer.set(MyShapeRenderer.ShapeType.Filled);

		if (navMeshGraphPath != null && navMeshGraphPath.getCount() > 0) {

			for (int i = 0; i < navMeshGraphPath.getCount(); i++) {
				Edge e = (Edge) navMeshGraphPath.get(i);
				if (triangleIsVisible(e.fromNode)) {
					if (currentTriangle.getIndex() == e.fromNode.getIndex()) {
						drawTriangle(e.fromNode, Color.RED, 0.2f);
					} else {
						drawTriangle(e.fromNode, Color.YELLOW, 0.2f);
					}
				}
				if (i == navMeshGraphPath.getCount() - 1) {
					if (triangleIsVisible(e.toNode)) {
						if (currentTriangle.getIndex() == e.toNode.getIndex()) {
							drawTriangle(e.toNode, Color.RED, 0.2f);
						} else {
							drawTriangle(e.toNode, Color.YELLOW, 0.2f);
						}
					}
				}
			}
			// Shared triangle edges
			shapeRenderer.set(MyShapeRenderer.ShapeType.Line);
			for (Connection<Triangle> connection : navMeshGraphPath) {
				Edge e = (Edge) connection;
				if (triangleIsVisible(e.fromNode) || triangleIsVisible(e.toNode)) {
					shapeRenderer.line(e.rightVertex, e.leftVertex, Color.GREEN, Color.RED);
				}
			}
		} else if (currentTriangle != null) {
			drawTriangle(currentTriangle, Color.RED, 0.2f);
		}

	}

	private void drawPathPoints(NavMeshPointPath navMeshPointPath) {
		shapeRenderer.set(MyShapeRenderer.ShapeType.Line);
		Vector3 q;
		Vector3 p = navMeshPointPath.getVector(navMeshPointPath.getSize() - 1);
		shapeRenderer.setColor(Color.WHITE);
		for (int i = navMeshPointPath.getSize() - 1; i >= 0; i--) {
			q = navMeshPointPath.getVector(i);
			shapeRenderer.setColor(Color.CYAN);
			shapeRenderer.line(p, q);
			p = q;
			drawVertex(p, 0.02f, Color.WHITE);
		}
	}

	public void drawClosestPointDebug(GameCharacter character) {
		Vector3 position = tmp1;
		Vector3 direction = tmp2;
		Vector3 closestPoint = tmp3;
		Vector3 aimPoint = tmp4;
		float radius = 2;
		character.getDirection(direction);
		character.getGroundPosition(position);
		aimPoint.set(direction).scl(radius).add(position);
		Triangle closest = navMesh.getClosestValidPointAt(position, direction, radius,
				closestPoint, character.visibleOnLayers);

		shapeRenderer.set(MyShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(0, 0, 1, 0.2f);
		shapeRenderer.triangle(closest.a, closest.b, closest.c);
		drawVertex(aimPoint, 0.05f, Color.GREEN);
		drawVertex(closestPoint, 0.05f, Color.WHITE);

	}

	public void drawNavMesh(MyShapeRenderer shapeRenderer,
							SpriteBatch spriteBatch,
							NavMesh navMesh,
							GameCharacter character,
							Bits visibleLayers,
							Camera camera,
							BitmapFont font) {

		this.visibleLayers = visibleLayers;
		this.shapeRenderer = shapeRenderer;
		this.navMesh = navMesh;

		if (shapeRenderer.isDrawing()) {
			shapeRenderer.end();
		}

		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		shapeRenderer.begin(MyShapeRenderer.ShapeType.Line);
		drawNavMeshTriangles();
		if (character != null && character.steerer instanceof FollowPathSteerer) {
			FollowPathSteerer fpSteerer = (FollowPathSteerer)character.steerer;
			drawPathTriangles(fpSteerer.navMeshGraphPath, character.getCurrentTriangle());
			if (fpSteerer.navMeshPointPath.getSize() > 0) {
				drawPathPoints(fpSteerer.navMeshPointPath);
			}
			drawClosestPointDebug(character);
		}
		shapeRenderer.end();
		Gdx.gl.glDisable(GL20.GL_BLEND);

		drawNavMeshIndices(spriteBatch, camera, font);
	}


}
