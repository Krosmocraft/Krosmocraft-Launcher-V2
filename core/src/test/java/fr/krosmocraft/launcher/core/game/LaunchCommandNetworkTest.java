package fr.krosmocraft.launcher.core.game;

import com.google.gson.JsonObject;
import fr.krosmocraft.launcher.core.Json;
import fr.krosmocraft.launcher.core.net.Http;
import fr.krosmocraft.launcher.core.platform.Platform;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("network")
class LaunchCommandNetworkTest {

	static final String MC = "26.3";
	static final String LOADER = "0.20.1";

	@Test
	void mergedFabricProfileBuildsACommandOnEveryPlatform() throws Exception {
		JsonObject manifest = Http.getJson("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json", JsonObject.class);
		String url = null;
		for (var v : manifest.getAsJsonArray("versions")) {
			if (v.getAsJsonObject().get("id").getAsString().equals(MC)) {
				url = v.getAsJsonObject().get("url").getAsString();
			}
		}
		VersionProfile vanilla = Json.GSON.fromJson(Http.getString(url), VersionProfile.class);
		VersionProfile fabric = Json.GSON.fromJson(Http.getString(
				"https://meta.fabricmc.net/v2/versions/loader/%s/%s/profile/json".formatted(MC, LOADER)), VersionProfile.class);
		VersionProfile merged = ProfileMerger.merge(fabric, vanilla);

		assertEquals("net.fabricmc.loader.impl.launch.knot.KnotClient", merged.mainClass());
		assertEquals(MC, merged.inheritsFromOrId());
		System.out.println("java component: " + merged.javaVersion());

		GameLayout layout = new GameLayout(Path.of("C:/Users/Some One/AppData/Roaming/krosmocraft"));
		for (Platform p : List.of(
				new Platform(Platform.Os.WINDOWS, Platform.Arch.X64),
				new Platform(Platform.Os.MACOS, Platform.Arch.ARM64),
				new Platform(Platform.Os.LINUX, Platform.Arch.X64))) {
			List<String> cmd = LaunchCommand.build(new LaunchCommand.Inputs(merged, layout, p, Path.of("java"),
					"Pepsi", "00000000-0000-0000-0000-000000000000", 4096, List.of(),
					List.of("--token", "abc", "--server", "play.krosmocraft.fr", "--port", "56119")));

			assertTrue(cmd.stream().noneMatch(a -> a.contains("${")), () -> p + " unresolved: " + cmd);
			assertTrue(cmd.stream().noneMatch(a -> a.startsWith("\"")), "no hand quoting");
			assertTrue(cmd.containsAll(List.of("--token", "abc", "--server", "--port")));
			assertTrue(cmd.contains("--gameDir"));
			assertFalse(cmd.contains("--demo"), "feature-gated args must be dropped");
			assertEquals(p.os() == Platform.Os.MACOS, cmd.contains("-XstartOnFirstThread"), p + " startOnFirstThread");

			String cp = cmd.get(cmd.indexOf("-cp") + 1);
			String natives = switch (p.os()) {
				case WINDOWS -> "natives-windows";
				case MACOS -> "natives-macos";
				case LINUX -> "natives-linux";
			};
			assertTrue(cp.contains(natives), p + " lwjgl natives on classpath");
			for (String other : Set.of("natives-windows", "natives-macos", "natives-linux")) {
				if (!other.equals(natives)) {
					assertFalse(cp.contains(other), p + " must not carry " + other);
				}
			}
			assertTrue(cp.contains("fabric-loader-" + LOADER), "fabric loader on classpath");
			System.out.println(p + " -> " + cmd.size() + " args, main=" + cmd.get(cmd.indexOf(merged.mainClass())));
		}
	}
}
