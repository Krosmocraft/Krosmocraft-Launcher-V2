package fr.krosmocraft.bootstrap;

import java.nio.file.Path;
import java.util.Locale;

// same as core Platform.dataDir, the bootstrap can't depend on core
final class DataDir {

	private DataDir() {
	}

	static Path get(String name) {
		String override = System.getenv("KROSMOCRAFT_HOME"); // tests, portable installs
		if (override != null && !override.isBlank()) {
			return Path.of(override);
		}
		String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		String home = System.getProperty("user.home");
		if (os.contains("win")) {
			String appData = System.getenv("APPDATA");
			return Path.of(appData != null ? appData : home, name);
		}
		if (os.contains("mac")) {
			return Path.of(home, "Library", "Application Support", name);
		}
		String xdg = System.getenv("XDG_DATA_HOME");
		return (xdg != null && !xdg.isBlank() ? Path.of(xdg) : Path.of(home, ".local", "share")).resolve(name);
	}
}
