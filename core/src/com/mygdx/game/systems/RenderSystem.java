package com.mygdx.game.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.components.ModelComponent;
import com.mygdx.game.components.SelectableComponent;
import com.mygdx.game.navmesh.Edge;
import com.mygdx.game.navmesh.NavMesh;
import com.mygdx.game.navmesh.NavMeshGraphPath;
import com.mygdx.game.navmesh.Triangle;
import com.mygdx.game.settings.DebugViewSettings;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.shaders.UberShader;
import com.mygdx.game.utilities.MyShapeRenderer;


/**
 * Created by user on 7/31/15.
 */
public class RenderSystem extends EntitySystem implements Disposable {

	public static final String tag = "RenderSystem";

	public final Family systemFamily;
	private final ComponentMapper<ModelComponent> models = ComponentMapper.getFor(ModelComponent.class);
	private final ModelBatch modelBatch;
	private final ModelBatch shadowBatch;
	private final MyShapeRenderer shapeRenderer;
	private final SpriteBatch spriteBatch;
	private final Vector3 tmp = new Vector3();
	private final Vector3 debugNodePos = new Vector3();
	private final Vector3 debugModelPos = new Vector3();
	private final Matrix4 tmpMatrix = new Matrix4();
	private final Quaternion tmpQuat = new Quaternion();
	private final Viewport viewport;
	private ComponentMapper<SelectableComponent> selectables = ComponentMapper.getFor(SelectableComponent.class);
	private ImmutableArray<Entity> entities;
	private BitmapFont font;
	private Camera camera;
	private Environment environment;
	private DirectionalShadowLight shadowLight;
	private NavMesh navmesh;

	public RenderSystem(Viewport viewport, Camera camera) {
		systemFamily = Family.all(ModelComponent.class).get();
		this.viewport = viewport;
		this.camera = camera;

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
	public void dispose() {
		modelBatch.dispose();
		shadowBatch.dispose();
		shapeRenderer.dispose();
		spriteBatch.dispose();
		font.dispose();
		shadowLight.dispose();
	}

	public void setEnvironmentLights(Array<BaseLight> lights, Vector3 sunDirection) {
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
		for (BaseLight light : lights) {
			environment.add(light);
		}
	}

	@Override
	public void addedToEngine(Engine engine) {
		entities = engine.getEntitiesFor(systemFamily);
	}

	private boolean isVisible(final Camera camera, final ModelComponent cmp) {
		cmp.modelInstance.transform.getTranslation(tmp);
		return camera.frustum.sphereInFrustum(tmp, cmp.radius);
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

		if (DebugViewSettings.drawArmature) {
			drawArmature();
		}

		if (DebugViewSettings.drawNavmesh) {
			drawNavMesh();
		}

	}

	private void drawNavMesh() {
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
		shapeRenderer.begin(MyShapeRenderer.ShapeType.Line);
		for (int i = 0; i < navmesh.graph.getNodeCount(); i++) {
			Triangle t = navmesh.graph.getTriangleFromIndex(i);
			shapeRenderer.setColor(Color.LIGHT_GRAY);
			shapeRenderer.line(t.a, t.b);
			shapeRenderer.line(t.b, t.c);
			shapeRenderer.line(t.c, t.a);
		}
		NavMeshGraphPath path = navmesh.debugPath;
		if (path != null && path.getCount() > 0) {
			// Path triangles
			shapeRenderer.set(MyShapeRenderer.ShapeType.Filled);
			shapeRenderer.setColor(1, 1, 0, 0.2f);
			for (int i = 0; i < path.getCount(); i++) {
				Edge e = (Edge) path.get(i);
				shapeRenderer.triangle(e.fromNode.a, e.fromNode.b, e.fromNode.c);
				if (i == path.getCount() - 1) {
					shapeRenderer.triangle(e.toNode.a, e.toNode.b, e.toNode.c);
				}
			}
			// Shared triangle edges
			shapeRenderer.set(MyShapeRenderer.ShapeType.Line);
			for (Connection<Triangle> connection : path) {
				Edge e = (Edge) connection;
				shapeRenderer.line(e.rightVertex, e.leftVertex, Color.GREEN, Color.RED);
			}
		}
		// Smoothed path
		Array<Vector3> smoothPath = navmesh.debugPathSmooth;
		if (smoothPath != null && smoothPath.size > 1) {
			shapeRenderer.set(MyShapeRenderer.ShapeType.Line);
			shapeRenderer.setColor(Color.CYAN);
			for (int i = 0; i < smoothPath.size - 1; i++) {
				Vector3 p = smoothPath.get(i);
				Vector3 q = smoothPath.get(i + 1);
				shapeRenderer.line(p, q);
			}
		}
		shapeRenderer.end();
		Gdx.gl.glDisable(GL20.GL_BLEND);

		spriteBatch.begin();
		spriteBatch.setProjectionMatrix(camera.combined);
		for (int i = 0; i < navmesh.graph.getNodeCount(); i++) {
			Triangle t = navmesh.graph.getTriangleFromIndex(i);
			tmpMatrix.set(camera.view).inv().getRotation(tmpQuat);
			tmpMatrix.setToTranslation(t.centroid).rotate(tmpQuat);
			spriteBatch.setTransformMatrix(tmpMatrix);
			font.draw(spriteBatch, Integer.toString(t.index), 0, 0);
		}
		spriteBatch.end();
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
		shapeRenderer.begin(MyShapeRenderer.ShapeType.Line);
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
