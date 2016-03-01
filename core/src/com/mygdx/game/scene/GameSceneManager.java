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

package com.mygdx.game.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * @author jsjolund
 */
public class GameSceneManager implements Disposable {

	private static final String TAG = "GameSceneManager";

	private final ObjectMap<String, GameScene> sceneMap = new ObjectMap<String, GameScene>();
	private final ObjectMap<String, GameObjectBlueprint> sharedBlueprints = new ObjectMap<String, GameObjectBlueprint>();

	private final String modelPath;
	private final String modelExt;
	private final String pfxPath;
	private final ModelLoader.ModelParameters modelParameters;
	private final TextureLoader.TextureParameter textureParameter;
	private final ParticleEffectLoader.ParticleEffectLoadParameter pfxParameter;

	public GameSceneManager(ModelLoader.ModelParameters modelParameters,
							TextureLoader.TextureParameter textureParameter,
							ParticleEffectLoader.ParticleEffectLoadParameter pfxParameter,
							String pfxPath, String modelPath, String modelExt) {
		this.modelPath = modelPath;
		this.modelExt = modelExt;
		this.pfxPath = pfxPath;

		this.modelParameters = modelParameters;
		this.textureParameter = textureParameter;
		this.pfxParameter = pfxParameter;
	}

	public GameScene open(String sceneId) {
		if (!sceneMap.containsKey(sceneId)) {
			sceneMap.put(sceneId, new GameScene(modelParameters, textureParameter, pfxParameter, 
					pfxPath, modelPath, modelExt, sharedBlueprints));
			Gdx.app.debug(TAG, "Added scene '" + sceneId + "'");
		}
		return sceneMap.get(sceneId);
	}

	public void addSharedBlueprint(String blueprintId, GameObjectBlueprint blueprint) {
		if (sharedBlueprints.containsKey(blueprintId)) {
			throw new GdxRuntimeException("Shared blueprint already exists '" + blueprintId + "'");
		}
		sharedBlueprints.put(blueprintId, blueprint);
		Gdx.app.debug(TAG, "Added shared blueprint '" + blueprintId + "'");
	}

	public void dispose(String sceneId) {
		Gdx.app.debug(TAG, "Disposing scene '" + sceneId + "'");
		sceneMap.get(sceneId).dispose();
	}

	@Override
	public void dispose() {
		for (GameScene scene : sceneMap.values()) {
			scene.dispose();
		}
		for (GameObjectBlueprint bp : sharedBlueprints.values()) {
			bp.dispose();
		}
	}

}
