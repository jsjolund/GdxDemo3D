package com.mygdx.game.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.GameSettings;
import com.mygdx.game.components.ModelComponent;
import com.mygdx.game.components.SelectableComponent;
import com.mygdx.game.shaders.UberShader;
import com.mygdx.game.utilities.NavMesh;
import com.mygdx.game.utilities.Triangle;


/**
 * Created by user on 7/31/15.
 */
public class RenderSystem extends EntitySystem {

	public static final String tag = "RenderSystem";

	public static final int SHADOW_MAP_WIDTH = 2048;
	public static final int SHADOW_MAP_HEIGHT = 2048;

	public static final float SHADOW_VIEWPORT_HEIGHT = 60;
	public static final float SHADOW_VIEWPORT_WIDTH = 60;
	public static final float SHADOW_NEAR = 1;
	public static final float SHADOW_FAR = 100;
	public static final float SHADOW_INTENSITY = 1f;

	public final Family systemFamily;

	private final ComponentMapper<ModelComponent> models = ComponentMapper.getFor(ModelComponent.class);
	private ComponentMapper<SelectableComponent> selectables = ComponentMapper.getFor(SelectableComponent.class);

	private final Vector3 pos = new Vector3();
	private final ModelBatch modelBatch;
	private ImmutableArray<Entity> entities;
	private Camera camera;

	private final Environment environment;
	private final Viewport viewport;
	private final ShapeRenderer shapeRenderer;

	DirectionalShadowLight shadowLight;
	ModelBatch shadowBatch;

	private final Vector3 debugNodePos = new Vector3();
	private final Vector3 debugModelPos = new Vector3();

	private NavMesh navmesh;

	public RenderSystem(Viewport viewport, Camera camera, Environment environment, Vector3 sunDirection) {
		systemFamily = Family.all(ModelComponent.class).get();

		this.viewport = viewport;
		this.camera = camera;
		this.environment = environment;

		shapeRenderer = new ShapeRenderer();

		environment.add((shadowLight = new DirectionalShadowLight(
				SHADOW_MAP_WIDTH, SHADOW_MAP_HEIGHT,
				SHADOW_VIEWPORT_WIDTH, SHADOW_VIEWPORT_HEIGHT,
				SHADOW_NEAR, SHADOW_FAR))
				.set(SHADOW_INTENSITY, SHADOW_INTENSITY, SHADOW_INTENSITY, sunDirection.nor()));
		environment.shadowMap = shadowLight;
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
	public void addedToEngine(Engine engine) {
		entities = engine.getEntitiesFor(systemFamily);
	}

	private boolean isVisible(final Camera camera, final ModelComponent cmp) {
		cmp.modelInstance.transform.getTranslation(pos);
//		pos.add(cmp.center);
		return camera.frustum.sphereInFrustum(pos, cmp.radius);
	}

	@Override
	public void update(float deltaTime) {
		drawShadowBatch();
		camera.update();
		modelBatch.begin(camera);
		for (int i = 0; i < entities.size(); ++i) {
			Entity entity = entities.get(i);
			ModelComponent mdlCmp = models.get(entity);

			if (isVisible(camera, mdlCmp) || mdlCmp.ignoreCulling) {

				SelectableComponent selCmp = selectables.get(entity);
				if (selCmp != null && selCmp.isSelected) {
					modelBatch.render(selCmp.selectedMarkerModel, environment);
				}
				modelBatch.render(mdlCmp.modelInstance, environment);
			}
		}
		modelBatch.end();

		if (GameSettings.DRAW_ARMATURE) {
			drawArmature();
		}

		if (GameSettings.DRAW_NAVMESH) {
			drawNavMesh();
		}

	}

	private void drawNavMesh() {
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
//		shapeRenderer.identity();
//		shapeRenderer.rotate(1, 0, 0, 90);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		float vertDim = 0.05f;
		float vertOffset = vertDim / 2;
		float nmAlpha = 0.3f;
		for (int i = 0; i < navmesh.triangleMap.size; i++) {
			Triangle t = navmesh.triangleMap.getKeyAt(i);
			shapeRenderer.setColor(0, 1, 0, nmAlpha);
			shapeRenderer.line(t.a, t.b);
			shapeRenderer.line(t.b, t.c);
			shapeRenderer.line(t.c, t.a);
			shapeRenderer.setColor(1, 0, 0, nmAlpha);
			Vector3 c = t.centroid;
			shapeRenderer.box(c.x - vertOffset, c.y - vertOffset, c.z - vertOffset, vertDim, vertDim, vertDim);
		}
		for (int i = 0; i < navmesh.triangleMap.size; i++) {
			Array<Connection<Triangle>> connections = navmesh.triangleMap.getValueAt(i);
			shapeRenderer.setColor(0, 1, 1, nmAlpha);
			for (Connection<Triangle> con : connections) {
				shapeRenderer.line(con.getFromNode().centroid, con.getToNode().centroid);
			}
		}
		shapeRenderer.setColor(1, 1, 1, 1f);
		for (int i = 0; i < navmesh.pathDebug.size - 1; i++) {
			shapeRenderer.line(navmesh.pathDebug.get(i), navmesh.pathDebug.get(i + 1));
		}
		shapeRenderer.end();
		Gdx.gl.glDisable(GL20.GL_BLEND);
	}

	private void drawShadowBatch() {
		int vw = viewport.getScreenWidth();
		int vh = viewport.getScreenHeight();
		int vx = viewport.getScreenX();
		int vy = viewport.getScreenY();

		shadowLight.begin(Vector3.Zero, camera.direction);
		shadowBatch.begin(shadowLight.getCamera());
		for (int i = 0; i < entities.size(); ++i) {
			Entity entity = entities.get(i);
			ModelComponent cmp = models.get(entity);
			shadowBatch.render(cmp.modelInstance);
		}
		shadowBatch.end();
		shadowLight.end();

		viewport.update(vw, vh);
		viewport.setScreenX(vx);
		viewport.setScreenY(vy);
		viewport.apply();
	}

	private void drawArmature() {
		shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(0, 1, 1, 1);

		for (int i = 0; i < entities.size(); ++i) {

			Entity entity = entities.get(i);
			ModelComponent cmp = models.get(entity);
			SelectableComponent selCmp = selectables.get(entity);
			if (selCmp != null) {
				Node skeleton = cmp.modelInstance.getNode("armature");
				if (skeleton != null) {
					cmp.modelInstance.transform.getTranslation(debugModelPos);
					skeleton.globalTransform.getTranslation(debugNodePos);
					drawArmatureNodes(skeleton, debugModelPos, debugNodePos);
				}
			}
		}
		shapeRenderer.end();
	}

	private void drawArmatureNodes(Node currentNode, Vector3 modelPos, Vector3 parentNodePos) {

		Vector3 debugTmp = new Vector3();
		currentNode.globalTransform.getTranslation(debugTmp);
		debugTmp.add(modelPos);
		shapeRenderer.box(debugTmp.x, debugTmp.y, debugTmp.z, 0.01f, 0.01f, 0.01f);
		if (currentNode.hasParent()) {
			shapeRenderer.setColor(1, 1, 0, 1);
			shapeRenderer.line(parentNodePos, debugTmp);
		}
		shapeRenderer.setColor(0, 1, 0, 1);

		if (!currentNode.hasChildren()) {
			return;
		} else {
			for (Node child : currentNode.getChildren()) {
				drawArmatureNodes(child, modelPos, debugTmp);
			}
		}
	}

	public void setNavmesh(NavMesh navmesh) {
		this.navmesh = navmesh;
	}
}
