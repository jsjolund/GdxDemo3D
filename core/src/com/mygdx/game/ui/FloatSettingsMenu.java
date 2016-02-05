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

package com.mygdx.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;

/**
 * @author jsjolund
 */
public class FloatSettingsMenu extends Table {

	private static final String TAG = "FloatSettingsMenu";

	public FloatSettingsMenu(String buttonText, Skin skin, Class<?> floatSettingsClass) {
		final Table innerTable = new Table();

		Field[] fields = ClassReflection.getFields(floatSettingsClass);
		for (final Field field : fields) {
			final Label fieldName = new Label(field.getName(), skin);
			float fieldValueFloat = 0;
			try {
				fieldValueFloat = (Float)field.get(field);
			} catch (Exception e) {
				Gdx.app.debug(TAG, "Cannot parse float value for " + field.getName());
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
						field.set(field, (Float)newFieldValue);
					} catch (ReflectionException e) {
						Gdx.app.debug(TAG, "Cannot set value for " + field.getName());
					}
					return true;
				}
			});
		}
		innerTable.setVisible(false);
		final TextButton button = new TextButton(buttonText, skin);
		button.addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				innerTable.setVisible(!innerTable.isVisible());
				return true;
			}
		});
		add(innerTable).fillX();
		row();
		add(button).fillX();
		row();
	}

}
