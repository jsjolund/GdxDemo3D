package com.mygdx.game.utilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

public class Sounds {

	static public Sound whistle;
	
	private Sounds () {
	}

	public static void load() {
		whistle = load("sounds/whistle.mp3");
	}

	public static void dispose() {
		whistle.dispose();
	}

	private static Sound load(String file) {
		Sound sound = Gdx.audio.newSound(Gdx.files.internal(file));
		sound.play(0); // this should force loading on Android, so avoiding the wait on first play in game
		return sound;
	}
}
