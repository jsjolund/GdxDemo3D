/*******************************************************************************
 * Copyright 2014 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.mygdx.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.StringBuilder;

/** A label continuously showing the most up-to-date value prefixed by a constant string.
 *
 * @param <T> the type of the value 
 * 
 * @author davebaol */
public abstract class ObjectValueLabel<T> extends Label {

	T oldValue;
	int appendIndex;

	public ObjectValueLabel (CharSequence text, T initialValue, Skin skin) {
		this(text, initialValue, skin.get(LabelStyle.class));
	}

	public ObjectValueLabel (CharSequence text, T initialValue, Skin skin, String styleName) {
		this(text, initialValue, skin.get(styleName, LabelStyle.class));
	}

	public ObjectValueLabel (CharSequence text, T initialValue, Skin skin, String fontName, Color color) {
		this(text, initialValue, new LabelStyle(skin.getFont(fontName), color));
	}

	public ObjectValueLabel (CharSequence text, T initialValue, Skin skin, String fontName, String colorName) {
		this(text, initialValue, new LabelStyle(skin.getFont(fontName), skin.getColor(colorName)));
	}

	public ObjectValueLabel (CharSequence text, T initialValue, LabelStyle style) {
		super(text.toString() + initialValue, style);
		this.oldValue = initialValue;
		this.appendIndex = text.length();
	}

	public abstract T getValue();

	public abstract void copyValue(T newValue, T oldValue);

	@Override
	public void act (float delta) {
		T newValue = getValue();
		if (!oldValue.equals(newValue)) {
			copyValue(newValue, oldValue);
			StringBuilder sb = getText();
			sb.setLength(appendIndex);
			sb.append(oldValue);
			invalidateHierarchy();
		}
		super.act(delta);
	}

}
