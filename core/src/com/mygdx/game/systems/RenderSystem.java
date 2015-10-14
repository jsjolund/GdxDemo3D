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
import com.badlogic.gdx.graphics.g3d.*;
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
import com.mygdx.game.components.PathFindingComponent;
import com.mygdx.game.components.SelectableComponent;
import com.mygdx.game.navmesh.Edge;
import com.mygdx.game.navmesh.NavMesh;
import com.mygdx.game.navmesh.Triangle;
import com.mygdx.game.settings.DebugViewSettings;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.shaders.UberShader;
import com.mygdx.game.utilities.MyShapeRenderer;
import com.mygdx.game.utilities.Observer;


/**
 * Created by user on 7/31/15.
 */
public class RenderSystem extends EntitySystem implements Disposable, Observer {

	public static final String tag = "RenderSystem";

	public final Family systemFamily;
	private final ComponentMapper<ModelComponent> models = ComponentMapper.getFor(ModelComponent.class);
	private final ModelBatch modelBatch;
	private final ModelBatch shadowBatch;
	private final MyShapeRenderer shapeRenderer;
	private final SpriteBatch spriteBatch;
	private final Vector3 tmp = new Vector3();
	private final Vector3 debugNodePos1 = new Vector3();
	private final Vector3 debugNodePos2 = new Vector3();
	private final Matrix4 tmpMatrix = new Matrix4();
	private final Quaternion tmpQuat = new Quaternion();
	private final Viewport viewport;
	private ImmutableArray<Entity> entities;
	private BitmapFont font;
	private Camera camera;
	private Environment environment;
	private DirectionalShadowLight shadowLight;
	private NavMesh navmesh;

	private ModelInstance selectedModelInstance;
	private ModelInstance selectedMarker;
	private PathFindingComponent selectedPath;

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
	public void notifyEntitySelected(Entity entity) {
		SelectableComponent selCmp = entity.getComponent(SelectableComponent.class);
		if (selCmp != null) {
			selectedMarker = selCmp.selectedMarkerModel;
		} else {
			selectedMarker = null;
		}
		selectedPath = entity.getComponent(PathFindingComponent.class);
		ModelComponent mdlCmp = entity.getComponent(ModelComponent.class);
		if (mdlCmp != null) {
			selectedModelInstance = mdlCmp.modelInstance;
		} else {
			selectedModelInstance = null;
		}
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
				modelBatch.render(mdlCmp.modelInstance, environment);
			}
		}
		if (selectedMarker != null) {
			modelBatch.render(selectedMarker, environment);
		}
		modelBatch.end();

		if (DebugViewSettings.drawArmature) {
			drawArmature();
		}

		if (DebugViewSettings.drawNavmesh) {
			drawNavMesh();
		}

		if (DebugViewSettings.drawPath) {
			drawPath();
		}
	}

	private void drawPath() {

		if (selectedPath != null && selectedPath.path.size > 1) {
			shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
			shapeRenderer.begin(MyShapeRenderer.ShapeType.Line);
			shapeRenderer.setColor(Color.CORAL);
			// Smoothed path
			Vector3 q;
			Vector3 p = selectedPath.currentGoal;
			for (int i = selectedPath.path.size - 1; i >= 0; i--) {
				q = selectedPath.path.get(i);
				shapeRenderer.line(p, q);
				p = q;
			}
			shapeRenderer.end();
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

		if (selectedPath != null && selectedPath.trianglePath.getCount() > 0) {
			// Path triangles
			shapeRenderer.set(MyShapeRenderer.ShapeType.Filled);
			shapeRenderer.setColor(1, 1, 0, 0.2f);
			for (int i = 0; i < selectedPath.trianglePath.getCount(); i++) {
				Edge e = (Edge) selectedPath.trianglePath.get(i);
				shapeRenderer.triangle(e.fromNode.a, e.fromNode.b, e.fromNode.c);
				if (i == selectedPath.trianglePath.getCount() - 1) {
					shapeRenderer.triangle(e.toNode.a, e.toNode.b, e.toNode.c);
				}
			}
			// Shared triangle edges
			shapeRenderer.set(MyShapeRenderer.ShapeType.Line);
			for (Connection<Triangle> connection : selectedPath.trianglePath) {
				Edge e = (Edge) connection;
				shapeRenderer.line(e.rightVertex, e.leftVertex, Color.GREEN, Color.RED);
			}
		}
		shapeRenderer.end();
		// Draw indices of the triangles
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
		shapeRenderer.begin(MyShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(0, 1, 1, 1);
		if (selectedModelInstance != null) {
			Node skeleton = selectedModelInstance.getNode("armature");
			if (skeleton != null) {
				selectedModelInstance.transform.getTranslation(tmp);
				skeleton.globalTransform.getTranslation(debugNodePos1);
				drawArmatureNodes(skeleton, tmp, debugNodePos1, debugNodePos2);
			}
		}
		shapeRenderer.end();
	}

	private void drawArmatureNodes(Node currentNode, Vector3 modelPos,
								   Vector3 parentNodePos, Vector3 currentNodePos) {
		currentNode.globalTransform.getTranslation(currentNodePos);
		currentNodePos.add(modelPos);
		shapeRenderer.setColor(Color.GREEN);
		shapeRenderer.box(currentNodePos.x, currentNodePos.y, currentNodePos.z, 0.01f, 0.01f, 0.01f);
		shapeRenderer.setColor(Color.YELLOW);
		if (currentNode.hasParent()) {
			shapeRenderer.line(parentNodePos, currentNodePos);
		}
		if (currentNode.hasChildren()) {
			float x = currentNodePos.x;
			float y = currentNodePos.y;
			float z = currentNodePos.z;
			for (Node child : currentNode.getChildren()) {
				drawArmatureNodes(child, modelPos, currentNodePos, parentNodePos);
				currentNodePos.set(x, y, z);
			}
		}
	}

	public void setNavmesh(NavMesh navmesh) {
		this.navmesh = navmesh;
	}


}
