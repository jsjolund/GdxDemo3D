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

package com.mygdx.game;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.glutils.MipMapGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw.DebugDrawModes;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.blender.BlenderScene;
import com.mygdx.game.objects.Billboard;
import com.mygdx.game.objects.GameCharacter;
import com.mygdx.game.settings.DebugViewSettings;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.utilities.CameraController;
import com.mygdx.game.utilities.GhostCamera;
import com.mygdx.game.utilities.ModelFactory;

/**
 * @author jsjolund
 */
public class GameScreen implements Screen {

	private final static String tag = "GameScreen";

	private final Viewport viewport;
	private final GameStage stage;
	private final GameEngine engine;
	private final Color viewportBackgroundColor;
	private final GhostCamera camera;
	private final AssetManager assets;
	private final ShapeRenderer shapeRenderer;
	private final btIDebugDraw debugDraw;
	private final Array<BlenderScene> scenes = new Array<BlenderScene>();
	private final CameraController cameraController;
	private final GameRenderer renderSys;

	public GameScreen(int reqWidth, int reqHeight) {
		Bullet.init();
		MipMapGenerator.setUseHardwareMipMap(true);

		assets = new AssetManager();
		engine = new GameEngine();
		viewportBackgroundColor = Color.BLACK;

		camera = new GhostCamera(GameSettings.CAMERA_FOV, reqWidth, reqHeight);
		camera.near = GameSettings.CAMERA_NEAR;
		camera.far = GameSettings.CAMERA_FAR;
		camera.update();
		viewport = new FitViewport(reqWidth, reqHeight, camera);

		shapeRenderer = new ShapeRenderer();

		BlenderScene blenderScene = new BlenderScene(
				"models/json/scene0_model.json",
				"models/json/scene0_empty.json",
				"models/json/scene0_light.json",
				"models/json/scene0_camera.json"
		);
		scenes.add(blenderScene);

		engine.navmesh = blenderScene.navMesh;
		engine.navmeshEntity = blenderScene.navmeshEntity;
		engine.addEntity(blenderScene.navmeshEntity);

		blenderScene.setToSceneCamera(camera);

		renderSys = new GameRenderer(viewport, camera, engine);
		renderSys.setEnvironmentLights(blenderScene.lights, blenderScene.shadowCameraDirection);

		debugDraw = engine.dynamicsWorld.getDebugDrawer();

		for (Entity entity : blenderScene.entities) {
			engine.addEntity(entity);
		}
		for (Entity entity : blenderScene.ghosts) {
			engine.addEntity(entity);
		}

		cameraController = new CameraController(camera);
		cameraController.setWorldBoundingBox(blenderScene.worldBounds);
		stage = new GameStage(engine, viewport, cameraController);
		stage.addObserver(renderSys);

		GameCharacter character = spawnCharacter(new Vector3(5, 1, 0));
		engine.addEntity(character);
		engine.addEntity(spawnCharacter(new Vector3(0, 1, 5)));
		engine.addEntity(spawnCharacter(new Vector3(10, 1, 5)));
		engine.addEntity(spawnCharacter(new Vector3(-12, 4, 10)));

		// Selection billboard
		TextureLoader.TextureParameter param = new TextureLoader.TextureParameter();
		param.genMipMaps = true;
		param.minFilter = Texture.TextureFilter.MipMap;
		param.magFilter = Texture.TextureFilter.Linear;
		assets.load("images/marker.png", Texture.class, param);
		assets.finishLoading();
		Texture billboardPixmap = assets.get("images/marker.png", Texture.class);
		// TODO: dispose or use an asset manager model
		Model billboardModel = ModelFactory.buildBillboardModel(billboardPixmap, 1, 1);
		Billboard markerBillboard = new Billboard(billboardModel, "marker", camera, true, new Matrix4(),
				new Vector3(0, -character.halfExtents.y * 0.95f, 0));
		renderSys.setSelectionMarker(markerBillboard);
		engine.addEntity(markerBillboard);
	}

	@Override
	public void dispose() {
		stage.dispose();
		assets.dispose();
		shapeRenderer.dispose();
		debugDraw.dispose();
		renderSys.dispose();
		for (BlenderScene scene : scenes) {
			scene.dispose();
		}
		scenes.clear();
		engine.dispose();
	}

	@Override
	public void render(float delta) {
		camera.update(delta, GameSettings.CAMERA_LERP_ALPHA);
		delta *= GameSettings.GAME_SPEED;
		Gdx.gl.glClearColor(0, 0, 0, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(viewportBackgroundColor);
		shapeRenderer.rect(0, 0, viewport.getScreenWidth(), viewport.getScreenHeight());
		shapeRenderer.end();

		engine.update(delta);
		renderSys.update(delta);

		if (DebugViewSettings.drawCollShapes || DebugViewSettings.drawConstraints) {
			int mode = 0;
			if (DebugViewSettings.drawConstraints) {
				mode |= DebugDrawModes.DBG_DrawConstraints;
				mode |= DebugDrawModes.DBG_DrawConstraintLimits;
			}
			if (DebugViewSettings.drawCollShapes) {
				mode |= DebugDrawModes.DBG_DrawWireframe;
			}
			debugDraw.setDebugMode(mode);
			engine.debugDrawWorld(camera);
		}
		stage.act(delta);
		stage.draw();

	}

	private GameCharacter spawnCharacter(Vector3 initialPosition) {

		short belongsToFlag = GameEngine.PC_FLAG;
		short collidesWithFlag = (short) (GameEngine.OBJECT_FLAG | GameEngine.GROUND_FLAG);

		// Model
		ModelLoader.ModelParameters param = new ModelLoader.ModelParameters();
		param.textureParameter.genMipMaps = true;
		param.textureParameter.minFilter = Texture.TextureFilter.MipMap;
		param.textureParameter.magFilter = Texture.TextureFilter.Linear;
		assets.load("models/g3db/character_male_base.g3db", Model.class, param);
		assets.finishLoading();
		Model model = assets.get("models/g3db/character_male_base.g3db");
		btCollisionShape shape = new btCapsuleShape(0.4f, 1.1f);
		float mass = 1;
		boolean callback = false;
		boolean noDeactivate = true;
		String ragdollJson = "models/json/character_empty.json";
		String armatureNodeId = "armature";

		GameCharacter character = new GameCharacter(
				model, "character",
				initialPosition, new Vector3(0, 0, 0), new Vector3(1, 1, 1),
				shape, mass, belongsToFlag, collidesWithFlag,
				callback, noDeactivate, ragdollJson, armatureNodeId);
		character.ignoreCulling = true;
		Ray posGroundRay = new Ray(initialPosition, new Vector3(0, -1, 0));
		character.currentTriangle = engine.navmesh.rayTest(posGroundRay, 100, null);
		character.layers.set(character.currentTriangle.meshPartIndex);
		return character;
	}

	@Override
	public void show() {
		// TODO Auto-generated method stub
	}

	@Override
	public void resize(int width, int height) {
		stage.resize(width, height);
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub
	}


}



