package fr.krosmocraft.launcher.core.game;

import com.google.gson.JsonObject;
import fr.krosmocraft.launcher.core.Json;
import fr.krosmocraft.launcher.core.net.Downloader;
import fr.krosmocraft.launcher.core.net.FileDownload;
import fr.krosmocraft.launcher.core.net.Http;
import fr.krosmocraft.launcher.core.platform.Platform;
import fr.krosmocraft.launcher.core.progress.ProgressListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GameInstaller {

	private static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
	private static final String FABRIC_PROFILE = "https://meta.fabricmc.net/v2/versions/loader/%s/%s/profile/json";
	private static final String ASSETS_URL = "https://resources.download.minecraft.net/";

	private final GameLayout layout;
	private final Platform platform;

	public GameInstaller(GameLayout layout, Platform platform) {
		this.layout = layout;
		this.platform = platform;
	}

	public VersionProfile install(String minecraftVersion, String fabricLoaderVersion, Downloader.Verify verify,
								  ProgressListener progress) throws IOException {
		progress.step("install.versions");
		VersionProfile vanilla = vanillaProfile(minecraftVersion);
		VersionProfile fabric = fabricProfile(minecraftVersion, fabricLoaderVersion);
		VersionProfile merged = ProfileMerger.merge(fabric, vanilla);

		progress.step("install.libraries");
		List<FileDownload> files = new ArrayList<>();
		var client = vanilla.downloads().client();
		files.add(new FileDownload(client.url(), layout.clientJar(vanilla.id()), client.sha1(), client.size()));
		for (VersionProfile.Library lib : merged.libraries()) {
			if (Rules.allowed(lib.rules(), platform, Set.of())) {
				LibraryArtifact a = LibraryArtifact.of(lib);
				files.add(new FileDownload(a.url(), layout.librariesDir().resolve(a.path()), a.sha1(), a.size()));
			}
		}
		if (merged.logging() != null && merged.logging().client() != null) {
			var log = merged.logging().client().file();
			files.add(new FileDownload(log.url(), layout.assetsDir().resolve("log_configs").resolve(log.id()), log.sha1(), log.size()));
		}
		Downloader.run(files, verify, progress);

		progress.step("install.assets");
		Downloader.run(assetFiles(merged.assetIndex()), verify, progress);

		// lwjgl extracts its natives itself, the folder just has to exist
		Files.createDirectories(layout.nativesDir(merged.id()));
		Files.createDirectories(layout.gameDir());
		return merged;
	}

	private VersionProfile vanillaProfile(String mc) throws IOException {
		Path json = layout.versionJson(mc);
		try {
			JsonObject manifest = Http.getJson(VERSION_MANIFEST, JsonObject.class);
			for (var e : manifest.getAsJsonArray("versions")) {
				JsonObject v = e.getAsJsonObject();
				if (v.get("id").getAsString().equals(mc)) {
					Downloader.run(List.of(new FileDownload(v.get("url").getAsString(), json, v.get("sha1").getAsString(), -1)),
							Downloader.Verify.FULL, ProgressListener.NONE);
					return read(json);
				}
			}
			throw new IOException("Minecraft " + mc + " not in Mojang's version manifest");
		} catch (IOException e) {
			if (Files.isRegularFile(json)) {
				return read(json); // offline: last known good
			}
			throw e;
		}
	}

	private VersionProfile fabricProfile(String mc, String loader) throws IOException {
		String id = "fabric-loader-" + loader + "-" + mc;
		Path json = layout.versionJson(id);
		try {
			Files.createDirectories(json.getParent());
			Files.writeString(json, Http.getString(FABRIC_PROFILE.formatted(mc, loader)));
		} catch (IOException e) {
			if (!Files.isRegularFile(json)) {
				throw e;
			}
		}
		return read(json);
	}

	private List<FileDownload> assetFiles(VersionProfile.AssetIndexRef ref) throws IOException {
		Path indexFile = layout.assetsDir().resolve("indexes").resolve(ref.id() + ".json");
		Downloader.run(List.of(new FileDownload(ref.url(), indexFile, ref.sha1(), ref.size())), Downloader.Verify.FULL,
				ProgressListener.NONE);
		JsonObject objects = Json.GSON.fromJson(Files.readString(indexFile), JsonObject.class).getAsJsonObject("objects");
		List<FileDownload> files = new ArrayList<>();
		for (Map.Entry<String, com.google.gson.JsonElement> e : objects.entrySet()) {
			JsonObject o = e.getValue().getAsJsonObject();
			String hash = o.get("hash").getAsString();
			String rel = hash.substring(0, 2) + "/" + hash;
			files.add(new FileDownload(ASSETS_URL + rel, layout.assetsDir().resolve("objects").resolve(rel), hash,
					o.get("size").getAsLong()));
		}
		return files;
	}

	private static VersionProfile read(Path json) throws IOException {
		return Json.GSON.fromJson(Files.readString(json), VersionProfile.class);
	}
}
