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
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Bits;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.debugdrawers.ArmatureDebugDrawer;
import com.mygdx.game.debugdrawers.NavMeshDebugDrawer;
import com.mygdx.game.objects.Billboard;
import com.mygdx.game.objects.GameCharacter;
import com.mygdx.game.objects.GameModel;
import com.mygdx.game.settings.DebugViewSettings;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.shaders.UberShader;
import com.mygdx.game.utilities.MyShapeRenderer;
import com.mygdx.game.utilities.Observer;

/**
 * @author jsjolund
 */
public class GameRenderer implements Disposable, Observer {

	/**
	 * Temporary vector that steerers can use to draw
	 */
	public final Vector3 vTmpDraw1 = new Vector3();

	/**
	 * Temporary vector that steerers can use to draw
	 */
	public final Vector3 vTmpDraw2 = new Vector3();

	private final ModelBatch modelBatch;
	private final ModelBatch shadowBatch;
	public final MyShapeRenderer shapeRenderer;
	private final SpriteBatch spriteBatch;
	private final ParticleSystem particleSystem  = ParticleSystem.get();;

	private final Vector3 tmp = new Vector3();
	private final Vector3 cursorWorldPosition = new Vector3();

	public final Viewport viewport;
	private GameCharacter selectedCharacter;
	private BitmapFont font;
	private Camera camera;
	private Environment environment;
	private DirectionalShadowLight shadowLight;
	private GameEngine engine;
	private Billboard markerBillboard;

	private ArmatureDebugDrawer armatureDebugDrawer = new ArmatureDebugDrawer();
	private NavMeshDebugDrawer navMeshDebugDrawer = new NavMeshDebugDrawer();

	public GameRenderer(Viewport viewport, Camera camera, GameEngine engine) {
		this.viewport = viewport;
		this.camera = camera;
		this.engine = engine;

		shapeRenderer = new MyShapeRenderer();
		shapeRenderer.setAutoShapeType(true);

		spriteBatch = new SpriteBatch();
		font = new BitmapFont();
		font.setColor(Color.WHITE);
		font.setUseIntegerPositions(false);
		font.getData().setScale(0.01f);
		shadowBatch = new ModelBatch(new DepthShaderProvider());

		ShaderProgram.pedantic = false;
		final String vertUber = Gdx.files.internal("shaders/uber.vert").readString();
		final String fragUber = Gdx.files.internal("shaders/uber.frag").readString();
		modelBatch = new ModelBatch(new DefaultShaderProvider(vertUber, fragUber) {
			@Override
			protected Shader createShader(final Renderable renderable) {
				return new UberShader(renderable, config);
			}
		});
	}
	
	@Override
	public void notifyEntitySelected(GameCharacter entity) {
		selectedCharacter = entity;
		markerBillboard.setFollowTransform(entity.motionState.transform, entity.selectionMarkerOffset);
		markerBillboard.visibleOnLayers.clear();
		markerBillboard.visibleOnLayers.or(entity.visibleOnLayers);
	}

	@Override
	public void notifyLayerChanged(Bits layer) {

	}

	@Override
	public void notifyCursorWorldPosition(float x, float y, float z) {
		this.cursorWorldPosition.set(x, y, z);
	}

	@Override
	public void dispose() {
		modelBatch.dispose();
		shadowBatch.dispose();
		shapeRenderer.dispose();
		spriteBatch.dispose();
		font.dispose();
		shadowLight.dispose();
	}

