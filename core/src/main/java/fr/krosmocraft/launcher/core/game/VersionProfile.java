package fr.krosmocraft.launcher.core.game;

import java.util.List;
import java.util.Map;

public record VersionProfile(
		String id,
		String inheritsFrom,
		String type,
		String mainClass,
		String assets,
		AssetIndexRef assetIndex,
		Downloads downloads,
		JavaVersion javaVersion,
		Arguments arguments,
		List<Library> libraries,
		Logging logging) {

	public List<Library> libraries() {
		return libraries != null ? libraries : List.of();
	}

	public String inheritsFromOrId() {
		return inheritsFrom != null ? inheritsFrom : id;
	}

	public record AssetIndexRef(String id, String sha1, long size, String url) {
	}

	public record Downloads(Download client) {
	}

	public record Download(String path, String sha1, long size, String url) {
	}

	public record JavaVersion(String component, int majorVersion) {
	}

	public record Arguments(List<Argument> game, List<Argument> jvm) {
		public List<Argument> game() {
			return game != null ? game : List.of();
		}

		public List<Argument> jvm() {
			return jvm != null ? jvm : List.of();
		}
	}

	public record Argument(List<String> values, List<Rule> rules) {
	}

	public record Rule(String action, OsRule os, Map<String, Boolean> features) {
	}

	public record OsRule(String name, String arch, String version) {
	}

	public record Library(String name, String url, LibraryDownloads downloads, List<Rule> rules, String sha1, Long size) {
	}

	public record LibraryDownloads(Download artifact) {
	}

	public record Logging(LoggingEntry client) {
	}

	public record LoggingEntry(String argument, LoggingFile file) {
	}

	public record LoggingFile(String id, String sha1, long size, String url) {
	}
}
