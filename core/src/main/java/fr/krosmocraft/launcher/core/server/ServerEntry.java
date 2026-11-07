package fr.krosmocraft.launcher.core.server;

import fr.krosmocraft.launcher.core.net.Http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public record ServerEntry(String name, String address, int port, String type) {

	public static final ServerEntry LIVE_FALLBACK = new ServerEntry("Live", "play.krosmocraft.fr", 56119, "Live");

	public String channel() {
		return "Live".equalsIgnoreCase(type) ? "live" : "beta";
	}

	public static List<ServerEntry> fetch(String url) {
		List<ServerEntry> out = new ArrayList<>();
		try {
			for (String line : Http.getString(url).lines().toList()) {
				String[] p = line.trim().split("/");
				if (p.length == 4) {
					try {
						out.add(new ServerEntry(p[0].trim(), p[1].trim(), Integer.parseInt(p[2].trim()), p[3].trim()));
					} catch (NumberFormatException ignored) {
						// bad port, skip
					}
				}
			}
		} catch (IOException ignored) {
		}
		if (out.isEmpty()) {
			out.add(LIVE_FALLBACK);
		}
		return List.copyOf(out);
	}
}
