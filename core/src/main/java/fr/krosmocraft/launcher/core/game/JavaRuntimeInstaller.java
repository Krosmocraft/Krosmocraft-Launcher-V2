package fr.krosmocraft.launcher.core.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.krosmocraft.launcher.core.net.Downloader;
import fr.krosmocraft.launcher.core.net.FileDownload;
import fr.krosmocraft.launcher.core.net.Http;
import fr.krosmocraft.launcher.core.platform.Platform;
import fr.krosmocraft.launcher.core.progress.ProgressListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class JavaRuntimeInstaller {

	private static final String RUNTIMES =
			"https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";

	private final GameLayout layout;
	private final Platform platform;

	public JavaRuntimeInstaller(GameLayout layout, Platform platform) {
		this.layout = layout;
		this.platform = platform;
	}

	public Path install(VersionProfile.JavaVersion javaVersion, Downloader.Verify verify, ProgressListener progress)
			throws IOException {
		String component = javaVersion.component();
		Path home = layout.runtimeDir(component);
		Path java = platform.javaExecutable(home);
		progress.step("install.java");

		JsonObject all;
		try {
			all = Http.getJson(RUNTIMES, JsonObject.class);
		} catch (IOException e) {
			if (Files.isRegularFile(java)) {
				return java; // offline, already installed
			}
			throw e;
		}
		JsonObject byPlatform = all.getAsJsonObject(platform.javaRuntimeKey());
		JsonArray entries = byPlatform != null ? byPlatform.getAsJsonArray(component) : null;
		if (entries == null || entries.isEmpty()) {
			throw new IOException("No Mojang Java runtime '" + component + "' for " + platform.javaRuntimeKey());
		}
		String manifestUrl = entries.get(0).getAsJsonObject().getAsJsonObject("manifest").get("url").getAsString();
		JsonObject files = Http.getJson(manifestUrl, JsonObject.class).getAsJsonObject("files");

		List<FileDownload> downloads = new ArrayList<>();
		List<Path> executables = new ArrayList<>();
		List<Map.Entry<Path, String>> links = new ArrayList<>();
		for (var e : files.entrySet()) {
			Path target = home.resolve(e.getKey());
			JsonObject f = e.getValue().getAsJsonObject();
			switch (f.get("type").getAsString()) {
				case "directory" -> Files.createDirectories(target);
				case "link" -> links.add(Map.entry(target, f.get("target").getAsString()));
				case "file" -> {
					JsonObject raw = f.getAsJsonObject("downloads").getAsJsonObject("raw");
					downloads.add(new FileDownload(raw.get("url").getAsString(), target, raw.get("sha1").getAsString(),
							raw.get("size").getAsLong()));
					if (f.has("executable") && f.get("executable").getAsBoolean()) {
						executables.add(target);
					}
				}
				default -> { }
			}
		}
		Downloader.run(downloads, verify, progress);

		if (platform.os() != Platform.Os.WINDOWS) {
			for (Path exe : executables) {
				Files.setPosixFilePermissions(exe, PosixFilePermissions.fromString("rwxr-xr-x"));
			}
			for (var link : links) {
				if (!Files.exists(link.getKey(), LinkOption.NOFOLLOW_LINKS)) {
					Files.createDirectories(link.getKey().getParent());
					Files.createSymbolicLink(link.getKey(), Path.of(link.getValue()));
				}
			}
		}
		return java;
	}
}