	public void setEnvironmentLights(Array<BaseLight<?>> lights, Vector3 sunDirection) {
		environment = new Environment();
		environment.add((shadowLight = new DirectionalShadowLight(
				GameSettings.SHADOW_MAP_WIDTH,
				GameSettings.SHADOW_MAP_HEIGHT,
				GameSettings.SHADOW_VIEWPORT_WIDTH,
				GameSettings.SHADOW_VIEWPORT_HEIGHT,
				GameSettings.SHADOW_NEAR,
				GameSettings.SHADOW_FAR))
				.set(GameSettings.SHADOW_INTENSITY,
						GameSettings.SHADOW_INTENSITY,
						GameSettings.SHADOW_INTENSITY,
						sunDirection.nor()));
		environment.shadowMap = shadowLight;

		float ambientLight = GameSettings.SCENE_AMBIENT_LIGHT;
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, ambientLight, ambientLight, ambientLight, 1));
		for (BaseLight<?> light : lights) {
			environment.add(light);
		}
	}

	/**
	 * Checks if a model is visible using camera frustum culling and model layer visibility.
	 *
	 * @param camera
	 * @param gameModel
	 * @return
	 */
	private boolean isVisible(final Camera camera, final GameModel gameModel) {
		if (!gameModel.visibleOnLayers.intersects(engine.getVisibleLayers())) {
			return false;
		}
		gameModel.modelInstance.transform.getTranslation(tmp);
		tmp.add(gameModel.center);
		return camera.frustum.sphereInFrustum(tmp, gameModel.boundingBoxRadius);
	}

	private void drawShadowBatch() {
		shadowLight.begin(Vector3.Zero, camera.direction);
		shadowBatch.begin(shadowLight.getCamera());

		shadowBatch.render(engine.getModelCache());
		for (GameModel mdl : engine.getDynamicModels()) {
			if (isVisible(camera, mdl)) {
				shadowBatch.render(mdl.modelInstance);
			}
		}
		shadowBatch.end();
		shadowLight.end();
	}

	public void update(float deltaTime) {
		camera.update();
		if (DebugViewSettings.drawModels) {
			drawShadowBatch();
			viewport.apply();
			modelBatch.begin(camera);
			modelBatch.render(engine.getModelCache(), environment);
			for (GameModel mdl : engine.getDynamicModels()) {
				if (isVisible(camera, mdl)) {
					modelBatch.render(mdl.modelInstance, environment);
				}
			}
			if (markerBillboard != null && isVisible(camera, markerBillboard)) {
				modelBatch.render(markerBillboard.modelInstance, environment);
			}
			
			particleSystem.update(); // technically not necessary for rendering
			particleSystem.begin();
			particleSystem.draw();
			particleSystem.end();
			modelBatch.render(particleSystem);

			modelBatch.end();

		}
		if (DebugViewSettings.drawArmature) {
			shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
			armatureDebugDrawer.drawArmature(shapeRenderer, selectedCharacter, "armature");

		}
		if (DebugViewSettings.drawSteering) {
			drawSteering();
		}
		if (DebugViewSettings.drawNavmesh) {
			shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
			navMeshDebugDrawer.drawNavMesh(shapeRenderer, spriteBatch,
					engine.getScene().navMesh, selectedCharacter,
					engine.getVisibleLayers(), viewport.getCamera(), font);
		}
		if (DebugViewSettings.drawMouseNavMeshPos) {
			shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
			drawMouseWorldAxis();
		}
	}

	private void drawModels() {

	}

	private void renderParticleEffects() {

	}

	/**
	 * Draws a kind of compass, showing the world axis
	 */
	private void drawMouseWorldAxis() {
		Vector3 v = cursorWorldPosition;
		if (!Float.isNaN(v.x + v.y + v.z)) { // No vector component is NaN
			shapeRenderer.begin();
			shapeRenderer.line(v.x, v.y, v.z, v.x + 1, v.y, v.z, Color.RED, Color.RED);
			shapeRenderer.line(v.x, v.y, v.z, v.x, v.y + 1, v.z, Color.GREEN, Color.GREEN);
			shapeRenderer.line(v.x, v.y, v.z, v.x, v.y, v.z + 1, Color.BLUE, Color.BLUE);
			shapeRenderer.end();
		}
	}

	/**
	 * Draws the steering behavior of selected character
	 */
	private void drawSteering() {
		if (selectedCharacter != null && selectedCharacter.steerer != null) {
			selectedCharacter.steerer.draw(this);
		}
	}

	public void setSelectionMarker(Billboard markerBillboard) {
		this.markerBillboard = markerBillboard;
	}
}

