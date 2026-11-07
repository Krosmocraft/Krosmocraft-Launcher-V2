package fr.krosmocraft.launcher.core.game;

import fr.krosmocraft.launcher.core.game.VersionProfile.OsRule;
import fr.krosmocraft.launcher.core.game.VersionProfile.Rule;
import fr.krosmocraft.launcher.core.platform.Platform;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class Rules {

	private Rules() {
	}

	public static boolean allowed(List<Rule> rules, Platform platform, Set<String> features) {
		if (rules == null || rules.isEmpty()) {
			return true;
		}
		boolean allowed = false;
		for (Rule rule : rules) {
			if (matches(rule, platform, features)) {
				allowed = "allow".equals(rule.action());
			}
		}
		return allowed;
	}

	private static boolean matches(Rule rule, Platform platform, Set<String> features) {
		OsRule os = rule.os();
		if (os != null) {
			if (os.name() != null && !os.name().equals(platform.os().mojangName)) {
				return false;
			}
			if (os.arch() != null && !os.arch().equals(platform.arch().mojangName)) {
				return false;
			}
			if (os.version() != null && !Pattern.compile(os.version()).matcher(System.getProperty("os.version")).find()) {
				return false;
			}
		}
		if (rule.features() != null) {
			for (Map.Entry<String, Boolean> f : rule.features().entrySet()) {
				if (features.contains(f.getKey()) != f.getValue()) {
					return false;
				}
			}
		}
		return true;
	}
}
