package fr.krosmocraft.launcher.core.game;

public final class Maven {

	private Maven() {
	}

	public static String path(String coordinate) {
		String[] p = coordinate.split(":");
		if (p.length < 3) {
			throw new IllegalArgumentException("Bad maven coordinate: " + coordinate);
		}
		String file = p[1] + "-" + p[2] + (p.length > 3 ? "-" + p[3] : "") + ".jar";
		return p[0].replace('.', '/') + "/" + p[1] + "/" + p[2] + "/" + file;
	}

	public static String key(String coordinate) {
		String[] p = coordinate.split(":");
		return p[0] + ":" + p[1] + (p.length > 3 ? ":" + p[3] : "");
	}
}
