package fr.krosmocraft.launcher.core.server;

import fr.krosmocraft.launcher.core.net.Http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ServerEntry(String name, String address, int port, String type, String authHost, int authPort) {

	// TODO per account list from the website
	public static final String LIST_URL = "https://krosmocraft.fr/downloads/next/servers.cfg";

	public static final ServerEntry LIVE_FALLBACK =
			new ServerEntry("Live", "play.krosmocraft.fr", 56119, "Live", "launcher.krosmocraft.fr", 24454);

	// live / build / beta: badge and list grouping
	public String category() {
		if ("Live".equalsIgnoreCase(type)) {
			return "live";
		}
		return "Build".equalsIgnoreCase(type) ? "build" : "beta";
	}

	// CDN channel: build servers run the live client files
	public String channel() {
		return category().equals("beta") ? "beta" : "live";
	}

	private int rank() {
		return switch (category()) {
			case "live" -> 0;
			case "build" -> 1;
			default -> 2;
		};
	}

	public String authUrl() {
		return "http://" + authHost + ":" + authPort;
	}

	public String key() {
		return address + ":" + port;
	}

	public static List<ServerEntry> fetch() {
		try {
			return parse(Http.getString(LIST_URL));
		} catch (IOException e) {
			// offline: Live only
			return List.of(LIVE_FALLBACK);
		}
	}

	// name/address/port/type/authPort
	public static List<ServerEntry> parse(String cfg) {
		List<ServerEntry> out = new ArrayList<>();
		out.add(LIVE_FALLBACK);
		Set<String> names = new HashSet<>(Set.of(LIVE_FALLBACK.name()));
		Set<String> keys = new HashSet<>(Set.of(LIVE_FALLBACK.key()));
		for (String line : cfg.lines().toList()) {
			String[] p = line.trim().split("/", -1);
			if (p.length != 5) {
				continue;
			}
			String name = p[0].trim();
			String address = p[1].trim();
			String type = p[3].trim();
			int port = port(p[2]);
			int authPort = port(p[4]);
			if (name.isEmpty() || address.isEmpty() || type.isEmpty() || port < 0 || authPort < 0) {
				continue;
			}
			ServerEntry s = new ServerEntry(name, address, port, type, address, authPort);
			if (names.add(name) && keys.add(s.key())) {
				out.add(s);
			}
		}
		// stable: Live, then build servers, then betas, file order kept inside each
		out.sort(Comparator.comparingInt(ServerEntry::rank));
		return List.copyOf(out);
	}

	private static int port(String s) {
		try {
			int p = Integer.parseInt(s.trim());
			return p > 0 && p <= 65535 ? p : -1;
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}
