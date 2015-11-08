package com.mygdx.game.utilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

public class Sounds {

	static public Sound whistle;
	
	private Sounds () {
	}

	public static void load() {
		whistle = Gdx.audio.newSound(Gdx.files.internal("sounds/whistle.mp3"));
		
	}

	public static void dispose() {
		whistle.dispose();
	}
}
