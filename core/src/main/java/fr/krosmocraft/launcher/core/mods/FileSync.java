package fr.krosmocraft.launcher.core.mods;

import fr.krosmocraft.launcher.core.net.Downloader;
import fr.krosmocraft.launcher.core.net.FileDownload;
import fr.krosmocraft.launcher.core.preset.Preset;
import fr.krosmocraft.launcher.core.progress.ProgressListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class FileSync {

	private FileSync() {
	}

	public static void sync(ClientManifest manifest, Preset preset, Path gameDir, Downloader.Verify verify,
							ProgressListener progress) throws IOException {
		progress.step("install.mods");
		List<ManifestFile> wanted = new ArrayList<>(manifest.files());
		wanted.addAll(preset.files());

		List<FileDownload> downloads = new ArrayList<>();
		Set<Path> keep = new HashSet<>();
		for (ManifestFile f : wanted) {
			Path target = resolveInside(gameDir, f.path());
			keep.add(target);
			downloads.add(new FileDownload(f.url(), target, f.sha1(), f.size()));
		}
		Downloader.run(downloads, verify, progress);

		for (String dir : manifest.managedDirs()) {
			Path managed = resolveInside(gameDir, dir);
			if (!Files.isDirectory(managed)) {
				continue;
			}
			try (Stream<Path> walk = Files.walk(managed)) {
				for (Path p : walk.filter(Files::isRegularFile).toList()) {
					if (!keep.contains(p.normalize())) {
						Files.delete(p);
					}
				}
			}
		}
	}

	// reject paths outside the game dir
	public static Path resolveInside(Path gameDir, String relative) throws IOException {
		Path root = gameDir.toAbsolutePath().normalize();
		Path p = root.resolve(relative).normalize();
		if (!p.startsWith(root) || p.equals(root)) {
			throw new IOException("Manifest path escapes the game directory: " + relative);
		}
		return p;
	}
}
