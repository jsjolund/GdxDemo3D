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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;

/**
 * @author jsjolund
 */
public class BooleanSettingsMenu extends Table {

	private static final String TAG = "BooleanSettingsMenu";

	public BooleanSettingsMenu(String buttonText, Skin skin, Class<?> booleanSettingsClass) {
		final Table innerTable = new Table();

		Field[] fields = ClassReflection.getDeclaredFields(booleanSettingsClass);
		for (final Field field : fields) {
			boolean fieldValueBoolean = false;
			try {
				fieldValueBoolean = (Boolean)field.get(field);
			} catch (Exception e) {
				Gdx.app.debug(TAG, "Cannot parse value for boolean " + field.getName());
			}
			final CheckBox checkBox = new CheckBox(field.getName(), skin);
			checkBox.setChecked(fieldValueBoolean);
			innerTable.add(checkBox).pad(1).align(Align.left);
			innerTable.row();
			checkBox.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					try {
						field.set(field, (Boolean)checkBox.isChecked());
					} catch (ReflectionException e) {
						Gdx.app.debug(TAG, "Cannot set value for " + field.getName());
					}
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
