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
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Bits;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.objects.GameCharacter;
import com.mygdx.game.objects.HumanCharacter;
import com.mygdx.game.objects.HumanCharacter.HumanState;
import com.mygdx.game.settings.DebugViewSettings;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.settings.ShaderSettings;
import com.mygdx.game.ui.BooleanSettingsMenu;
import com.mygdx.game.ui.FloatSettingsMenu;
import com.mygdx.game.ui.FloatValueLabel;
import com.mygdx.game.ui.GameSpeedController;
import com.mygdx.game.ui.IntValueLabel;
import com.mygdx.game.utilities.BehaviorTreeController;
import com.mygdx.game.utilities.CameraController;
import com.mygdx.game.utilities.Constants;
import com.mygdx.game.utilities.Entity;
import com.mygdx.game.utilities.Observable;
import com.mygdx.game.utilities.Observer;

/**
 * @author jsjolund
 */
public class GameStage extends Stage implements Observable {

	private class LayerController extends Table {
		private final Slider slider;
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
		private final Vector3 mouseNavMeshPos = new Vector3();
		private final Vector2 mouseScreenPos = new Vector2();


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
			if (keyChar >= '1' && keyChar <= '9') {
				layerController.setLayer(keyChar - '0');
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

				Gdx.app.debug(TAG, "Hit entity " + hitEntity);

				if (hitEntity instanceof GameCharacter) {
					characterController.handleCharacterSelection((GameCharacter) hitEntity);

				} else if (hitEntity == engine.getScene().navmeshBody) {
					characterController.handleMovementRequest(touchUpRay, visibleLayers);
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
			mouseMoved(screenX, screenY);
			return true;
		}

		@Override
		public boolean mouseMoved(int screenX, int screenY) {
			movedRay.set(viewport.getPickRay(screenX, screenY));
			Entity e = engine.rayTest(movedRay, mouseNavMeshPos, GameEngine.NAVMESH_FLAG,
					GameEngine.NAVMESH_FLAG, GameSettings.CAMERA_PICK_RAY_DST, null);
			if (e == null) mouseNavMeshPos.set(Float.NaN, Float.NaN, Float.NaN);
			notifyCursorWorldPosition(mouseNavMeshPos.x, mouseNavMeshPos.y, mouseNavMeshPos.z);
			mouseScreenPos.set(screenX, screenY);
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


	private class CharacterController extends Table implements Telegraph {
		private class CharacterButton extends ImageButton {

			HumanState state;

			CharacterButton(HumanState state, TextureAtlas buttonAtlas, String up, String down, String checked) {
				super(new TextureRegionDrawable(buttonAtlas.findRegion(up)),
						new TextureRegionDrawable(buttonAtlas.findRegion(down)),
						new TextureRegionDrawable(buttonAtlas.findRegion(checked)));
				this.state = state;
				this.addListener(new ClickListener() {
					@Override
					public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
						if (!CharacterButton.this.isChecked()) {
							if (selectedCharacter instanceof HumanCharacter) {
								HumanCharacter human = (HumanCharacter) selectedCharacter;
								if (CharacterButton.this.state.isMovementState()) {
									HumanState hs = human.stateMachine.getCurrentState();
									if (hs.isMovementState()) {
										human.handleStateCommand(CharacterButton.this.state);
									} else {
										human.moveState = CharacterButton.this.state;
										human.handleStateCommand(CharacterButton.this.state.idleState);
									}
								} else {
									human.handleStateCommand(CharacterButton.this.state);
								}
							}
						}
						return true;
					}
				});
			}
		}

		private ButtonGroup<CharacterButton> radioGroup;
		private CharacterButton whistleButton;
		private CharacterButton throwButton;
		private Cell<CharacterButton> dogCell;
		private GameCharacter selectedCharacter;

		public CharacterController(TextureAtlas buttonAtlas) {
			whistleButton = new CharacterButton(HumanState.WHISTLE, buttonAtlas, "whistle-up", "whistle-down", "whistle-down");
			throwButton = new CharacterButton(HumanState.THROW, buttonAtlas, "throw-up", "throw-down", "throw-down");

			radioGroup = new ButtonGroup<CharacterButton>(
					new CharacterButton(HumanState.MOVE_RUN, buttonAtlas, "run-up", "run-down", "run-down"),
					new CharacterButton(HumanState.MOVE_WALK, buttonAtlas, "walk-up", "walk-down", "walk-down"),
					new CharacterButton(HumanState.MOVE_CROUCH, buttonAtlas, "crouch-up", "crouch-down", "crouch-down"),
					//new CharacterButton(CharacterState.MOVE_CRAWL, buttonAtlas, "crawl-up", "crawl-down", "crawl-down"),
					new CharacterButton(HumanState.DEAD, buttonAtlas, "kill-up", "kill-down", "kill-down")
			);

			// Add whistle button and save the reference to the 1st cell
			this.dogCell = add(whistleButton);

			// Add radio buttons
			for (CharacterButton btn : radioGroup.getButtons()) {
				add(btn);
			}

			// Register this controller's interests
			MessageManager.getInstance().addListeners(this,
					Constants.MSG_GUI_CLEAR_DOG_BUTTON,
					Constants.MSG_GUI_SET_DOG_BUTTON_TO_WHISTLE,
					Constants.MSG_GUI_SET_DOG_BUTTON_TO_THROW);
		}

		private final void setDogButton(CharacterButton btn, HumanCharacter human) {
			if (human != null && human == selectedCharacter) {
				whistleButton.setVisible(false);
				throwButton.setVisible(false);
				dogCell.setActor(btn);
				btn.setVisible(true);
				btn.setChecked(false);
				Gdx.app.log("CharacterController", "setDogButton: " + (btn == whistleButton ? "whistleButton" : (btn == throwButton ? "throwButton" : "???")));
			}
		}

		private final void clearDogButton(HumanCharacter human) {
			if (human != null && human == selectedCharacter) {
				whistleButton.setVisible(false);
				throwButton.setVisible(false);
				Gdx.app.log("CharacterController", "clearDogButton");
			}
		}

		public void handleCharacterSelection(GameCharacter character) {
			cameraController.setFollowTarget(character.motionState.transform);
			
			if (selectedCharacter == character)
				return;

			// Remove selection from previously selected human (if any)
			if (selectedCharacter != null && selectedCharacter instanceof HumanCharacter) {
				HumanCharacter human = (HumanCharacter) selectedCharacter;
				human.selected = false;
			}

			// Select new character
			selectedCharacter = character;
			notifyObserversEntitySelected(selectedCharacter);

			if (character instanceof HumanCharacter) {
				steerSettings.clearChildren();
				steerSettings.add(humanSteerSettings);
				steerSettings.invalidateHierarchy();

				HumanCharacter human = (HumanCharacter) character;
				human.selected = true;
				// Restore the controller based on the newly selected human
				updateDogButton(human);
				for (CharacterButton btn : radioGroup.getButtons()) {
					if (btn.state == HumanState.DEAD && human.isDead()) {
						btn.setChecked(true);
						break;
					}
					if (btn.state == human.getCurrentMoveState()) {
						btn.setChecked(true);
						break;
					}
				}
				this.setVisible(true);
			} else if (character instanceof DogCharacter) {
				steerSettings.clearChildren();
				steerSettings.add(dogSteerSettings);
				steerSettings.invalidateHierarchy();
				this.setVisible(false);  // no controller for dogs
				
				btreeController.setCurrentDog((DogCharacter) character);
			}
		}

		public void handleMovementRequest(Ray ray, Bits visibleLayers) {
			// Handle movement request only if a character is selected and a movement button is checked
			if (selectedCharacter != null && radioGroup.getCheckedIndex() > -1 && radioGroup.getChecked().state.isMovementState()) {
				selectedCharacter.handleMovementRequest(ray, visibleLayers);
			}
		}
		
		// Restore the controller for the given human if it is currently selected
		private void updateDogButton(HumanCharacter human) {
			if (selectedCharacter != null && selectedCharacter == human) {
				if (human.dog != null && !human.isDead()) {
					if (!human.dog.humanWantToPlay) {
						this.setDogButton(whistleButton, human);
					} else if (!human.dog.stickThrown) {
						this.setDogButton(throwButton, human);
					} else {
						this.clearDogButton(human);
					}
				} else {
					this.clearDogButton(human);
				}
			}
		}

		@Override
		public boolean handleMessage(Telegram telegram) {
			switch (telegram.message) {
				case Constants.MSG_GUI_SET_DOG_BUTTON_TO_WHISTLE:
					setDogButton(whistleButton, (HumanCharacter) telegram.extraInfo);
					return true;
				case Constants.MSG_GUI_SET_DOG_BUTTON_TO_THROW:
					setDogButton(throwButton, (HumanCharacter) telegram.extraInfo);
					return true;
				case Constants.MSG_GUI_CLEAR_DOG_BUTTON:
					clearDogButton((HumanCharacter) telegram.extraInfo);
					return true;
				case Constants.MSG_GUI_UPDATE_DOG_BUTTON:
					updateDogButton((HumanCharacter) telegram.extraInfo);
					return true;
			}
			return false;
		}
	}

	private class MouseNavMeshCoordTable extends Table {
		public MouseNavMeshCoordTable(Skin skin) {
			align(Align.topLeft); // Fix flickering due to the variable width of rows

			// Create label X
			FloatValueLabel mouseLabelX = new FloatValueLabel("x: ", 0, skin) {
				@Override
				public float getValue() {
					return worldInputProcessor.mouseNavMeshPos.x;
				}
			};
			// Create label Y
			FloatValueLabel mouseLabelY = new FloatValueLabel("y: ", 0, skin) {
				@Override
				public float getValue() {
					return worldInputProcessor.mouseNavMeshPos.y;
				}
			};
			// Create label Z
			FloatValueLabel mouseLabelZ = new FloatValueLabel("z: ", 0, skin) {
				@Override
				public float getValue() {
					return worldInputProcessor.mouseNavMeshPos.z;
				}
			};
			// This should be floating
			add(mouseLabelX).left();
			row();
			add(mouseLabelY).left();
			row();
			add(mouseLabelZ).left();
		}

		@Override
		public void act(float delta) {
			boolean visible = DebugViewSettings.drawMouseNavMeshPos;
			setVisible(visible);
			if (visible) {
				screenToStageCoordinates(tmpV2.set(worldInputProcessor.mouseScreenPos));
				setPosition(tmpV2.x + 10, tmpV2.y - 10);
				super.act(delta);  // No need to update this actor if it's not visible
			}
		}
	}
	private static final String TAG = "GameStage";
	private final Viewport viewport;
	private final SpriteBatch batch;
	private final ShapeRenderer shapeRenderer;
	private final Table rootTable;
	private final TextureAtlas buttonsAtlas;
	private final Camera cameraUI;
	private final Skin skin;
	private final CameraController cameraController;
	private final GameEngine engine;
	private final Array<Observer> observers = new Array<Observer>();
	private final WorldInputProcessor worldInputProcessor;
	private final GameSpeedController speedController;
	private final CharacterController characterController;
	private final LayerController layerController;
	private final FloatSettingsMenu humanSteerSettings;
	private final FloatSettingsMenu dogSteerSettings;
	private final Table steerSettings;
	private final Vector3 tmp = new Vector3();
	private final Vector2 tmpV2 = new Vector2();
	private Bits visibleLayers;

	public final BehaviorTreeController btreeController;
	private final TextButton toggle;

	public GameStage(GameEngine engine, Viewport viewport, CameraController cameraController) {
		super(viewport);
		this.engine = engine;
		this.viewport = viewport;
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

		addActor(new MouseNavMeshCoordTable(skin));

		// Create controllers
		speedController = new GameSpeedController(buttonsAtlas);
		characterController = new CharacterController(buttonsAtlas);
		characterController.setVisible(false); // initially no character is selected, so hide the controller
		layerController = new LayerController(buttonsAtlas);

		// Create a root table that auto-scales on resize
		rootTable = new Table() {
			@Override
			protected void sizeChanged () {
				super.sizeChanged();
				
				float scaleX = 1;
				float scaleY = 1;
				float scaleTolerance = 0.001f;
				if (getPrefWidth() > getWidth())
					scaleX -= (rootTable.getPrefWidth() - getWidth()) / (float) getPrefWidth();
//				if (getPrefHeight() > getHeight())
//					scaleY -= (getPrefHeight() - getHeight()) / (float)getPrefHeight();

				if (MathUtils.isEqual(scaleX, 1, scaleTolerance) && MathUtils.isEqual(scaleY, 1, scaleTolerance)) {
					Gdx.app.log(TAG, "No need to scale rootTable: scaleX = " + scaleX + "  scaleY = " + scaleY);
					setTransform(false);
					setOrigin(0, 0);
					setScale(1);
				} else {
					Gdx.app.log(TAG, "Scaling rootTable: scaleX = " + scaleX + "  scaleY = " + scaleY);
					setTransform(true);
					setOrigin(0, 0);
					setScale(scaleX, scaleY);
				}
			}
		};
		addActor(rootTable);
		rootTable.setDebug(true, true);

		// Add top left row with FPS label
		rootTable.row().top().left().colspan(3);
		rootTable.add(new IntValueLabel("FPS: ", 999, skin) {
			@Override
			public int getValue() {
				return Gdx.graphics.getFramesPerSecond();
			}
		});

		rootTable.row().bottom();
		humanSteerSettings = new FloatSettingsMenu("Steering (human)", skin,
				HumanCharacter.HumanSteerSettings.class);
		dogSteerSettings = new FloatSettingsMenu("Steering (dog)", skin,
				DogCharacter.DogSteerSettings.class);
		steerSettings = new Table();
		steerSettings.add(humanSteerSettings);

		btreeController = new BehaviorTreeController();
		toggle = new TextButton("Show editor", skin);
		toggle.addListener(new ClickListener(){
			@Override public void clicked (InputEvent event, float x, float y) {
				btreeController.toggleEditorWindow(rootTable, toggle);
			}
		});
		btreeController.toggleEditorWindow(rootTable, toggle);

		// Add bottom left table with settings
		Table bottomLeftTable = new Table();
		bottomLeftTable.add(new FloatSettingsMenu("Shader", skin, ShaderSettings.class)).bottom();
		bottomLeftTable.add(new BooleanSettingsMenu("Debug", skin, DebugViewSettings.class)).bottom();
		bottomLeftTable.add(steerSettings).bottom();
		bottomLeftTable.add(toggle).bottom();
		rootTable.add(bottomLeftTable).left();

		// Add space between bottom left and bottom right tables
		rootTable.add(new Actor()).width(10).grow();

		// Add bottom right table with controllers
		//
		// FIXME: This is pretty ugly but we have to scale
		// the table because button's images are too big.
		// Unfortunately, scaling the table does not change
		// its size, so we have to override the preferred size. :(
		// Atlas and texture should be resized instead.
		// Maybe sooner or later someone will do :)
		final float scale = 0.5f;
		Table bottomRightTable = new Table() {
			public float getPrefWidth() {
				return super.getPrefWidth() * scale;
			}

			public float getPrefHeight() {
				return super.getPrefHeight() * scale;
			}
		};
		bottomRightTable.add(layerController).right().colspan(2);
		bottomRightTable.row();
		bottomRightTable.add(characterController);
		bottomRightTable.add(speedController);
		bottomRightTable.setTransform(true);
		bottomRightTable.setOrigin(bottomRightTable.getPrefWidth() * scale, bottomRightTable.getPrefHeight() * scale);
		bottomRightTable.setScale(scale);
		rootTable.add(bottomRightTable).width(bottomRightTable.getPrefWidth()).height(bottomRightTable.getPrefHeight()).bottom().right();

		rootTable.left().bottom();

		getRoot().addCaptureListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (!(event.getTarget() instanceof TextField)) setKeyboardFocus(null);
				return false;
			}
		});
	}

	// Code modified from super.calculateScissors()
	@Override
	public void calculateScissors (Rectangle localRect, Rectangle scissorRect) {
		ScissorStack.calculateScissors(cameraUI, viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight(), batch.getTransformMatrix(), localRect, scissorRect);
		Matrix4 transformMatrix;
		if (shapeRenderer != null && shapeRenderer.isDrawing())
			transformMatrix = shapeRenderer.getTransformMatrix();
		else
			transformMatrix = batch.getTransformMatrix();
		ScissorStack.calculateScissors(cameraUI, viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight(), transformMatrix, localRect, scissorRect);
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

		// Resize the root table that will auto-scale if needed
		rootTable.setSize(viewport.getScreenWidth(), viewport.getScreenHeight());
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
		cameraController.update();
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

	@Override
	public void notifyCursorWorldPosition(float x, float y, float z) {
		for (Observer observer : observers) {
			observer.notifyCursorWorldPosition(x, y, z);
		}
	}

	public void setVisibleLayers(Bits visibleLayers) {
		this.visibleLayers.clear();
		this.visibleLayers.or(visibleLayers);
		layerController.setLayer(visibleLayers.nextClearBit(0) - 1);
		notifyObserversLayerChanged(this.visibleLayers);
	}
}
