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
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.mygdx.game.GameScreen;
import com.mygdx.game.blender.objects.BlenderCamera;
import com.mygdx.game.blender.objects.BlenderEmpty;
import com.mygdx.game.blender.objects.BlenderLight;
import com.mygdx.game.blender.objects.BlenderModel;
import com.mygdx.game.blender.objects.BlenderObject;

/**
 * Reads blender json exported from a particular .blend file, and loads
 * all {@link Model} assets required to render the scene.
 * </p>
 * When the json is read, Game object placeholders called {@link BlenderObject}s are created
 * and placed in 3D space.
 *
 * @author jsjolund
 */
public class BlenderAssetManager implements Disposable {

	/**
	 * Holds an id reference to an asset by id, e.g. Model.class -> "dog" -> Model
	 * or Texture.class -> "dogtexture" -> Texture
	 *
	 * @param <T>
	 */
	private class DisposableHolder<T extends Disposable> implements Disposable {
		ObjectMap<Class<T>, ObjectMap<String, T>> map = new ObjectMap<Class<T>, ObjectMap<String, T>>();

		/**
		 * Add a disposable asset to be held
		 *
		 * @param name  Asset name
		 * @param asset The asset
		 * @param type  Type of asset
		 */
		public void add(String name, T asset, Class<T> type) {
			if (!map.containsKey(type)) {
				map.put(type, new ObjectMap<String, T>());
			}
			ObjectMap<String, T> innerMap = map.get(type);
			if (innerMap.containsKey(name)) {
				throw new GdxRuntimeException("Asset name is already used, try changing it: '" + name + "'");
			}
			innerMap.put(name, asset);
		}

		/**
		 * Get a held asset
		 *
		 * @param name
		 * @param type
		 * @return
		 */
		public T get(String name, Class<T> type) {
			return map.get(type).get(name);
		}

		public boolean contains(String id, Class<T> type) {
			return map.containsKey(type) && map.get(type).containsKey(id);
		}

		@Override
		public void dispose() {
			for (ObjectMap.Entry<Class<T>, ObjectMap<String, T>> entryClass : map) {
				for (ObjectMap.Entry<String, T> entryId : entryClass.value) {
					entryId.value.dispose();
				}
			}
			map.clear();
		}
	}

	private class BlenderTexture extends BlenderObject {
		String filePath;

		public BlenderTexture(String name, String filePath) {
			this.name = name;
			this.filePath = filePath;
		}
	}

	private final String modelPath;
	private final String modelExt;
	private final String pfxPath;

	private final ModelLoader.ModelParameters modelParameters;
	private final TextureLoader.TextureParameter textureParameter;
	private final ParticleEffectLoader.ParticleEffectLoadParameter pfxParameter;
	
	private final AssetManager assetManager = GameScreen.screen.getGame().getAssetManager();
	private final DisposableHolder disposableHolder = new DisposableHolder();


	public BlenderAssetManager(
			ModelLoader.ModelParameters modelParameters,
			TextureLoader.TextureParameter textureParameter,
			ParticleEffectLoader.ParticleEffectLoadParameter pfxParameter,
			String pfxPath, String modelPath, String modelExt) {
		this.modelExt = modelExt;
		this.modelPath = modelPath;
		this.pfxPath = pfxPath;

		this.modelParameters = modelParameters;
		this.textureParameter = textureParameter;
		this.pfxParameter = pfxParameter;
	}

	public <T extends Disposable> void manageDisposable(String assetId, T asset, Class<T> type) {
		disposableHolder.add(assetId, asset, type);
	}

	public <T> void manageDisposableFromPath(String assetId, String localPath, Class<T> type) {
		if (type == Texture.class) {
			sceneData.textures.add(new BlenderTexture(assetId, localPath));
			assetManager.load(localPath, Texture.class, textureParameter);
		} else if (type == Model.class) {
			sceneData.models.add(new BlenderModel(assetId, localPath));
			assetManager.load(localPath, Model.class, modelParameters);
		} else {
			throw new GdxRuntimeException("Asset type not supported '" + type + "'");
		}
	}

	private static class BlenderObjectMap<T extends BlenderObject> extends ArrayMap<String, Array<T>> {

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

		public void addAll(BlenderObjectMap<T> other) {
			for (Array<T> value : other.values()) {
				addAll(value);
			}
		}

		public Array<T> removeByName(String name) {
			return removeKey(name);
		}

		public Array<T> getByName(String name) {
			return get(name);
		}

	}

	private static class BlenderSceneData implements Json.Serializable {

		final BlenderObjectMap<BlenderTexture> textures = new BlenderObjectMap<BlenderTexture>();
		final BlenderObjectMap<BlenderModel> models = new BlenderObjectMap<BlenderModel>();
		final BlenderObjectMap<BlenderEmpty> empties = new BlenderObjectMap<BlenderEmpty>();
		final BlenderObjectMap<BlenderLight> lights = new BlenderObjectMap<BlenderLight>();
		final BlenderObjectMap<BlenderCamera> cameras = new BlenderObjectMap<BlenderCamera>();

