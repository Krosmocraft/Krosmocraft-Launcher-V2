package fr.krosmocraft.launcher.core;

import fr.krosmocraft.launcher.core.game.Maven;
import fr.krosmocraft.launcher.core.game.Rules;
import fr.krosmocraft.launcher.core.game.VersionProfile;
import fr.krosmocraft.launcher.core.mods.FileSync;
import fr.krosmocraft.launcher.core.platform.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreUnitTest {

	static final Platform WIN = new Platform(Platform.Os.WINDOWS, Platform.Arch.X64);
	static final Platform MAC_ARM = new Platform(Platform.Os.MACOS, Platform.Arch.ARM64);

	@Test
	void rulesLastMatchWins() {
		var allowAll = new VersionProfile.Rule("allow", null, null);
		var denyMac = new VersionProfile.Rule("disallow", new VersionProfile.OsRule("osx", null, null), null);
		assertTrue(Rules.allowed(List.of(allowAll, denyMac), WIN, Set.of()));
		assertFalse(Rules.allowed(List.of(allowAll, denyMac), MAC_ARM, Set.of()));
		assertTrue(Rules.allowed(List.of(), MAC_ARM, Set.of()));
	}

	@Test
	void featureRules() {
		var demo = new VersionProfile.Rule("allow", null, Map.of("is_demo_user", true));
		assertFalse(Rules.allowed(List.of(demo), WIN, Set.of()));
		assertTrue(Rules.allowed(List.of(demo), WIN, Set.of("is_demo_user")));
	}

	@Test
	void mavenPaths() {
		assertEquals("net/fabricmc/fabric-loader/0.20.1/fabric-loader-0.20.1.jar", Maven.path("net.fabricmc:fabric-loader:0.20.1"));
		assertEquals("org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3-natives-macos-arm64.jar", Maven.path("org.lwjgl:lwjgl:3.3.3:natives-macos-arm64"));
		assertEquals(Maven.key("org.ow2.asm:asm:9.6"), Maven.key("org.ow2.asm:asm:9.8"));
	}

	@Test
	void manifestCannotEscapeGameDir(@TempDir Path dir) {
		assertThrows(IOException.class, () -> FileSync.resolveInside(dir, "../evil.jar"));
		assertThrows(IOException.class, () -> FileSync.resolveInside(dir, "mods/../../evil.jar"));
	}

	@Test
	void optionsMergeKeepsPlayerKeys(@TempDir Path dir) throws Exception {
		Path options = dir.resolve("options.txt");
		Files.writeString(options, "version:4000\nfov:0.5\nrenderDistance:32\n");
		var m = Class.forName("fr.krosmocraft.launcher.core.preset.PresetApplier").getDeclaredMethod("mergeOptions", Path.class, Map.class);
		m.setAccessible(true);
		m.invoke(null, options, Map.of("renderDistance", "10", "maxFps", "120"));
		assertEquals(List.of("version:4000", "fov:0.5", "renderDistance:10", "maxFps:120"), Files.readAllLines(options));
	}
}
