package fr.krosmocraft.launcher.app;

import fr.krosmocraft.launcher.core.preset.Preset;

import java.util.Map;

public final class Texts {

	private static final Map<String, String> STEPS = Map.of(
			"install.versions", "Récupération des versions…",
			"install.libraries", "Téléchargement des bibliothèques…",
			"install.assets", "Téléchargement des ressources…",
			"install.java", "Installation de Java…",
			"install.mods", "Mise à jour des mods…",
			"install.preset", "Application des réglages graphiques…",
			"launch", "Lancement du jeu…");

	private Texts() {
	}

	public static String step(String key) {
		return STEPS.getOrDefault(key, key);
	}

	public static String impact(Preset.FpsImpact impact) {
		return switch (impact) {
			case LOW -> "Impact FPS : faible";
			case MEDIUM -> "Impact FPS : moyen";
			case HIGH -> "Impact FPS : élevé";
		};
	}
}
