package fr.krosmocraft.launcher.core.preset;

import fr.krosmocraft.launcher.core.mods.FileSync;
import fr.krosmocraft.launcher.core.mods.ManifestFile;
import fr.krosmocraft.launcher.core.net.Downloader;
import fr.krosmocraft.launcher.core.net.FileDownload;
import fr.krosmocraft.launcher.core.progress.ProgressListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PresetApplier {

	private PresetApplier() {
	}

	public static void apply(Preset preset, Path gameDir) throws IOException {
		List<FileDownload> configs = new ArrayList<>();
		for (ManifestFile f : preset.configFiles()) {
			configs.add(new FileDownload(f.url(), FileSync.resolveInside(gameDir, f.path()), f.sha1(), f.size()));
		}
		Downloader.run(configs, Downloader.Verify.FULL, ProgressListener.NONE);
		mergeOptions(gameDir.resolve("options.txt"), preset.options());
	}

	static void mergeOptions(Path file, Map<String, String> overrides) throws IOException {
		if (overrides.isEmpty()) {
			return;
		}
		Map<String, String> lines = new LinkedHashMap<>();
		if (Files.isRegularFile(file)) {
			for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
				int i = line.indexOf(':');
				if (i > 0) {
					lines.put(line.substring(0, i), line.substring(i + 1));
				}
			}
		}
		lines.putAll(overrides);
		List<String> out = new ArrayList<>();
		lines.forEach((k, v) -> out.add(k + ":" + v));
		Files.createDirectories(file.getParent());
		Files.write(file, out, StandardCharsets.UTF_8);
	}
}
