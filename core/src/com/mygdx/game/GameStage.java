package com.mygdx.game;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
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
	private final Table shaderSettingsTable = new Table();
	private final Table debugSettingsTable = new Table();
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

		skin = new Skin(Gdx.files.internal("skins/uiskin.json"));

		addShaderMenu(0, 0, 55, 200, 100);
		addDebugViewMenu(120, 0, 180, 100, 100);
	}

	private void addShaderMenu(float buttonX, float buttonY, float tableX, float tableY, float width) {
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
			shaderSettingsTable.add(fieldName);
			shaderSettingsTable.row();
			shaderSettingsTable.add(fieldValue).width(width);
			shaderSettingsTable.row();
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
		addActor(shaderSettingsTable);
		shaderSettingsTable.setPosition(tableX, tableY);
		shaderSettingsTable.setVisible(false);
		final TextButton button = new TextButton("shader", skin);
		button.addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (shaderSettingsTable.isVisible()) {
					shaderSettingsTable.setVisible(false);
					shaderSettingsTable.remove();
				} else {
					shaderSettingsTable.setVisible(true);
					addActor(shaderSettingsTable);
				}
				return false;
			}
		});
		button.setPosition(buttonX, buttonY);
		button.setWidth(width);
		addActor(button);
	}

	private void addDebugViewMenu(float buttonX, float buttonY, float tableX, float tableY, float width) {
		Field[] fields = DebugViewSettings.class.getFields();
		for (final Field field : fields) {
			final Label fieldName = new Label(field.getName(), skin);
			boolean fieldValueBoolean = false;
			try {
				fieldValueBoolean = field.getBoolean(field);
			} catch (Exception e) {
				Gdx.app.debug(tag, "Cannot parse value for " + field.getName());
			}
			final CheckBox checkBox = new CheckBox(field.getName(), skin);
			checkBox.setChecked(fieldValueBoolean);
			debugSettingsTable.add(checkBox).pad(5).align(Align.left);
			debugSettingsTable.row();
			checkBox.addListener(new InputListener() {
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					checkBox.setChecked(!checkBox.isChecked());
					try {
						field.setBoolean(field, checkBox.isChecked());
					} catch (IllegalAccessException e) {
						Gdx.app.debug(tag, "Cannot set value for " + field.getName());
					}
					return true;
				}
			});
		}
		addActor(debugSettingsTable);
		debugSettingsTable.setPosition(tableX, tableY);
		debugSettingsTable.setVisible(false);
		final TextButton button = new TextButton("debug", skin);
		button.addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (debugSettingsTable.isVisible()) {
					debugSettingsTable.setVisible(false);
					debugSettingsTable.remove();
				} else {
					debugSettingsTable.setVisible(true);
					addActor(debugSettingsTable);
				}
				return false;
			}
		});
		button.setPosition(buttonX, buttonY);
		button.setWidth(width);
		addActor(button);
	}

	public void resize(int width, int height) {
		getViewport().update(width, height, false);
		cameraUI.viewportWidth = viewport.getScreenWidth();
		cameraUI.viewportHeight = viewport.getScreenHeight();
		cameraUI.position.set(viewport.getScreenWidth() / 2, viewport.getScreenHeight() / 2, 0);
		cameraUI.update();
		batch.setProjectionMatrix(cameraUI.combined);
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
	}
}
