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

package com.mygdx.game.blender;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Json;
import com.mygdx.game.blender.objects.BlenderObject;

/**
 * @author jsjolund
 */
class BlenderObjectMap<T extends BlenderObject> extends ArrayMap<String, Array<T>> {

	public BlenderObjectMap() {
		super();
	}

	public Array<T> addFromJson(String jsonPath, Class<T> type) {
		Array<T> objects = deserialize(jsonPath, type);
		for (T object : objects) {
			if (containsKey(object.name)) {
				get(object.name).add(object);
			} else {
				Array<T> array = new Array<T>();
				put(object.name, array);
				array.add(object);
			}
		}
		return objects;
	}

	@SuppressWarnings("unchecked")
	private Array<T> deserialize(String jsonPath, Class<T> type) {
		return (jsonPath == null) ? new Array<T>() : new Json().fromJson(Array.class, type, Gdx.files.internal(jsonPath));
	}

	public void add(T object) {
		if (containsKey(object.name)) {
			get(object.name).add(object);
		} else {
			Array<T> array = new Array<T>();
			put(object.name, array);
			array.add(object);
		}
	}

	public void addAll(Array<T> objects) {
		for (T object : objects) {
			add(object);
		}
	}

	public Array<T> removeByName(String name) {
		return removeKey(name);
	}

	public Array<T> getByName(String name) {
		return get(name);
	}

}
