package fr.krosmocraft.launcher.core.preset;

import fr.krosmocraft.launcher.core.mods.ManifestFile;

import java.util.List;
import java.util.Map;

public record Preset(
		String id,
		int revision,
		String name,
		String description,
		FpsImpact fpsImpact,
		String screenshot,
		List<ManifestFile> files,
		List<ManifestFile> configFiles,
		Map<String, String> options) {

	public enum FpsImpact { LOW, MEDIUM, HIGH }

	public List<ManifestFile> files() {
		return files != null ? files : List.of();
	}

	public List<ManifestFile> configFiles() {
		return configFiles != null ? configFiles : List.of();
	}

	public Map<String, String> options() {
		return options != null ? options : Map.of();
	}

	public String stamp() {
		return id + "@" + revision;
	}
}
