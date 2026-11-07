package fr.krosmocraft.launcher.core.game;

import fr.krosmocraft.launcher.core.game.VersionProfile.Library;

public record LibraryArtifact(String path, String url, String sha1, long size) {

	private static final String DEFAULT_REPO = "https://libraries.minecraft.net/";

	public static LibraryArtifact of(Library lib) {
		if (lib.downloads() != null && lib.downloads().artifact() != null) {
			var a = lib.downloads().artifact();
			return new LibraryArtifact(a.path(), a.url(), a.sha1(), a.size());
		}
		String path = Maven.path(lib.name());
		String repo = lib.url() != null ? lib.url() : DEFAULT_REPO;
		if (!repo.endsWith("/")) {
			repo += "/";
		}
		return new LibraryArtifact(path, repo + path, lib.sha1(), lib.size() != null ? lib.size() : -1);
	}
}
