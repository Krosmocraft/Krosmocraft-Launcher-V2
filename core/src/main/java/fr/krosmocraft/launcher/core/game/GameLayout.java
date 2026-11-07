package fr.krosmocraft.launcher.core.game;

import java.nio.file.Path;

public record GameLayout(Path root) {

	public Path settingsFile() {
		return root.resolve("launcher.json");
	}

	public Path logsDir() {
		return root.resolve("logs");
	}

	public Path runtimeDir(String component) {
		return root.resolve("runtime").resolve(component);
	}

	public Path librariesDir() {
		return root.resolve("libraries");
	}

	public Path assetsDir() {
		return root.resolve("assets");
	}

	public Path versionDir(String id) {
		return root.resolve("versions").resolve(id);
	}

	public Path versionJson(String id) {
		return versionDir(id).resolve(id + ".json");
	}

	public Path clientJar(String id) {
		return versionDir(id).resolve(id + ".jar");
	}

	public Path nativesDir(String id) {
		return versionDir(id).resolve("natives");
	}

	public Path gameDir() {
		return root.resolve("game");
	}
}
