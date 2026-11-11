package fr.krosmocraft.launcher.app;

import java.util.Set;

public final class Texts {

	private static final Set<String> STEPS = Set.of("install.versions", "install.libraries", "install.assets", "install.java", "install.mods");

	private Texts() {
	}

	public static String step(String key) {
		return Tr.t(STEPS.contains(key) ? key : "step.checking");
	}
}
