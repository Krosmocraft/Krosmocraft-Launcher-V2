package fr.krosmocraft.launcher.core.net;

import fr.krosmocraft.launcher.core.progress.ProgressListener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class Downloader {

	public enum Verify {
		FAST,
		FULL
	}

	private static final int PARALLEL = 8;
	private static final int RETRIES = 3;

	private Downloader() {
	}

	public static void run(List<FileDownload> files, Verify verify, ProgressListener progress) throws IOException {
		List<FileDownload> todo = new ArrayList<>();
		for (FileDownload f : files) {
			if (!isValid(f, verify)) {
				todo.add(f);
			}
		}
		long total = todo.stream().mapToLong(f -> Math.max(f.size(), 0)).sum();
		AtomicLong done = new AtomicLong();
		progress.progress(0, total);

		Semaphore permits = new Semaphore(PARALLEL);
		ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
		try {
			List<Future<?>> futures = new ArrayList<>();
			for (FileDownload f : todo) {
				futures.add(pool.submit(() -> {
					permits.acquire();
					try {
						fetch(f);
						progress.progress(done.addAndGet(Math.max(f.size(), 0)), total);
					} finally {
						permits.release();
					}
					return null;
				}));
			}
			for (Future<?> future : futures) {
				future.get();
			}
		} catch (ExecutionException e) {
			throw e.getCause() instanceof IOException io ? io : new IOException(e.getCause());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Download cancelled", e);
		} finally {
			stop(pool);
		}
	}

	private static void stop(ExecutorService pool) {
		pool.shutdownNow();
		boolean interrupted = Thread.interrupted();
		try {
			for (int i = 0; i < 3; i++) {
				try {
					if (pool.awaitTermination(10, TimeUnit.SECONDS)) {
						return;
					}
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private static void fetch(FileDownload f) throws IOException {
		IOException last = null;
		for (int attempt = 1; attempt <= RETRIES; attempt++) {
			try {
				Http.download(f.url(), f.target());
				if (isValid(f, Verify.FULL)) {
					return;
				}
				last = new IOException("Checksum mismatch: " + f.url());
			} catch (IOException e) {
				last = e;
			}
		}
		throw last;
	}

	static boolean isValid(FileDownload f, Verify verify) throws IOException {
		Path p = f.target();
		if (!Files.isRegularFile(p)) {
			return false;
		}
		if (f.size() >= 0 && Files.size(p) != f.size()) {
			return false;
		}
		if (verify == Verify.FULL && f.sha1() != null) {
			return f.sha1().equalsIgnoreCase(sha1(p));
		}
		return true;
	}

	public static String sha1(Path file) throws IOException {
		try (InputStream in = Files.newInputStream(file)) {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] buf = new byte[64 * 1024];
			int n;
			while ((n = in.read(buf)) > 0) {
				md.update(buf, 0, n);
			}
			return HexFormat.of().formatHex(md.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
