package fr.krosmocraft.launcher.core;

import fr.krosmocraft.launcher.core.auth.AuthResult;
import fr.krosmocraft.launcher.core.config.LauncherSettings;
import fr.krosmocraft.launcher.core.game.GameInstaller;
import fr.krosmocraft.launcher.core.game.GameLayout;
import fr.krosmocraft.launcher.core.game.JavaRuntimeInstaller;
import fr.krosmocraft.launcher.core.game.LaunchCommand;
import fr.krosmocraft.launcher.core.game.LaunchGuard;
import fr.krosmocraft.launcher.core.game.VersionProfile;
import fr.krosmocraft.launcher.core.mods.ClientManifest;
import fr.krosmocraft.launcher.core.mods.FileSync;
import fr.krosmocraft.launcher.core.net.Downloader;
import fr.krosmocraft.launcher.core.net.Http;
import fr.krosmocraft.launcher.core.platform.Platform;
import fr.krosmocraft.launcher.core.preset.Preset;
import fr.krosmocraft.launcher.core.preset.PresetApplier;
import fr.krosmocraft.launcher.core.progress.ProgressListener;
import fr.krosmocraft.launcher.core.server.ServerEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LaunchPipeline {

	public static final String CDN = "https://krosmocraft.fr/downloads/";

	public record Prepared(ClientManifest manifest, VersionProfile profile, Path java) {
	}

	private final GameLayout layout;
	private final Platform platform;

	public LaunchPipeline(GameLayout layout, Platform platform) {
		this.layout = layout;
		this.platform = platform;
	}

	public static ClientManifest fetchManifest(ServerEntry server) throws IOException {
		try {
			return Http.getJson(CDN + server.channel() + "/manifest.json", ClientManifest.class);
		} catch (IOException e) {
			// test server without its own channel yet
			if (server.channel().startsWith("dev/")) {
				return Http.getJson(CDN + "beta/manifest.json", ClientManifest.class);
			}
			throw e;
		}
	}

	public Prepared prepare(ClientManifest manifest, String presetId, Downloader.Verify verify, ProgressListener progress)
			throws IOException {
		VersionProfile profile = new GameInstaller(layout, platform)
				.install(manifest.minecraft(), manifest.fabricLoader(), verify, progress);
		Path java = new JavaRuntimeInstaller(layout, platform).install(profile.javaVersion(), verify, progress);
		FileSync.sync(manifest, manifest.preset(presetId), layout.gameDir(), verify, progress);
		return new Prepared(manifest, profile, java);
	}

	public void syncPresetFiles(ClientManifest manifest, String presetId, ProgressListener progress) throws IOException {
		FileSync.sync(manifest, manifest.preset(presetId), layout.gameDir(), Downloader.Verify.FAST, progress);
	}

	public Process launch(Prepared prepared, ServerEntry server, AuthResult.Success session, LauncherSettings settings)
			throws IOException {
		List<String> problems = LaunchGuard.problems();
		if (!problems.isEmpty()) {
			throw new IOException("Options Java non autorisées détectées : " + String.join(", ", problems));
		}
		Preset preset = prepared.manifest().preset(settings.presetId);
		Downloader.run(new GameInstaller(layout, platform).codeFiles(prepared.profile()), Downloader.Verify.FULL, ProgressListener.NONE);
		FileSync.sync(prepared.manifest(), preset, layout.gameDir(), Downloader.Verify.FULL, ProgressListener.NONE);

		if (!preset.stamp().equals(settings.appliedPreset)) {
			PresetApplier.apply(preset, layout.gameDir());
			settings.appliedPreset = preset.stamp();
			settings.save(layout.settingsFile());
		}
		PresetApplier.mergeOptions(layout.gameDir().resolve("options.txt"),
				Map.of("fullscreen", String.valueOf(settings.fullscreen)));

		List<String> modArgs = new ArrayList<>(List.of(
				"--token", session.token(),
				"--server", server.address(),
				"--port", String.valueOf(server.port()),
				"--krosmoLang", settings.krosmoLanguageArg()));
		if (settings.borderless) {
			modArgs.add("--borderless"); // handled by our mod, vanilla has no borderless mode
		}
		List<String> command = LaunchCommand.build(new LaunchCommand.Inputs(
				prepared.profile(), layout, platform, prepared.java(), session.pseudo(), offlineUuid(session.pseudo()),
				settings.memoryMb, settings.windowWidth(), settings.windowHeight(), modArgs));

		Files.createDirectories(layout.logsDir());
		System.out.println("Launching: " + redact(command));
		ProcessBuilder builder = new ProcessBuilder(command);
		LaunchGuard.cleanEnvironment(builder.environment());
		return builder
				.directory(layout.gameDir().toFile())
				.redirectErrorStream(true)
				.redirectOutput(layout.logsDir().resolve("game-stdout.log").toFile())
				.start();
	}

	// same uuid an offline-mode server computes
	public static String offlineUuid(String name) {
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)).toString();
	}

	public static String redact(List<String> command) {
		List<String> out = new ArrayList<>(command);
		for (int i = 0; i < out.size() - 1; i++) {
			if (out.get(i).equals("--token") || out.get(i).equals("--accessToken")) {
				out.set(i + 1, "********");
			}
		}
		return String.join(" ", out);
	}
}
