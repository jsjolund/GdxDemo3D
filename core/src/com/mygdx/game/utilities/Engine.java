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

package com.mygdx.game.utilities;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;

public class Engine {

	private Array<Entity> entities;
	private LongMap<Entity> entitiesById;

	private long nextEntityId = 1;

	public Engine(){
		entities = new Array<Entity>(false, 16);
		entitiesById = new LongMap<Entity>();
	}

	private long obtainEntityId() {
		return nextEntityId++;
	}

	/**
	 * Adds an entity to this Engine.
	 * This will throw an IllegalArgumentException if the given entity
	 * was already registered with an engine.
	 */
	public void addEntity(Entity entity){
		if (entity.uuid != 0L) {
			throw new IllegalArgumentException("Entity is already registered with an Engine id = " + entity.uuid);
		}

		entity.uuid = obtainEntityId();
		
		entities.add(entity);
		entitiesById.put(entity.getId(), entity);
	}

	/**
	 * Removes an entity from this Engine.
	 */
	public void removeEntity(Entity entity){
		boolean removed = false;
		
		entities.removeValue(entity, true);
		
		if (entitiesById.remove(entity.getId()) == entity) {
			removed = true;
		}

		if (removed) {
			entity.uuid = 0L;
		}
	}

	public Entity getEntity(long id) {
		return entitiesById.get(id);
	}

}
