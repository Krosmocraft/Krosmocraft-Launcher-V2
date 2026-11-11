package fr.krosmocraft.launcher.core.game;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LaunchGuard {
	public static final List<String> JAVA_ENV = List.of("JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS", "CLASSPATH");

	private static final List<String> FORBIDDEN = List.of("-javaagent", "-agentlib", "-agentpath", "-xbootclasspath",
			"-djava.system.class.loader", "--patch-module", "-xx:+enabledynamicagentloading", "-djdk.attach.allowattachself",
			"-xx:flags", "-xx:vmoptionsfile");

	private LaunchGuard() {
	}

	public static final class ForbiddenOptions extends IOException {
		private final List<String> problems;

		public ForbiddenOptions(List<String> problems) {
			super("Forbidden Java options: " + String.join(", ", problems));
			this.problems = List.copyOf(problems);
		}

		public List<String> problems() {
			return problems;
		}
	}

	public static List<String> problems() {
		List<String> out = new ArrayList<>();
		for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
			if (forbidden(arg) && !devDebugger(arg)) {
				out.add(arg);
			}
		}
		for (String name : JAVA_ENV) {
			String value = System.getenv(name);
			if (value != null && containsForbidden(value)) {
				out.add(name);
			}
		}
		return out;
	}

	public static void cleanEnvironment(Map<String, String> env) {
		JAVA_ENV.forEach(env::remove);
	}

	static boolean forbidden(String arg) {
		String a = arg.toLowerCase(Locale.ROOT);
		return FORBIDDEN.stream().anyMatch(a::startsWith);
	}

	private static boolean containsForbidden(String value) {
		for (String token : value.trim().split("\\s+")) {
			if (forbidden(token)) {
				return true;
			}
		}
		return false;
	}

	private static boolean devDebugger(String arg) {
		return arg.startsWith("-agentlib:jdwp") && !"krosmocraft-launcher".equals(LaunchGuard.class.getClassLoader().getName());
	}
}
