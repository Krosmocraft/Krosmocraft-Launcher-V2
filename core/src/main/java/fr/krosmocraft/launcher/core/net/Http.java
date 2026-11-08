package fr.krosmocraft.launcher.core.net;

import fr.krosmocraft.launcher.core.Json;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class Http {

	private static final String USER_AGENT = "KrosmocraftLauncher/2";

	private static final HttpClient CLIENT = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	private Http() {
	}

	public static HttpRequest.Builder request(String url) {
		return HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofSeconds(30))
				.header("User-Agent", USER_AGENT);
	}

	public static String getString(String url) throws IOException {
		return send(request(url).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
	}

	public static byte[] getBytes(String url) throws IOException {
		return send(request(url).GET().build(), HttpResponse.BodyHandlers.ofByteArray()).body();
	}

	public static <T> T getJson(String url, Class<T> type) throws IOException {
		return Json.GSON.fromJson(getString(url), type);
	}

	public static void download(String url, Path target) throws IOException {
		Files.createDirectories(target.getParent());
		Path tmp = target.resolveSibling(target.getFileName() + ".part");
		try {
			send(request(url).timeout(Duration.ofMinutes(5)).GET().build(), HttpResponse.BodyHandlers.ofFile(tmp));
		} catch (IOException e) {
			Files.deleteIfExists(tmp);
			throw e;
		}
		Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
	}

	public static <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
		try {
			HttpResponse<T> response = CLIENT.send(request, handler);
			if (response.statusCode() >= 400) {
				throw new IOException("HTTP " + response.statusCode() + " for " + request.uri());
			}
			return response;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted: " + request.uri(), e);
		}
	}

	public static <T> HttpResponse<T> sendRaw(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
		try {
			return CLIENT.send(request, handler);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted: " + request.uri(), e);
		}
	}
}
