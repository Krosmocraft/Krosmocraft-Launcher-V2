package fr.krosmocraft.launcher.core.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fr.krosmocraft.launcher.core.Json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

public final class LauncherSettings {

	public String username = "";
	public boolean rememberMe = false;
	public String savedPassword = "";

	public int memoryMb = 4096;
	public String gameSize = "854x480";
	public boolean fullscreen = false;
	public boolean borderless = false;

	public String server = "Live";
	public boolean muted = false;

	public String presetId;
	public String appliedPreset;

	public static final String AUTO = "auto"; // Follows the system language
	public static final List<String> LANGUAGES = List.of(AUTO, "fr_fr", "en_us", "es_es");

	public String krosmoLanguage = AUTO;

	public String krosmoLanguageArg() {
		return LANGUAGES.contains(krosmoLanguage) ? krosmoLanguage : AUTO;
	}

	public String krosmoLanguageCode() {
		String choice = krosmoLanguageArg();
		return choice.equals(AUTO) ? systemLanguage(Locale.getDefault()) : choice;
	}

	// Picks up a language changed in game by player
	public void syncKrosmoLanguage(Path gameDir) {
		Path file = gameDir.resolve("config").resolve("krosmocraft").resolve("settings.json");
		try {
			if (Files.isRegularFile(file)) {
				JsonObject json = Json.GSON.fromJson(Files.readString(file), JsonObject.class);
				if (json != null && json.get("krosmoLanguage") instanceof JsonPrimitive p && LANGUAGES.contains(p.getAsString())) {
					krosmoLanguage = p.getAsString();
				}
			}
		} catch (IOException | RuntimeException e) {
			// unreadable game settings, keep ours
		}
	}

	public static String systemLanguage(Locale locale) {
		return switch (locale.getLanguage()) {
			case "fr" -> "fr_fr";
			case "es" -> "es_es";
			default -> "en_us";
		};
	}

	public int windowWidth() {
		return part(0);
	}

	public int windowHeight() {
		return part(1);
	}

	private int part(int i) {
		try {
			return Integer.parseInt(gameSize.split("x")[i]);
		} catch (RuntimeException e) {
			return 0;
		}
	}

	public static LauncherSettings load(Path file) {
		try {
			if (Files.isRegularFile(file)) {
				LauncherSettings s = Json.GSON.fromJson(Files.readString(file), LauncherSettings.class);
				if (s != null) {
					return s;
				}
			}
		} catch (IOException | RuntimeException e) {
			// corrupt file, use defaults
		}
		return new LauncherSettings();
	}

	public void save(Path file) throws IOException {
		Files.createDirectories(file.getParent());
		Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
		Files.writeString(tmp, Json.GSON.toJson(this));
		Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}
}
