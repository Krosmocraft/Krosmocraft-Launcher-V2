package fr.krosmocraft.launcher.core.game;

import fr.krosmocraft.launcher.core.game.VersionProfile.Argument;
import fr.krosmocraft.launcher.core.game.VersionProfile.Library;
import fr.krosmocraft.launcher.core.platform.Platform;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LaunchCommand {

	private static final Pattern VAR = Pattern.compile("\\$\\{([a-zA-Z_]+)}");

	public record Inputs(
			VersionProfile profile,
			GameLayout layout,
			Platform platform,
			Path javaExecutable,
			String playerName,
			String playerUuid,
			int memoryMb,
			List<String> userJvmArgs,
			int windowWidth,
			int windowHeight,
			List<String> extraGameArgs) {
	}

	private LaunchCommand() {
	}

	public static List<String> build(Inputs in) {
		VersionProfile p = in.profile();
		if (p.arguments() == null) {
			throw new IllegalStateException("Profile " + p.id() + " has no 'arguments' (pre-1.13 versions are not supported)");
		}
		Set<String> features = in.windowWidth() > 0 && in.windowHeight() > 0 ? Set.of("has_custom_resolution") : Set.of();
		Map<String, String> vars = variables(in);

		List<String> cmd = new ArrayList<>();
		cmd.add(in.javaExecutable().toString());
		cmd.add("-Xmx" + in.memoryMb() + "M");
		cmd.addAll(in.userJvmArgs());
		appendArgs(cmd, p.arguments().jvm(), in.platform(), features, vars);
		if (p.logging() != null && p.logging().client() != null) {
			Path config = in.layout().assetsDir().resolve("log_configs").resolve(p.logging().client().file().id());
			cmd.add(p.logging().client().argument().replace("${path}", config.toString()));
		}
		cmd.add(p.mainClass());
		appendArgs(cmd, p.arguments().game(), in.platform(), features, vars);
		cmd.addAll(in.extraGameArgs());
		return cmd;
	}

	public static String classpath(VersionProfile profile, GameLayout layout, Platform platform) {
		Set<String> entries = new LinkedHashSet<>();
		for (Library lib : profile.libraries()) {
			if (Rules.allowed(lib.rules(), platform, Set.of())) {
				entries.add(layout.librariesDir().resolve(LibraryArtifact.of(lib).path()).toString());
			}
		}
		entries.add(layout.clientJar(profile.inheritsFromOrId()).toString());
		return String.join(File.pathSeparator, entries);
	}

	private static Map<String, String> variables(Inputs in) {
		VersionProfile p = in.profile();
		GameLayout l = in.layout();
		Map<String, String> v = new HashMap<>();
		v.put("auth_player_name", in.playerName());
		v.put("auth_uuid", in.playerUuid());
		v.put("auth_access_token", "0");     // real auth is --token
		v.put("auth_xuid", "");             // empty = Optional.empty() since 26.3
		v.put("clientid", "");
		v.put("user_type", "msa");          // ignored since 26.3
		v.put("user_properties", "{}");
		v.put("version_name", p.id());
		v.put("version_type", "release");
		v.put("game_directory", l.gameDir().toString());
		v.put("assets_root", l.assetsDir().toString());
		v.put("game_assets", l.assetsDir().toString());
		v.put("assets_index_name", p.assetIndex() != null ? p.assetIndex().id() : p.assets());
		v.put("library_directory", l.librariesDir().toString());
		v.put("natives_directory", l.nativesDir(p.id()).toString());
		v.put("classpath_separator", File.pathSeparator);
		v.put("classpath", classpath(p, l, in.platform()));
		v.put("resolution_width", String.valueOf(in.windowWidth()));
		v.put("resolution_height", String.valueOf(in.windowHeight()));
		v.put("launcher_name", "krosmocraft-launcher");
		v.put("launcher_version", "2");
		return v;
	}

	private static void appendArgs(List<String> cmd, List<Argument> args, Platform platform, Set<String> features,
								   Map<String, String> vars) {
		for (Argument arg : args) {
			if (Rules.allowed(arg.rules(), platform, features)) {
				for (String value : arg.values()) {
					cmd.add(substitute(value, vars));
				}
			}
		}
	}

	static String substitute(String value, Map<String, String> vars) {
		Matcher m = VAR.matcher(value);
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
			String replacement = vars.getOrDefault(m.group(1), m.group());
			m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		m.appendTail(sb);
		return sb.toString();
	}
}
