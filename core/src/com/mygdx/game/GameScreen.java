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

package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.glutils.MipMapGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw.DebugDrawModes;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Bits;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.blender.objects.BlenderCamera;
import com.mygdx.game.blender.objects.BlenderEmpty;
import com.mygdx.game.blender.objects.BlenderLight;
import com.mygdx.game.blender.objects.BlenderModel;
import com.mygdx.game.objects.*;
import com.mygdx.game.scene.GameObjectBlueprint;
import com.mygdx.game.scene.GameScene;
import com.mygdx.game.scene.GameSceneManager;
import com.mygdx.game.settings.DebugViewSettings;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.utilities.CameraController;
import com.mygdx.game.utilities.GhostCamera;
import com.mygdx.game.utilities.ModelFactory;

/**
 * @author jsjolund
 */
public class GameScreen implements Screen {

	private final static String TAG = "GameScreen";

	private final Viewport viewport;
	private final GameStage stage;
	private final GameEngine engine;
	private final Color viewportBackgroundColor;
	private final GhostCamera camera;
	private final AssetManager assets;
	private final ShapeRenderer shapeRenderer;
	private final CameraController cameraController;
	private final GameRenderer renderer;
	private final GameSceneManager sceneManager;

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

		sceneManager = new GameSceneManager("models/g3db/", ".g3db");
		// Create a default scene, which will be the game world.
		GameScene defaultScene = sceneManager.get("scene0");

		// Add placeholders consisting of blender objects to the scene, loaded from blender json.
		// These will later be used in various ways to spawn actual game objects.
		// The reason for this decoupling is that some models have their collision shapes defined as Blender empties,
		// coupled by matching string ids. These are later compared, and used for creating game object blueprints.
		defaultScene.assets.loadPlaceholders("models/json/scene0_model.json", BlenderModel.class);
		defaultScene.assets.loadPlaceholders("models/json/scene0_empty.json", BlenderEmpty.class);
		defaultScene.assets.loadPlaceholders("models/json/scene0_light.json", BlenderLight.class);
		defaultScene.assets.loadPlaceholders("models/json/scene0_camera.json", BlenderCamera.class);

		// Since blender placeholder objects from different .blend scenes might have string name collisions,
		// load each into a new scene  then distribute shared object blueprints among game scenes (only one scene so far).
		sceneManager.get("human_scene").assets.loadPlaceholders("models/json/human_empty.json", BlenderEmpty.class);
		sceneManager.get("human_scene").assets.loadPlaceholders("models/json/human_model.json", BlenderModel.class);
		sceneManager.get("dog_scene").assets.loadPlaceholders("models/json/dog_model.json", BlenderModel.class);
		sceneManager.get("dog_scene").assets.loadPlaceholders("models/json/dog_empty.json", BlenderEmpty.class);

		// Create some game object blueprints which will be shared between game scenes.
		sceneManager.get("utils_scene").assets.manageDisposableFromPath("marker", "images/marker.png", Texture.class);
		Texture texture = sceneManager.get("utils_scene").assets.getAsset("marker", Texture.class);
		Model billboardModel = ModelFactory.buildBillboardModel(texture, 1, 1);
		sceneManager.get("utils_scene").assets.manageDisposable("markerModel", billboardModel, Model.class);
		GameObjectBlueprint markerBlueprint = new GameObjectBlueprint();
		markerBlueprint.name = "marker";
		markerBlueprint.model = billboardModel;
		sceneManager.addSharedBlueprint("marker", markerBlueprint);

		GameObjectBlueprint humanBlueprint = new GameObjectBlueprint();
		humanBlueprint.position = new Vector3();
		humanBlueprint.rotation = new Vector3();
		humanBlueprint.scale = new Vector3(1, 1, 1);
		humanBlueprint.model = sceneManager.get("human_scene").assets.getAsset("human", Model.class);
		humanBlueprint.name = "human";
		humanBlueprint.shape = new btCapsuleShape(0.4f, 1.1f);
		humanBlueprint.shapeType = "capsule"; // TODO: Create enums
		humanBlueprint.mass = 1;
		humanBlueprint.noDeactivate = true;
		humanBlueprint.ragdollJson = "models/json/human_empty.json";
		humanBlueprint.armatureNodeId = "armature";
		humanBlueprint.belongsToFlag = GameEngine.PC_FLAG;
		humanBlueprint.collidesWithFlag = (short) (GameEngine.OBJECT_FLAG | GameEngine.GROUND_FLAG);
		sceneManager.addSharedBlueprint("human", humanBlueprint);

