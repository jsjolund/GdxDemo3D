package com.mygdx.game;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.components.CharacterActionComponent;
import com.mygdx.game.settings.DebugViewSettings;
import com.mygdx.game.settings.ShaderSettings;

import java.lang.reflect.Field;

/**
 * Created by user on 8/24/15.
 */
public class GameStage extends Stage {

	public static final String tag = "GameStage";

	private final Viewport viewport;
	private final Batch batch;
	private final ShapeRenderer shapeRenderer;
	private final Table rootTable;
	private final TextureAtlas movementButtonsAtlas;

	Camera cameraUI;
	Camera camera3D;
	Skin skin;

	public GameStage(Viewport viewport) {
		super(viewport);

		this.viewport = viewport;

		camera3D = viewport.getCamera();
		cameraUI = new OrthographicCamera(viewport.getScreenWidth(), viewport.getScreenHeight());
		cameraUI.position.set(viewport.getScreenWidth() / 2, viewport.getScreenHeight() / 2, 0);
		cameraUI.update();

		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		shapeRenderer.setAutoShapeType(true);
		skin = new Skin(Gdx.files.internal("skins/uiskin.json"));
		movementButtonsAtlas = new TextureAtlas(Gdx.files.internal("skins/movement_buttons.atlas"));

		rootTable = new Table();
		rootTable.setDebug(true, true);

		rootTable.add(createShaderMenu()).bottom();
		rootTable.add(createDebugViewMenu()).bottom();
		rootTable.add(new Table()).expandX().fillX();
		rootTable.add(createMovementButtons()).bottom();
		rootTable.left().bottom();

		addActor(rootTable);
	}

	@Override
	public void dispose() {
		super.dispose();
		batch.dispose();
		skin.dispose();
		shapeRenderer.dispose();
		movementButtonsAtlas.dispose();
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

	private WidgetGroup createMovementButtons() {
		final Table table = new Table();
		final ArrayMap<ImageButton, CharacterActionComponent.Action> btns = new ArrayMap<ImageButton, CharacterActionComponent.Action>();

		ImageButton.ImageButtonStyle btnRunStyle = new ImageButton.ImageButtonStyle();
		btnRunStyle.up = new TextureRegionDrawable(movementButtonsAtlas.findRegion("run-up"));
		btnRunStyle.down = new TextureRegionDrawable(movementButtonsAtlas.findRegion("run-down"));
		btnRunStyle.checked = new TextureRegionDrawable(movementButtonsAtlas.findRegion("run-down"));
		final ImageButton btnRun = new ImageButton(btnRunStyle);

		ImageButton.ImageButtonStyle btnWalkStyle = new ImageButton.ImageButtonStyle();
		btnWalkStyle.up = new TextureRegionDrawable(movementButtonsAtlas.findRegion("walk-up"));
		btnWalkStyle.down = new TextureRegionDrawable(movementButtonsAtlas.findRegion("walk-down"));
		btnWalkStyle.checked = new TextureRegionDrawable(movementButtonsAtlas.findRegion("walk-down"));
		final ImageButton btnWalk = new ImageButton(btnWalkStyle);

		ImageButton.ImageButtonStyle btnCrouchStyle = new ImageButton.ImageButtonStyle();
		btnCrouchStyle.up = new TextureRegionDrawable(movementButtonsAtlas.findRegion("crouch-up"));
		btnCrouchStyle.down = new TextureRegionDrawable(movementButtonsAtlas.findRegion("crouch-down"));
		btnCrouchStyle.checked = new TextureRegionDrawable(movementButtonsAtlas.findRegion("crouch-down"));
		final ImageButton btnCrouch = new ImageButton(btnCrouchStyle);

		ImageButton.ImageButtonStyle btnCrawlStyle = new ImageButton.ImageButtonStyle();
		btnCrawlStyle.up = new TextureRegionDrawable(movementButtonsAtlas.findRegion("crawl-up"));
		btnCrawlStyle.down = new TextureRegionDrawable(movementButtonsAtlas.findRegion("crawl-down"));
		btnCrawlStyle.checked = new TextureRegionDrawable(movementButtonsAtlas.findRegion("crawl-down"));
		final ImageButton btnCrawl = new ImageButton(btnCrawlStyle);

		btns.put(btnRun, null);
		btns.put(btnWalk, null);
		btns.put(btnCrouch, null);
		btns.put(btnCrawl, null);

		for (ImageButton btn : btns.keys()) {
			table.add(btn).size(75, 75);
		}

		btnRun.addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				handleMoveButtonPress(btnRun, btns);
				return true;
			}
		});
		btnWalk.addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				handleMoveButtonPress(btnWalk, btns);
				return true;
			}
		});
		btnCrouch.addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				handleMoveButtonPress(btnCrouch, btns);
				return true;
			}
		});
		btnCrawl.addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				handleMoveButtonPress(btnCrawl, btns);
				return true;
			}
		});
		return table;
	}

	private void handleMoveButtonPress(ImageButton thisBtn, ArrayMap<ImageButton, CharacterActionComponent.Action> btns) {
		boolean checked = thisBtn.isChecked();
		if (!checked) {
			for (ImageButton btn : btns.keys()) {
				btn.setChecked(false);
			}
			thisBtn.setChecked(true);
		}
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
			checkBox.addListener(new InputListener() {
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					checkBox.toggle();
					try {
						field.setBoolean(field, checkBox.isChecked());
					} catch (IllegalAccessException e) {
						Gdx.app.debug(tag, "Cannot set value for " + field.getName());
					}
					return true;
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
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		viewport.setCamera(cameraUI);
		boolean result = super.touchDown(screenX, screenY, pointer, button);
		viewport.setCamera(camera3D);
		return result;
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
}
