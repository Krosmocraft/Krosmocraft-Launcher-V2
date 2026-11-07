package fr.krosmocraft.bootstrap;

import javax.swing.SwingUtilities;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

public final class Bootstrap {

	// bump when the bootstrap changes
	public static final int VERSION = 1;

	public static final String MANIFEST_URL = "https://krosmocraft.fr/downloads/launcher/manifest.txt";

	private Bootstrap() {
	}

	public static void main(String[] args) throws Exception {
		Path dataDir = DataDir.get("krosmocraft");
		redirectOutput(dataDir.resolve("logs").resolve("bootstrap.log"));
		System.out.println("Bootstrap " + VERSION + " on " + System.getProperty("os.name") + " "
				+ System.getProperty("os.arch") + ", java " + Runtime.version() + ", data " + dataDir);
		Splash splash = Splash.show();
		LauncherUpdater.Installed installed;
		try {
			installed = new LauncherUpdater(dataDir.resolve("launcher"), MANIFEST_URL)
					.update(VERSION, splash::progress);
			System.out.println("Starting launcher " + installed.version());
		} catch (Exception e) {
			e.printStackTrace();
			splash.fail("Impossible de mettre à jour le launcher.\n" + e.getMessage());
			return;
		}
		SwingUtilities.invokeAndWait(splash::close);
		try {
			start(installed, dataDir, args);
		} catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}

	private static void redirectOutput(Path log) {
		try {
			java.nio.file.Files.createDirectories(log.getParent());
			java.io.PrintStream out = new java.io.PrintStream(java.nio.file.Files.newOutputStream(log), true,
					java.nio.charset.StandardCharsets.UTF_8);
			System.setOut(out);
			System.setErr(out);
		} catch (java.io.IOException e) {
			// keep default streams
		}
	}

	private static void start(LauncherUpdater.Installed installed, Path dataDir, String[] args) throws Exception {
		List<Path> jars = installed.jars();
		URL[] urls = new URL[jars.size()];
		for (int i = 0; i < urls.length; i++) {
			urls[i] = jars.get(i).toUri().toURL();
		}
		ClassLoader loader = new URLClassLoader("krosmocraft-launcher", urls, Bootstrap.class.getClassLoader());
		Thread.currentThread().setContextClassLoader(loader);

		String[] launcherArgs = new String[args.length + 2];
		launcherArgs[0] = "--bootstrap-version=" + VERSION;
		launcherArgs[1] = "--data-dir=" + dataDir;
		System.arraycopy(args, 0, launcherArgs, 2, args.length);

		Class.forName(installed.mainClass(), true, loader)
				.getMethod("main", String[].class)
				.invoke(null, (Object) launcherArgs);
	}
}
