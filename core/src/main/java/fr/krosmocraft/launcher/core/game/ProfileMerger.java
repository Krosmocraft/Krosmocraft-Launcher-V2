package fr.krosmocraft.launcher.core.game;

import fr.krosmocraft.launcher.core.game.VersionProfile.Argument;
import fr.krosmocraft.launcher.core.game.VersionProfile.Arguments;
import fr.krosmocraft.launcher.core.game.VersionProfile.Library;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ProfileMerger {

	private ProfileMerger() {
	}

	public static VersionProfile merge(VersionProfile child, VersionProfile parent) {
		List<Library> libraries = new ArrayList<>(child.libraries());
		Set<String> childKeys = new HashSet<>();
		child.libraries().forEach(l -> childKeys.add(Maven.key(l.name())));
		for (Library lib : parent.libraries()) {
			if (!childKeys.contains(Maven.key(lib.name()))) {
				libraries.add(lib);
			}
		}

		return new VersionProfile(
				child.id(),
				parent.id(), // kept so the merged profile still knows which client jar to use
				or(child.type(), parent.type()),
				or(child.mainClass(), parent.mainClass()),
				or(child.assets(), parent.assets()),
				or(child.assetIndex(), parent.assetIndex()),
				or(child.downloads(), parent.downloads()),
				or(child.javaVersion(), parent.javaVersion()),
				new Arguments(
						concat(parent.arguments() != null ? parent.arguments().game() : null,
								child.arguments() != null ? child.arguments().game() : null),
						concat(parent.arguments() != null ? parent.arguments().jvm() : null,
								child.arguments() != null ? child.arguments().jvm() : null)),
				List.copyOf(libraries),
				or(child.logging(), parent.logging()));
	}

	private static <T> T or(T a, T b) {
		return a != null ? a : b;
	}

	private static List<Argument> concat(List<Argument> a, List<Argument> b) {
		List<Argument> out = new ArrayList<>();
		if (a != null) out.addAll(a);
		if (b != null) out.addAll(b);
		return List.copyOf(out);
	}
}
