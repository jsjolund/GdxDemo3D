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


import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Bits;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.objects.GameCharacter;
import com.mygdx.game.objects.HumanCharacter;
import com.mygdx.game.settings.DebugViewSettings;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.settings.ShaderSettings;
import com.mygdx.game.ui.IntValueLabel;
import com.mygdx.game.ui.ValueLabel;
import com.mygdx.game.utilities.CameraController;
import com.mygdx.game.utilities.Observable;
import com.mygdx.game.utilities.Observer;

import java.lang.reflect.Field;

/**
 * @author jsjolund
 */
public class GameStage extends Stage implements Observable {

	private class LayerController extends Table {
		public final Slider slider;
		int minLayer = 1;
		int maxLayer = 4;

		public LayerController(TextureAtlas buttonAtlas) {
			Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
			sliderStyle.knob = new TextureRegionDrawable(buttonAtlas.findRegion("slider-knob-up"));
			sliderStyle.knobDown = new TextureRegionDrawable(buttonAtlas.findRegion("slider-knob-down"));
			sliderStyle.knobOver = new TextureRegionDrawable(buttonAtlas.findRegion("slider-knob-down"));
			sliderStyle.background = new TextureRegionDrawable(buttonAtlas.findRegion("slider-background"));

			slider = new Slider(minLayer, maxLayer, 1, true, sliderStyle);
			slider.setValue(maxLayer);
			slider.setAnimateDuration(0.1f);

			slider.addCaptureListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					setLayer((int) slider.getValue());
				}
			});
			add(slider).height(300);
		}

		public void setLayer(int layer) {
			layer = MathUtils.clamp(layer, minLayer, maxLayer);
			slider.setValue(layer);
			visibleLayers.clear();
			for (int i = 0; i < layer; i++) {
				visibleLayers.set(i);
			}
			notifyObserversLayerChanged(visibleLayers);
		}
	}

	public class WorldInputProcessor implements InputProcessor {

		private final Ray dragCurrentRay = new Ray();
		private final Ray lastDragProcessedRay = new Ray();
		private final Ray touchDownRay = new Ray();
		private final Ray touchUpRay = new Ray();
		private final Ray movedRay = new Ray();

		private final Vector2 lastDragProcessed = new Vector2();
		private final Vector2 cursorDelta = new Vector2();
		private final Vector2 dragCurrent = new Vector2();
		private final Vector2 keyPanDirection = new Vector2();
		private final Vector2 lastTouchDown = new Vector2();

		private final Vector3 tmp = new Vector3();
		private final Vector3 mouseCoords = new Vector3();

		private final IntIntMap keys = new IntIntMap();

		private boolean isDragging = false;

		@Override
		public boolean keyDown(int keycode) {
			keys.put(keycode, keycode);
			if (keycode == GameSettings.KEY_PAUSE) {
				speedController.setGameSpeed();
			}
			return true;
		}

		@Override
		public boolean keyUp(int keycode) {
			keys.remove(keycode, 0);
			return true;
		}

		@Override
		public boolean keyTyped(char keyChar) {
			int keyInt = Character.getNumericValue(keyChar);
			keyInt--;
			if (keyInt >= 0 && keyInt <= 9) {
				layerController.setLayer(keyInt + 1);
			}
			return true;
		}

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			lastTouchDown.set(screenX, screenY);
			lastDragProcessed.set(screenX, screenY);
			if (button == Input.Buttons.LEFT) {
				touchDownRay.set(viewport.getPickRay(screenX, screenY));
				cameraController.processTouchDownLeft(touchDownRay);

			} else if (button == Input.Buttons.RIGHT) {
				cameraController.processTouchDownRight();
			}
			return true;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			if (!isDragging) {
				touchUpRay.set(viewport.getPickRay(screenX, screenY));

				Entity hitEntity = engine.rayTest(
						touchUpRay, tmp,
						GameEngine.ALL_FLAG,
						GameEngine.ALL_FLAG,
						GameSettings.CAMERA_PICK_RAY_DST, visibleLayers);

				Gdx.app.debug(tag, "Hit entity " + hitEntity);

				if (hitEntity instanceof GameCharacter) {
					characterController.handleCharacterSelection((GameCharacter) hitEntity);

				} else if (hitEntity == engine.navmeshEntity) {
					characterController.handleCharacterPathing(touchUpRay);
				}
			}
			isDragging = false;
			dragCurrent.setZero();
			lastDragProcessed.setZero();
			return true;
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			dragCurrent.set(screenX, screenY);
			if (dragCurrent.dst2(lastTouchDown) > GameSettings.MOUSE_DRAG_THRESHOLD) {
				isDragging = true;
				if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
					dragCurrentRay.set(viewport.getPickRay(dragCurrent.x, dragCurrent.y));
					lastDragProcessedRay.set(viewport.getPickRay(lastDragProcessed.x, lastDragProcessed.y));
					cameraController.processDragPan(dragCurrentRay, lastDragProcessedRay);

				} else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
					cursorDelta.set(lastDragProcessed).sub(screenX, screenY).scl(GameSettings.MOUSE_SENSITIVITY);
					cameraController.processDragRotation(cursorDelta);
				}
				lastDragProcessed.set(screenX, screenY);
			}
			return true;
		}

		@Override
		public boolean mouseMoved(int screenX, int screenY) {
			movedRay.set(viewport.getPickRay(screenX, screenY));
			Entity e = engine.rayTest(movedRay, mouseCoords, GameEngine.NAVMESH_FLAG,
					GameEngine.NAVMESH_FLAG, GameSettings.CAMERA_PICK_RAY_DST, null);
			if (e == null) mouseCoords.set(Float.NaN, Float.NaN, Float.NaN);
			return true;
		}

		@Override
		public boolean scrolled(int amount) {
			cameraController.processZoom(amount);
			return true;
		}

		public void update(float deltaTime) {
			keyPanDirection.setZero();
			if (keys.containsKey(GameSettings.KEY_PAN_FORWARD)) {
				keyPanDirection.y++;
			}
			if (keys.containsKey(GameSettings.KEY_PAN_BACKWARD)) {
				keyPanDirection.y--;
			}
			if (keys.containsKey(GameSettings.KEY_PAN_LEFT)) {
				keyPanDirection.x--;
			}
			if (keys.containsKey(GameSettings.KEY_PAN_RIGHT)) {
				keyPanDirection.x++;
			}
			if (!keyPanDirection.isZero()) {
				keyPanDirection.nor();
				cameraController.processKeyboardPan(keyPanDirection, deltaTime);
			}
		}
	}

	private class GameSpeedController extends Table {

		private final ImageButton imageButton;
		private final ImageButton.ImageButtonStyle btnPauseStyle;
		private final ImageButton.ImageButtonStyle btnPlayStyle;
		private final ImageButton.ImageButtonStyle btnSlowStyle;

		public GameSpeedController(TextureAtlas buttonAtlas) {
			btnPauseStyle = new ImageButton.ImageButtonStyle();
			btnPauseStyle.up = new TextureRegionDrawable(buttonAtlas.findRegion("pause-up"));
			btnPauseStyle.down = new TextureRegionDrawable(buttonAtlas.findRegion("pause-down"));

			btnPlayStyle = new ImageButton.ImageButtonStyle();
			btnPlayStyle.up = new TextureRegionDrawable(buttonAtlas.findRegion("play-up"));
			btnPlayStyle.down = new TextureRegionDrawable(buttonAtlas.findRegion("play-down"));

			btnSlowStyle = new ImageButton.ImageButtonStyle();
			btnSlowStyle.up = new TextureRegionDrawable(buttonAtlas.findRegion("slow-up"));
			btnSlowStyle.down = new TextureRegionDrawable(buttonAtlas.findRegion("slow-down"));

			imageButton = new ImageButton(btnPauseStyle);

			add(imageButton);

			imageButton.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					setGameSpeed();
					event.cancel();
				}

			});
		}

		public void setGameSpeed() {
			if (GameSettings.GAME_SPEED == GameSettings.GAME_SPEED_PLAY) {
				GameSettings.GAME_SPEED = GameSettings.GAME_SPEED_PAUSE;
				imageButton.setStyle(btnSlowStyle);

			} else if (GameSettings.GAME_SPEED == GameSettings.GAME_SPEED_PAUSE) {
				GameSettings.GAME_SPEED = GameSettings.GAME_SPEED_SLOW;
				imageButton.setStyle(btnPlayStyle);

			} else if (GameSettings.GAME_SPEED == GameSettings.GAME_SPEED_SLOW) {
				GameSettings.GAME_SPEED = GameSettings.GAME_SPEED_PLAY;
				imageButton.setStyle(btnPauseStyle);
			}
		}
	}


	private class CharacterController extends Table {
		private class CharacterButton extends ImageButton {

			HumanCharacter.CharacterState state;

			CharacterButton(HumanCharacter.CharacterState state, TextureAtlas buttonAtlas, String up, String down, String checked) {
				super(new TextureRegionDrawable(buttonAtlas.findRegion(up)),
						new TextureRegionDrawable(buttonAtlas.findRegion(down)),
						new TextureRegionDrawable(buttonAtlas.findRegion(checked)));
				this.state = state;
				this.addListener(new ClickListener() {
					@Override
					public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
						if (selectedCharacter instanceof HumanCharacter) {
							HumanCharacter human = (HumanCharacter) selectedCharacter;
							human.handleStateCommand(CharacterButton.this.state);
						}
						return true;
					}
				});
			}
		}
		private ButtonGroup<CharacterButton> radioGroup;
		private GameCharacter selectedCharacter;

		public CharacterController(TextureAtlas buttonAtlas) {
			radioGroup = new ButtonGroup<CharacterButton>(
					new CharacterButton(HumanCharacter.CharacterState.MOVE_RUN, buttonAtlas, "run-up", "run-down", "run-down"),
					new CharacterButton(HumanCharacter.CharacterState.MOVE_WALK, buttonAtlas, "walk-up", "walk-down", "walk-down"),
					new CharacterButton(HumanCharacter.CharacterState.MOVE_CROUCH, buttonAtlas, "crouch-up", "crouch-down", "crouch-down"),
					//new CharacterButton(HumanCharacter.CharacterState.MOVE_CRAWL, buttonAtlas, "crawl-up", "crawl-down", "crawl-down"),
					new CharacterButton(HumanCharacter.CharacterState.DEAD, buttonAtlas, "kill-up", "kill-down", "kill-down")
			);

			for (CharacterButton btn : radioGroup.getButtons()) {
				add(btn);
			}
		}

		public void handleCharacterSelection(GameCharacter character) {
			if (character instanceof HumanCharacter) {
				HumanCharacter human = (HumanCharacter) character;
				for (CharacterButton btn : radioGroup.getButtons()) {
					if (btn.state == human.getCurrentMoveState())
						btn.setChecked(true);
				}
			}
			selectedCharacter = character;
			notifyObserversEntitySelected(selectedCharacter);
		}

		public void handleCharacterPathing(Ray ray) {
			if (selectedCharacter == null) {
				return;
			}
			if (engine.navmesh.getPath(selectedCharacter.currentTriangle,
					selectedCharacter.getGroundPosition(),
					ray, visibleLayers,
					GameSettings.CAMERA_PICK_RAY_DST,
					selectedCharacter.navMeshGraphPath)) {

				selectedCharacter.calculateNewPath();
			}
		}
	}


	public static final String tag = "GameStage";
	private final Viewport viewport;
	private final SpriteBatch batch;
	private final ShapeRenderer shapeRenderer;
	private final Table rootTable;
	private final TextureAtlas buttonsAtlas;
	private final ValueLabel<Vector3> mouseCoordsLabel;
	private final Camera cameraUI;
	private final Camera camera3D;
	private final Skin skin;
	private final CameraController cameraController;
	private final GameEngine engine;
	private final Array<Observer> observers = new Array<Observer>();
	private final WorldInputProcessor worldInputProcessor;
	private final Label fpsLabel;
	private final GameSpeedController speedController;
	private final CharacterController characterController;
	private final LayerController layerController;
	private final Vector3 tmp = new Vector3();
	private Bits visibleLayers;

	public GameStage(GameEngine engine, Viewport viewport, CameraController cameraController) {
		super(viewport);
		this.engine = engine;
		this.viewport = viewport;
		this.camera3D = viewport.getCamera();
		this.cameraController = cameraController;

		visibleLayers = new Bits();

		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(this);
		multiplexer.addProcessor(worldInputProcessor = new WorldInputProcessor());
		Gdx.input.setInputProcessor(multiplexer);

		cameraUI = new OrthographicCamera(viewport.getScreenWidth(), viewport.getScreenHeight());
		cameraUI.position.set(viewport.getScreenWidth() / 2, viewport.getScreenHeight() / 2, 0);
		cameraUI.update();

		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		shapeRenderer.setAutoShapeType(true);
		skin = new Skin(Gdx.files.internal("skins/uiskin.json"));

		buttonsAtlas = new TextureAtlas(Gdx.files.internal("skins/buttons.atlas"));

		mouseCoordsLabel = new ValueLabel<Vector3>("NavMesh: ", new Vector3(), skin) {
			@Override
			public Vector3 getValue() {
				return worldInputProcessor.mouseCoords;
			}

			@Override
			public void copyValue(Vector3 newValue, Vector3 oldValue) {
				oldValue.set(newValue);
			}
		};
		fpsLabel = new IntValueLabel("FPS: ", 999, skin) {
			@Override
			public int getValue() {
				return Gdx.graphics.getFramesPerSecond();
			}
		};
		speedController = new GameSpeedController(buttonsAtlas);
		characterController = new CharacterController(buttonsAtlas);
		layerController = new LayerController(buttonsAtlas);
		layerController.setLayer(Integer.MAX_VALUE);

		rootTable = new Table();
		addActor(rootTable);
		rootTable.setDebug(true, true);

		Table topTable = new Table();
		topTable.add(fpsLabel).width(fpsLabel.getWidth()).top().left();
		topTable.add(mouseCoordsLabel).padLeft(15).top().left();
		topTable.add(new Table()).expandX().fillX();
		rootTable.add(topTable).expandX().fillX();

		rootTable.row();
		rootTable.add(new Table()).expandY().fillY();
		rootTable.row();

		Table bottomRightTable = new Table();
		bottomRightTable.add(createShaderMenu()).bottom();
		bottomRightTable.add(createDebugViewMenu()).bottom();
		bottomRightTable.add(createSteeringMenu()).bottom();
		bottomRightTable.add(new Table()).expandX().fillX().bottom();
		rootTable.add(bottomRightTable).expandX().fillX();

		Table bottomLeftTable = new Table();
		bottomLeftTable.add(new Table());
		bottomLeftTable.add(layerController).bottom().right();
		bottomLeftTable.row();
		bottomLeftTable.add(characterController).bottom();
		bottomLeftTable.add(speedController).bottom();
		bottomLeftTable.setTransform(true);
		bottomLeftTable.setOrigin(bottomLeftTable.getPrefWidth(), 0);
		bottomLeftTable.setScale(0.5f);
		rootTable.add(bottomLeftTable).bottom();

		rootTable.row();
		rootTable.left().bottom();

		getRoot().addCaptureListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (!(event.getTarget() instanceof TextField)) setKeyboardFocus(null);
				return false;
			}
		});
	}

	public Bits getVisibleLayers(Bits out) {
		out.clear();
		out.or(visibleLayers);
		return out;
	}

	@Override
	public void dispose() {
		super.dispose();
		batch.dispose();
		skin.dispose();
		shapeRenderer.dispose();
		buttonsAtlas.dispose();
	}

	public void resize(int width, int height) {
		getViewport().update(width, height, false);
		cameraUI.viewportWidth = viewport.getScreenWidth();
		cameraUI.viewportHeight = viewport.getScreenHeight();
		cameraUI.position.set(viewport.getScreenWidth() / 2, viewport.getScreenHeight() / 2, 0);
		cameraUI.update();
		batch.setProjectionMatrix(cameraUI.combined);
		shapeRenderer.setProjectionMatrix(cameraUI.combined);

		rootTable.setSize(viewport.getScreenWidth(), viewport.getScreenHeight());
	}

	private WidgetGroup createShaderMenu() {
		final Table innerTable = new Table();
		final Table outerTable = new Table();

		Field[] fields = ShaderSettings.class.getFields();
		for (final Field field : fields) {
			final Label fieldName = new Label(field.getName(), skin);
			float fieldValueFloat = 0;
			try {
				fieldValueFloat = field.getFloat(field);
			} catch (Exception e) {
				Gdx.app.debug(tag, "Cannot parse value for " + field.getName());
			}
			final TextField fieldValue = new TextField(String.valueOf(fieldValueFloat), skin);
			innerTable.add(fieldName).fillX();
			innerTable.row();
			innerTable.add(fieldValue).fillX();
			innerTable.row();
			fieldValue.addListener(new InputListener() {
				@Override
				public boolean keyTyped(InputEvent event, char character) {
					String userInput = fieldValue.getText();
					float newFieldValue;
					try {
						newFieldValue = Float.parseFloat(userInput);
					} catch (NumberFormatException e) {
						return true;
					}
					try {
						field.setFloat(field, newFieldValue);
					} catch (IllegalAccessException e) {
						Gdx.app.debug(tag, "Cannot set value for " + field.getName());
					}
					return true;
				}
			});
		}
		innerTable.setVisible(false);
		final TextButton button = new TextButton("shader", skin);
		button.addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				innerTable.setVisible(!innerTable.isVisible());
				return true;
			}
		});
		outerTable.add(innerTable).fillX();
		outerTable.row();
		outerTable.add(button).fillX();
		outerTable.row();
		return outerTable;
	}

	private WidgetGroup createSteeringMenu() {
		final Table innerTable = new Table();
		final Table outerTable = new Table();

		Field[] fields = HumanCharacter.HumanSteerSettings.class.getFields();
		for (final Field field : fields) {
			final Label fieldName = new Label(field.getName(), skin);
			float fieldValueFloat = 0;
			try {
				fieldValueFloat = field.getFloat(field);
			} catch (Exception e) {
				Gdx.app.debug(tag, "Cannot parse value for " + field.getName());
			}
			final TextField fieldValue = new TextField(String.valueOf(fieldValueFloat), skin);
			innerTable.add(fieldName).fillX();
			innerTable.row();
			innerTable.add(fieldValue).fillX();
			innerTable.row();
			fieldValue.addListener(new InputListener() {
				@Override
				public boolean keyTyped(InputEvent event, char character) {
					String userInput = fieldValue.getText();
					float newFieldValue;
					try {
						newFieldValue = Float.parseFloat(userInput);
					} catch (NumberFormatException e) {
						return true;
					}
					try {
						field.setFloat(field, newFieldValue);
					} catch (IllegalAccessException e) {
						Gdx.app.debug(tag, "Cannot set value for " + field.getName());
					}
					return true;
				}
			});
		}
		innerTable.setVisible(false);
		final TextButton button = new TextButton("steer", skin);
		button.addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				innerTable.setVisible(!innerTable.isVisible());
				return true;
			}
		});
		outerTable.add(innerTable).fillX();
		outerTable.row();
		outerTable.add(button).fillX();
		outerTable.row();
		return outerTable;
	}

	private WidgetGroup createDebugViewMenu() {
		final Table innerTable = new Table();
		final Table outerTable = new Table();

		Field[] fields = DebugViewSettings.class.getFields();
		for (final Field field : fields) {
			boolean fieldValueBoolean = false;
			try {
				fieldValueBoolean = field.getBoolean(field);
			} catch (Exception e) {
				Gdx.app.debug(tag, "Cannot parse value for " + field.getName());
			}
			final CheckBox checkBox = new CheckBox(field.getName(), skin);
			checkBox.setChecked(fieldValueBoolean);
			innerTable.add(checkBox).pad(1).align(Align.left);
			innerTable.row();
			checkBox.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					try {
						field.setBoolean(field, checkBox.isChecked());
					} catch (IllegalAccessException e) {
						Gdx.app.debug(tag, "Cannot set value for " + field.getName());
					}
				}
			});
		}
		innerTable.setVisible(false);
		final TextButton button = new TextButton("debug", skin);
		button.addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				innerTable.setVisible(!innerTable.isVisible());
				return true;
			}
		});
		outerTable.add(innerTable).fillX();
		outerTable.row();
		outerTable.add(button).fillX();
		outerTable.row();
		return outerTable;
	}

	@Override
	public Vector2 screenToStageCoordinates(Vector2 screenCoords) {
		tmp.set(screenCoords.x, screenCoords.y, 1);
		cameraUI.unproject(tmp, viewport.getScreenX(), viewport.getScreenY(),
				viewport.getScreenWidth(), viewport.getScreenHeight());
		screenCoords.set(tmp.x, tmp.y);
		return screenCoords;
	}

	@Override
	public void act(float delta) {
		super.act(delta);
		worldInputProcessor.update(delta);
	}

	@Override
	public void draw() {
		batch.begin();
		for (Actor actor : getActors()) {
			if (actor.isVisible()) {
				actor.draw(batch, 1);
			}
		}
		batch.end();

		if (DebugViewSettings.drawUIDebug) {
			shapeRenderer.begin();
			rootTable.drawDebug(shapeRenderer);
			shapeRenderer.end();
		}
	}

	@Override
	public void addObserver(Observer observer) {
		observers.add(observer);
	}

	@Override
	public void removeObserver(Observer observer) {
		observers.removeValue(observer, true);
	}

	@Override
	public void notifyObserversEntitySelected(GameCharacter entity) {
		for (Observer observer : observers) {
			observer.notifyEntitySelected(entity);
		}
	}

	@Override
	public void notifyObserversLayerChanged(Bits layer) {
		for (Observer observer : observers) {
			observer.notifyLayerChanged(layer);
		}
	}
}
