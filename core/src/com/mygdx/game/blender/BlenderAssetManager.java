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

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.mygdx.game.blender.objects.*;

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
		ObjectMap<Class, ObjectMap<String, T>> map = new ObjectMap<Class, ObjectMap<String, T>>();

		/**
		 * Add a disposable asset to be held
		 *
		 * @param id    Asset id
		 * @param asset The asset
		 * @param type  Type of asset
		 */
		public void add(String id, T asset, Class<T> type) {
			if (!map.containsKey(type)) {
				map.put(type, new ObjectMap<String, T>());
			}
			ObjectMap<String, T> innerMap = map.get(type);
			if (innerMap.containsKey(id)) {
				throw new GdxRuntimeException(String.format(
						"Asset id is already used, try changing it: '%s'", id));
			}
			innerMap.put(id, asset);
		}

		/**
		 * Get a held asset
		 *
		 * @param id
		 * @param type
		 * @return
		 */
		public T get(String id, Class<T> type) {
			return map.get(type).get(id);
		}

		public boolean contains(String id, Class<T> type) {
			return map.containsKey(type) && map.get(type).containsKey(id);
		}

		@Override
		public void dispose() {
			for (ObjectMap.Entry<Class, ObjectMap<String, T>> entryClass : map) {
				for (ObjectMap.Entry<String, T> entryId : entryClass.value) {
					entryId.value.dispose();
				}
			}
			map.clear();
		}
	}

	private class BlenderTexture extends BlenderObject {
		String filePath;

		public BlenderTexture(String id, String filePath) {
			this.id = id;
			this.filePath = filePath;
		}
	}

	private final String modelPath;
	private final String modelExt;
	private final ModelLoader.ModelParameters modelParameters;
	private final TextureLoader.TextureParameter textureParameter;
	private final BlenderObjectMap<BlenderTexture> textures = new BlenderObjectMap<BlenderTexture>();
	private final BlenderObjectMap<BlenderModel> models = new BlenderObjectMap<BlenderModel>();
	private final BlenderObjectMap<BlenderEmpty> empties = new BlenderObjectMap<BlenderEmpty>();
	private final BlenderObjectMap<BlenderLight> lights = new BlenderObjectMap<BlenderLight>();
	private final BlenderObjectMap<BlenderCamera> cameras = new BlenderObjectMap<BlenderCamera>();
	private final AssetManager assetManager = new AssetManager();
	private final DisposableHolder disposableHolder = new DisposableHolder();

	public BlenderAssetManager(String modelPath, String modelExt) {
		this.modelExt = modelExt;
		this.modelPath = modelPath;

		modelParameters = new ModelLoader.ModelParameters();
		modelParameters.textureParameter.genMipMaps = true;
		modelParameters.textureParameter.minFilter = Texture.TextureFilter.MipMap;
		modelParameters.textureParameter.magFilter = Texture.TextureFilter.Linear;

		textureParameter = new TextureLoader.TextureParameter();
		textureParameter.genMipMaps = true;
		textureParameter.minFilter = Texture.TextureFilter.MipMap;
		textureParameter.magFilter = Texture.TextureFilter.Linear;
	}

	public <T extends Disposable> void manageDisposable(String assetId, T asset, Class<T> type) {
		disposableHolder.add(assetId, asset, type);
	}

	public <T> void manageDisposableFromPath(String assetId, String localPath, Class<T> type) {
		if (type == Texture.class) {
			textures.add(new BlenderTexture(assetId, localPath));
			assetManager.load(localPath, Texture.class, textureParameter);
		} else if (type == Model.class) {
			models.add(new BlenderModel(assetId, localPath));
			assetManager.load(localPath, Model.class, modelParameters);
		} else {
			throw new GdxRuntimeException(String.format("Asset type not supported '%s'", type));
		}
	}

	public void loadPlaceholders(String jsonPath, Class type) {
		if (type == BlenderModel.class) {
			Array<BlenderModel> mergedObjects = models.addFromJson(jsonPath, BlenderModel.class);
			// Load the models into asset manager
			for (BlenderModel bModel : mergedObjects) {
				String filePath = String.format("%s%s%s", modelPath, bModel.model_file_name, modelExt);
				assetManager.load(filePath, Model.class, modelParameters);
			}
		} else if (type == BlenderLight.class) {
			lights.addFromJson(jsonPath, BlenderLight.class);
		} else if (type == BlenderEmpty.class) {
			empties.addFromJson(jsonPath, BlenderEmpty.class);
		} else if (type == BlenderCamera.class) {
			cameras.addFromJson(jsonPath, BlenderCamera.class);
		} else {
			throw new GdxRuntimeException(String.format("Could not add scene objects of type '%s'", type));
		}
	}


	public <T extends Disposable> T getAsset(String assetId, Class<T> type) {
		if (disposableHolder.contains(assetId, type)) {
			return (T) disposableHolder.get(assetId, type);
		}

		String filePath = null;

		if (type == Model.class) {
			try {
				String fileName = models.getById(assetId).first().model_file_name;
				filePath = String.format("%s%s%s", modelPath, fileName, modelExt);
			} catch (Exception e) {
				throw new GdxRuntimeException(String.format("Could not find asset type:'%s', id:'%s'", type, assetId));
			}

		} else if (type == Texture.class) {
			try {
				filePath = textures.getById(assetId).first().filePath;
			} catch (Exception e) {
				throw new GdxRuntimeException(String.format("Could not find asset type:'%s', id:'%s'", type, assetId));
			}
		}

		assetManager.finishLoadingAsset(filePath);
		return assetManager.get(filePath, type);
	}

	private <S extends BlenderObjectMap<T>, T extends BlenderObject> S getTypeMap(Class<T> objClass) {
		BlenderObjectMap<T> map = null;
		if (objClass == BlenderModel.class) {
			map = (BlenderObjectMap<T>) models;
		} else if (objClass == BlenderLight.class) {
			map = (BlenderObjectMap<T>) lights;
		} else if (objClass == BlenderEmpty.class) {
			map = (BlenderObjectMap<T>) empties;
		} else if (objClass == BlenderCamera.class) {
			map = (BlenderObjectMap<T>) cameras;
		} else {
			throw new GdxRuntimeException(String.format("Unknown map for type '%s'", objClass));
		}
		return (S) map;
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

	public <S extends Array<T>, T extends BlenderObject> S getPlaceholders(String assetId, Class<T> type, Array<T> out) {

		Array<T> instances = getTypeMap(type).getById(assetId);
		if (instances != null) {
			out.addAll(instances);
		}
		return (S) out;
	}

	public <S extends Array<T>, T extends BlenderObject> S getAllPlaceholders(Class<T> type, Array<T> out) {
		BlenderObjectMap<T> map = getTypeMap(type);
		for (ObjectMap.Entry<String, Array<T>> entry : map) {
			out.addAll(entry.value);
		}
		return (S) out;
	}

	@Override
	public void dispose() {
		assetManager.dispose();
		disposableHolder.dispose();

	}


}
