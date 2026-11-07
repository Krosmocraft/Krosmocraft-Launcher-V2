package fr.krosmocraft.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;
import java.util.stream.Stream;

final class LauncherUpdater {

	// base64 X.509 Ed25519 key, when set manifest.txt.sig is required
	private static final String PUBLIC_KEY = "MCowBQYDK2VwAyEAN0fgqvGjvv9j9gsPkrZNvl2YhvVOP00RjV9rQPsTMZE=";

	record ManifestFile(String name, String url, String sha1, long size) {
	}

	record Manifest(String version, String mainClass, int minBootstrap, List<ManifestFile> files) {
	}

	record Installed(String version, String mainClass, List<Path> jars) {
	}

	private static final HttpClient HTTP = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	private final Path dir;
	private final String manifestUrl;

	LauncherUpdater(Path dir, String manifestUrl) {
		this.dir = dir;
		this.manifestUrl = manifestUrl;
	}

	Installed update(int bootstrapVersion, DoubleConsumer progress) throws IOException {
		Path localManifest = dir.resolve("manifest.txt");
		byte[] remote;
		try {
			remote = get(manifestUrl);
			if (!PUBLIC_KEY.isEmpty()) {
				verifySignature(remote, get(manifestUrl + ".sig"));
			}
		} catch (IOException e) {
			if (Files.isRegularFile(localManifest)) {
				return installed(read(Files.readAllBytes(localManifest)));
			}
			throw e;
		}

		Manifest manifest = read(remote);
		if (manifest.minBootstrap() > bootstrapVersion) {
			throw new IOException("Cette version du launcher est trop ancienne : réinstallez-le depuis krosmocraft.fr");
		}

		List<ManifestFile> missing = new ArrayList<>();
		for (ManifestFile f : manifest.files()) {
			Path target = inside(f.name());
			if (!Files.isRegularFile(target) || Files.size(target) != f.size() || !f.sha1().equalsIgnoreCase(sha1(target))) {
				missing.add(f);
			}
		}
		long total = Math.max(1, missing.stream().mapToLong(ManifestFile::size).sum());
		long[] done = {0};
		Files.createDirectories(dir);
		for (ManifestFile f : missing) {
			download(f, inside(f.name()), n -> {
				done[0] += n;
				progress.accept(Math.min(1, (double) done[0] / total));
			});
		}

		// remove old jars
		Set<Path> keep = new HashSet<>();
		for (ManifestFile f : manifest.files()) {
			keep.add(inside(f.name()));
		}
		try (Stream<Path> files = Files.list(dir)) {
			for (Path p : files.filter(p -> p.toString().endsWith(".jar")).toList()) {
				if (!keep.contains(p.normalize())) {
					Files.deleteIfExists(p);
				}
			}
		}
		Files.write(localManifest, remote);
		return installed(manifest);
	}

	private Installed installed(Manifest m) throws IOException {
		List<Path> jars = new ArrayList<>();
		for (ManifestFile f : m.files()) {
			jars.add(inside(f.name()));
		}
		return new Installed(m.version(), m.mainClass(), jars);
	}

	private void download(ManifestFile f, Path target, LongConsumer bytesRead) throws IOException {
		Path tmp = target.resolveSibling(target.getFileName() + ".part");
		HttpRequest req = HttpRequest.newBuilder(URI.create(f.url())).timeout(Duration.ofMinutes(5))
				.header("User-Agent", "KrosmocraftBootstrap/" + Bootstrap.VERSION).build();
		try (InputStream in = send(req, HttpResponse.BodyHandlers.ofInputStream()).body();
			 OutputStream out = Files.newOutputStream(tmp)) {
			byte[] buf = new byte[64 * 1024];
			for (int n; (n = in.read(buf)) >= 0; ) {
				out.write(buf, 0, n);
				bytesRead.accept(n);
			}
		}
		if (!f.sha1().equalsIgnoreCase(sha1(tmp))) {
			Files.deleteIfExists(tmp);
			throw new IOException("Fichier corrompu : " + f.name());
		}
		Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	private Path inside(String name) throws IOException {
		Path p = dir.resolve(name).normalize();
		if (!p.getParent().equals(dir.normalize())) {
			throw new IOException("Bad file name in manifest: " + name);
		}
		return p;
	}

	static Manifest read(byte[] bytes) throws IOException {
		String version = null;
		String mainClass = null;
		int minBootstrap = 0;
		List<ManifestFile> files = new ArrayList<>();
		for (String line : new String(bytes, StandardCharsets.UTF_8).split("\\R")) {
			line = line.strip();
			int eq = line.indexOf('=');
			if (line.isEmpty() || line.startsWith("#") || eq < 0) {
				continue;
			}
			String value = line.substring(eq + 1).strip();
			switch (line.substring(0, eq).strip()) {
				case "version" -> version = value;
				case "mainClass" -> mainClass = value;
				case "minBootstrap" -> minBootstrap = Integer.parseInt(value);
				case "file" -> {
					String[] p = value.split("\\s+");
					if (p.length != 4) {
						throw new IOException("Bad manifest line: " + line);
					}
					files.add(new ManifestFile(p[0], p[3], p[1], Long.parseLong(p[2])));
				}
				default -> { }
			}
		}
		if (version == null || mainClass == null || files.isEmpty()) {
			throw new IOException("Incomplete launcher manifest");
		}
		return new Manifest(version, mainClass, minBootstrap, List.copyOf(files));
	}

	private static byte[] get(String url) throws IOException {
		HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15))
				.header("User-Agent", "KrosmocraftBootstrap/" + Bootstrap.VERSION).build();
		return send(req, HttpResponse.BodyHandlers.ofByteArray()).body();
	}

	private static <T> HttpResponse<T> send(HttpRequest req, HttpResponse.BodyHandler<T> handler) throws IOException {
		try {
			HttpResponse<T> r = HTTP.send(req, handler);
			if (r.statusCode() != 200) {
				throw new IOException("HTTP " + r.statusCode() + " : " + req.uri());
			}
			return r;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
	}

	private static void verifySignature(byte[] data, byte[] sigBase64) throws IOException {
		try {
			PublicKey key = KeyFactory.getInstance("Ed25519")
					.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY)));
			Signature s = Signature.getInstance("Ed25519");
			s.initVerify(key);
			s.update(data);
			if (!s.verify(Base64.getDecoder().decode(new String(sigBase64, StandardCharsets.US_ASCII).trim()))) {
				throw new IOException("Signature du launcher invalide");
			}
		} catch (java.security.GeneralSecurityException | IllegalArgumentException e) {
			throw new IOException("Signature du launcher invalide", e);
		}
	}

	static String sha1(Path file) throws IOException {
		try (InputStream in = Files.newInputStream(file)) {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] buf = new byte[64 * 1024];
			int n;
			while ((n = in.read(buf)) > 0) {
				md.update(buf, 0, n);
			}
			return HexFormat.of().formatHex(md.digest());
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
