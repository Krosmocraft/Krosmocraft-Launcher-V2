package fr.krosmocraft.launcher.core.game;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchGuardTest {

	@Test
	void flagsCodeLoadingLauncherOptions() {
		assertTrue(LaunchGuard.forbidden("-javaagent:cheat.jar"));
		assertTrue(LaunchGuard.forbidden("-AgentPath:/x.dll"));
		assertTrue(LaunchGuard.forbidden("-XX:+EnableDynamicAgentLoading"));
		assertFalse(LaunchGuard.forbidden("-Xmx2G"));
		assertFalse(LaunchGuard.forbidden("--enable-native-access=ALL-UNNAMED,javafx.graphics,javafx.media"));
	}

	@Test
	void stripsJavaEnvFromTheGame() {
		Map<String, String> env = new HashMap<>(Map.of("JAVA_TOOL_OPTIONS", "-javaagent:x.jar", "_JAVA_OPTIONS", "x",
				"JDK_JAVA_OPTIONS", "x", "CLASSPATH", "x", "PATH", "keep"));
		LaunchGuard.cleanEnvironment(env);
		assertEquals(Map.of("PATH", "keep"), env);
	}
}
