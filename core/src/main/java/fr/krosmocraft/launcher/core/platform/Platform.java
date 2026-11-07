package fr.krosmocraft.launcher.core.platform;

import java.nio.file.Path;
import java.util.Locale;

public record Platform(Os os, Arch arch) {

	public enum Os {
		WINDOWS("windows"), MACOS("osx"), LINUX("linux");

		public final String mojangName;

		Os(String mojangName) {
			this.mojangName = mojangName;
		}
	}

	public enum Arch {
		X64("x86_64"), ARM64("arm64"), X86("x86");

		public final String mojangName;

		Arch(String mojangName) {
			this.mojangName = mojangName;
		}
	}

	public static Platform current() {
		String name = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
		Os os = name.contains("win") ? Os.WINDOWS : name.contains("mac") ? Os.MACOS : Os.LINUX;
		Arch a = switch (arch) {
			case "aarch64", "arm64" -> Arch.ARM64;
			case "x86", "i386", "i486", "i586", "i686" -> Arch.X86;
			default -> Arch.X64;
		};
		return new Platform(os, a);
	}

	public Path dataDir(String name) {
		String home = System.getProperty("user.home");
		return switch (os) {
			case WINDOWS -> {
				String appData = System.getenv("APPDATA");
				yield Path.of(appData != null ? appData : home, name);
			}
			case MACOS -> Path.of(home, "Library", "Application Support", name);
			case LINUX -> {
				String xdg = System.getenv("XDG_DATA_HOME");
				yield (xdg != null && !xdg.isBlank() ? Path.of(xdg) : Path.of(home, ".local", "share")).resolve(name);
			}
		};
	}

	public String javaRuntimeKey() {
		return switch (os) {
			case WINDOWS -> switch (arch) {
				case X64 -> "windows-x64";
				case ARM64 -> "windows-arm64";
				case X86 -> "windows-x86";
			};
			case MACOS -> arch == Arch.ARM64 ? "mac-os-arm64" : "mac-os";
			case LINUX -> arch == Arch.X86 ? "linux-i386" : "linux";
		};
	}

	public Path javaExecutable(Path runtimeHome) {
		return switch (os) {
			case WINDOWS -> runtimeHome.resolve("bin").resolve("javaw.exe");
			case MACOS -> runtimeHome.resolve("jre.bundle/Contents/Home/bin/java");
			case LINUX -> runtimeHome.resolve("bin").resolve("java");
		};
	}
}
