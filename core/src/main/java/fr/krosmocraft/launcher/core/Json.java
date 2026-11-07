package fr.krosmocraft.launcher.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class Json {

	public static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(fr.krosmocraft.launcher.core.game.VersionProfile.Argument.class,
					fr.krosmocraft.launcher.core.game.Adapters.argument())
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	private Json() {
	}
}
