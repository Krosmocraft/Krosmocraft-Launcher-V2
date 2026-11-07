package fr.krosmocraft.launcher.core.game;

import com.google.gson.JsonDeserializer;
import fr.krosmocraft.launcher.core.game.VersionProfile.Argument;

public final class Adapters {

	private Adapters() {
	}

	public static JsonDeserializer<Argument> argument() {
		return new ArgumentAdapter();
	}
}
