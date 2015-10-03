package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g3d.ModelInstance;

/**
 * Created by user on 8/24/15.
 */
public class SelectableComponent implements Component {

	public boolean isSelected = false;
	public ModelInstance selectedMarkerModel;

	public SelectableComponent(ModelInstance selectedMarkerModel) {
		this.selectedMarkerModel = selectedMarkerModel;
	}
}
