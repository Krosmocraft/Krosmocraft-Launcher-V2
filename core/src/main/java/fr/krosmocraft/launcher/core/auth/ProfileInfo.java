package fr.krosmocraft.launcher.core.auth;

import fr.krosmocraft.launcher.core.net.Http;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public record ProfileInfo(String grade, String voteTokens, String ogrines) {

	public static final ProfileInfo EMPTY = new ProfileInfo("", "", "");

	public static ProfileInfo fetch(String pseudo) {
		try {
			String page = Http.getString("https://krosmocraft.fr/profile/" + URLEncoder.encode(pseudo, StandardCharsets.UTF_8));
			String text = page.replaceAll("<[^>]*>", "").replaceAll("\\s+", "");
			return new ProfileInfo(
					between(text, "Grade", "NombredeJetonde"),
					between(text, "NombredeJetondevotes", "JetondevotesNomb"),
					between(text, "Nombred'Ogrines", "OgrinesNombre"));
		} catch (IOException e) {
			return EMPTY;
		}
	}

	private static String between(String text, String start, String end) {
		int i = text.indexOf(start);
		if (i < 0) {
			return "";
		}
		String rest = text.substring(i + start.length());
		int j = rest.indexOf(end);
		return j < 0 ? rest : rest.substring(0, j);
	}
}