		GameObjectBlueprint dogBlueprint = new GameObjectBlueprint();
		dogBlueprint.position = new Vector3();
		dogBlueprint.rotation = new Vector3();
		dogBlueprint.scale = new Vector3(0.3f, 0.3f, 0.3f);
		dogBlueprint.model = sceneManager.get("dog_scene").assets.getAsset("dog", Model.class);
		dogBlueprint.name = "dog";
		dogBlueprint.shape = new btCapsuleShape(0.4f, 0.5f);
		dogBlueprint.shapeType = "capsule";
		dogBlueprint.mass = 1;
		dogBlueprint.noDeactivate = true;
		dogBlueprint.armatureNodeId = "armature";
		dogBlueprint.belongsToFlag = GameEngine.PC_FLAG;
		dogBlueprint.collidesWithFlag = (short) (GameEngine.OBJECT_FLAG | GameEngine.GROUND_FLAG);
		sceneManager.addSharedBlueprint("dog", dogBlueprint);

		// Spawn game objects for the house, ground, trees and stuff,
		// which were defined in the blender scene json.
		defaultScene.spawnGameObjectsFromPlaceholders();

		defaultScene.setToSceneCamera(camera);

		renderer = new GameRenderer(viewport, camera, engine);
		renderer.setEnvironmentLights(defaultScene.lights, defaultScene.shadowCameraDirection);
		Billboard markerBillboard = defaultScene.spawnSelectionBillboard("marker", camera);
		renderer.setSelectionMarker(markerBillboard);

		cameraController = new CameraController(camera);
		cameraController.setWorldBoundingBox(defaultScene.worldBounds);

		stage = new GameStage(engine, viewport, cameraController);
		stage.addObserver(renderer);
		stage.addObserver(engine);

		// Create humans by supplying the name of the shared blueprint "human", along with position
		HumanCharacter[] humans = new HumanCharacter[] {
			defaultScene.spawnHuman("human", new Vector3(20, 1, 0), 0),
			defaultScene.spawnHuman("human", new Vector3(24, 1, -5)),
			defaultScene.spawnHuman("human", new Vector3(20, 1, 5)),
		};

		// Create dogs by supplying the name of the shared blueprint "dog", along with position
		DogCharacter[] dogs = new DogCharacter[] {
			defaultScene.spawnDog("dog", new Vector3(7, 0.5f, -10)),
			defaultScene.spawnDog("dog", new Vector3(12, 0.5f, 10)),
			defaultScene.spawnDog("dog", new Vector3(15, 0.5f, 4))
		};

		// Assign each dog to a human
		for (int i = 0, n = Math.min(humans.length, dogs.length); i < n; i ++) {
			humans[i].assignDog(dogs[i]);
		}

		// Grabs all the game objects from the scene
		engine.setScene(defaultScene);

		Bits defaultSceneLayers = new Bits();
		for (int i = 0; i <= 2; i++) {
			defaultSceneLayers.set(i);
		}
		stage.setVisibleLayers(defaultSceneLayers);
		
		Array<GameObject> trees = new Array<GameObject>();
		defaultScene.getGameModelById("tree_0", trees);
		for (GameObject obj : trees) {
			GameModel tree = (GameModel) obj;
			Gdx.app.debug(TAG, "Found tree at " + tree.modelTransform.getTranslation(new Vector3()));
		}
	}

	@Override
	public void dispose() {
		stage.dispose();
		assets.dispose();
		shapeRenderer.dispose();
		renderer.dispose();

		engine.dispose();
	}

	@Override
	public void render(float delta) {
		camera.update(delta * GameSettings.CAMERA_LERP_ALPHA);
		stage.act(delta);

		delta *= GameSettings.GAME_SPEED;
		Gdx.gl.glClearColor(0, 0, 0, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(viewportBackgroundColor);
		shapeRenderer.rect(0, 0, viewport.getScreenWidth(), viewport.getScreenHeight());
		shapeRenderer.end();

		engine.update(delta);
		renderer.update(delta);

		if (DebugViewSettings.drawCollShapes || DebugViewSettings.drawConstraints) {
			int mode = 0;
			if (DebugViewSettings.drawConstraints) {
				mode |= DebugDrawModes.DBG_DrawConstraints;
				mode |= DebugDrawModes.DBG_DrawConstraintLimits;
			}
			if (DebugViewSettings.drawCollShapes) {
				mode |= DebugDrawModes.DBG_DrawWireframe;
			}
			engine.setDebugMode(mode);
			engine.debugDrawWorld(camera);
		}
		stage.draw();

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



