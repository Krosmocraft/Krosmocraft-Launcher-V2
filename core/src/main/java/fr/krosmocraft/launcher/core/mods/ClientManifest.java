package fr.krosmocraft.launcher.core.mods;

import fr.krosmocraft.launcher.core.preset.Preset;

import java.util.List;

public record ClientManifest(
		String minecraft,
		String fabricLoader,
		List<String> managedDirs,
		List<ManifestFile> files,
		List<Preset> presets,
		String defaultPreset) {

	public Preset preset(String id) {
		return presets.stream().filter(p -> p.id().equals(id)).findFirst()
				.orElseGet(() -> presets.stream().filter(p -> p.id().equals(defaultPreset)).findFirst().orElseThrow());
	}
}
