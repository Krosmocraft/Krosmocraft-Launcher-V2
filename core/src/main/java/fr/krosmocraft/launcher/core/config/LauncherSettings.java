package fr.krosmocraft.launcher.core.config;

import fr.krosmocraft.launcher.core.Json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class LauncherSettings {

	public String username = "";
	public boolean rememberMe = true;
	public int memoryMb = 4096;
	public List<String> jvmArgs = new ArrayList<>();
	public String server = "Live";
	public boolean muted = false;

	public String presetId;
	public String appliedPreset;

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
