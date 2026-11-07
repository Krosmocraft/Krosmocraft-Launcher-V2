package fr.krosmocraft.launcher.app;

import java.util.Map;

public final class Texts {

	private static final Map<String, String> STEPS = Map.of(
			"install.versions", "Indexion de la version Minecraft...",
			"install.libraries", "Téléchargement des librairies...",
			"install.assets", "Téléchargement des ressources...",
			"install.java", "Téléchargement de java...",
			"install.mods", "Indexion des fichiers Krosmocraft...");

	private Texts() {
	}

	public static String step(String key) {
		return STEPS.getOrDefault(key, "Vérification de l'installation...");
	}
}