		@Override
		public void write(Json json) {

		}

		@Override
		public void read(Json json, JsonValue jsonData) {
			for (JsonValue category : jsonData) {
				if (category.name.equals("model")) {
					models.addAll(json.readValue(Array.class, BlenderModel.class, category));
				} else if (category.name.equals("empty")) {
					empties.addAll(json.readValue(Array.class, BlenderEmpty.class, category));
				} else if (category.name.equals("light")) {
					lights.addAll(json.readValue(Array.class, BlenderLight.class, category));
				} else if (category.name.equals("camera")) {
					cameras.addAll(json.readValue(Array.class, BlenderCamera.class, category));
				}
			}
		}

		public void add(BlenderSceneData other) {
			textures.addAll(other.textures);
			models.addAll(other.models);
			empties.addAll(other.empties);
			lights.addAll(other.lights);
			cameras.addAll(other.cameras);
		}
	}

	BlenderSceneData sceneData = new BlenderSceneData();

	public void load(String jsonPath) {
		BlenderSceneData newData = new Json().fromJson(BlenderSceneData.class, Gdx.files.internal(jsonPath));
		// Load models with assetmanager
		for (String model : newData.models.keys()) {
			BlenderModel bModel = newData.models.get(model).first();
			String filePath = modelPath + bModel.model_file_name + modelExt;
			assetManager.load(filePath, Model.class, modelParameters);
		}
		// Load particle effects with assetmanager
		for (Array<BlenderEmpty> empties : newData.empties.values()) {
			for (BlenderEmpty empty : empties) {
				if (empty.custom_properties.containsKey("pfx")) {
					String filePath = pfxPath + empty.custom_properties.get("pfx");
					assetManager.load(filePath, ParticleEffect.class, pfxParameter);
				}
			}
		}
		sceneData.add(newData);
	}

	public <T extends Disposable> T getAsset(String assetId, Class<T> type) {
		if (disposableHolder.contains(assetId, type)) {
			return (T) disposableHolder.get(assetId, type);
		}

		String filePath = null;

		if (type == Model.class) {
			try {
				String fileName = sceneData.models.getByName(assetId).first().model_file_name;
				filePath = modelPath + fileName + modelExt;
			} catch (Exception e) {
				throw new GdxRuntimeException("Could not find asset type:'" + type + "', name:'" + assetId + "'");
			}

		} else if (type == Texture.class) {
			try {
				filePath = sceneData.textures.getByName(assetId).first().filePath;
			} catch (Exception e) {
				throw new GdxRuntimeException("Could not find asset type:'" + type + "', name:'" + assetId + "'");
			}
			
		} else if (type == ParticleEffect.class) {
			try {
				filePath = pfxPath + assetId;
			} catch (Exception e) {
				throw new GdxRuntimeException("Could not find asset type:'" + type + "', name:'" + assetId + "'");
			}
		}

		assetManager.finishLoadingAsset(filePath);
		return assetManager.get(filePath, type);
	}
	
	@SuppressWarnings("unchecked")
	private <S extends BlenderObjectMap<T>, T extends BlenderObject> S getTypeMap(Class<T> objClass) {
		S map = null;
		if (objClass == BlenderModel.class) {
			map = (S) sceneData.models;
		} else if (objClass == BlenderLight.class) {
			map = (S) sceneData.lights;
		} else if (objClass == BlenderEmpty.class) {
			map = (S) sceneData.empties;
		} else if (objClass == BlenderCamera.class) {
			map = (S) sceneData.cameras;
		} else {
			throw new GdxRuntimeException("Unknown map for type '" + objClass + "'");
		}
		return map;
	}

	public <T extends BlenderObject> Array<String> getPlaceholderIdsByType(Class<T> objClass) {
		return getTypeMap(objClass).keys().toArray();
	}

	public <T extends BlenderObject> void addPlaceholder(T instance, Class<T> type) {
		getTypeMap(type).add(instance);
	}

	public <T extends BlenderObject> void addPlaceholders(Array<T> instances, Class<T> type) {
		getTypeMap(type).addAll(instances);
	}

	public <S extends Array<T>, T extends BlenderObject> S getPlaceholders(String assetName, Class<T> type, S out) {
		Array<T> instances = getTypeMap(type).getByName(assetName);
		if (instances != null) {
			out.addAll(instances);
		}
		return out;
	}

	public <S extends Array<T>, T extends BlenderObject> S getAllPlaceholders(Class<T> type, S out) {
		BlenderObjectMap<T> map = getTypeMap(type);
		for (ObjectMap.Entry<String, Array<T>> entry : map) {
			out.addAll(entry.value);
		}
		return out;
	}

	@Override
	public void dispose() {
		disposableHolder.dispose();
	}

}
