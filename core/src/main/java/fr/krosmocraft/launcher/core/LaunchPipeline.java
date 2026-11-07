package fr.krosmocraft.launcher.core;

import fr.krosmocraft.launcher.core.auth.AuthResult;
import fr.krosmocraft.launcher.core.config.LauncherSettings;
import fr.krosmocraft.launcher.core.game.GameInstaller;
import fr.krosmocraft.launcher.core.game.GameLayout;
import fr.krosmocraft.launcher.core.game.JavaRuntimeInstaller;
import fr.krosmocraft.launcher.core.game.LaunchCommand;
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
import java.util.UUID;

public final class LaunchPipeline {

	public static final String CDN = "https://krosmocraft.fr/downloads/";

	private final GameLayout layout;
	private final Platform platform;

	public LaunchPipeline(GameLayout layout, Platform platform) {
		this.layout = layout;
		this.platform = platform;
	}

	public static ClientManifest fetchManifest(ServerEntry server) throws IOException {
		return Http.getJson(CDN + server.channel() + "/manifest.json", ClientManifest.class);
	}

	public Process prepareAndLaunch(ClientManifest manifest, ServerEntry server, AuthResult.Success session,
									LauncherSettings settings, boolean resetSettings, Downloader.Verify verify,
									ProgressListener progress) throws IOException {
		VersionProfile profile = new GameInstaller(layout, platform)
				.install(manifest.minecraft(), manifest.fabricLoader(), verify, progress);
		Path java = new JavaRuntimeInstaller(layout, platform).install(profile.javaVersion(), verify, progress);

		Preset preset = manifest.preset(settings.presetId);
		FileSync.sync(manifest, preset, layout.gameDir(), verify, progress);
		if (resetSettings || !preset.stamp().equals(settings.appliedPreset)) {
			progress.step("install.preset");
			PresetApplier.apply(preset, layout.gameDir());
			settings.appliedPreset = preset.stamp();
			settings.save(layout.settingsFile());
		}

		progress.step("launch");
		List<String> modArgs = new ArrayList<>(List.of(
				"--token", session.token(),
				"--server", server.address(),
				"--port", String.valueOf(server.port())));
		List<String> command = LaunchCommand.build(new LaunchCommand.Inputs(
				profile, layout, platform, java, session.pseudo(), offlineUuid(session.pseudo()),
				settings.memoryMb, settings.jvmArgs, modArgs));

		Files.createDirectories(layout.logsDir());
		return new ProcessBuilder(command)
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
