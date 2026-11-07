package fr.krosmocraft.launcher.app.ui;

public record Geo(int w, int h) {

	public static final int BASE_W = 1067;
	public static final int BASE_H = 600;

	public boolean maximized() {
		return h > BASE_H + 1;
	}

	public int fs() {
		return maximized() ? 22 : 12;
	}

	public int fs2() {
		return maximized() ? 26 : 14;
	}

	public int r(int x) {
		return switch (x) {
			case 2 -> h / 300;
			case 3 -> h / 200;
			case 4 -> h / 135;
			case 5 -> w / 200;
			case 6 -> w / 170;
			case 7 -> w / 135;
			case 8 -> w / 130;
			case 9 -> w / 110;
			case 10 -> w / 100;
			default -> 0;
		};
	}

	public boolean small() {
		return r(10) == 10;
	}
}
