package fr.krosmocraft.launcher.core.auth;

import com.google.gson.JsonObject;
import fr.krosmocraft.launcher.core.Json;
import fr.krosmocraft.launcher.core.net.Http;

import javax.crypto.Cipher;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class KrosmoAuthClient {

	private static final String RSA = "RSA/ECB/PKCS1Padding"; // what Cipher.getInstance("RSA") resolved to in the old client

	private final String baseUrl;
	private KeyPair clientKeys;

	public KrosmoAuthClient(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public AuthResult login(String username, String password) {
		try {
			PublicKey server = handshake(username);
			HttpResponse<byte[]> r = post("/login", encrypt(server, username + "_PS_" + password));
			return switch (r.statusCode()) {
				case 200 -> parseSuccess(r.body());
				case 201 -> new AuthResult.NeedsPseudo();
				default -> new AuthResult.WrongCredentials();
			};
		} catch (IOException | GeneralSecurityException e) {
			return new AuthResult.Unreachable(e.getMessage());
		}
	}

	public AuthResult choosePseudo(String username, String password, String pseudo) {
		try {
			PublicKey server = handshake(username);
			HttpResponse<byte[]> r = post("/pseudo", encrypt(server, username + "_.=._" + password + "_.#._" + pseudo));
			if (r.statusCode() == 200) {
				return parseSuccess(r.body());
			}
			if (r.statusCode() == 201) {
				return switch (new String(r.body(), StandardCharsets.UTF_8).trim()) {
					case "1" -> new AuthResult.PseudoRejected(AuthResult.PseudoRejected.Reason.TAKEN);
					case "2" -> new AuthResult.PseudoRejected(AuthResult.PseudoRejected.Reason.INVALID);
					case "3" -> new AuthResult.PseudoRejected(AuthResult.PseudoRejected.Reason.BAD_SIZE);
					default -> new AuthResult.WrongCredentials();
				};
			}
			return new AuthResult.WrongCredentials();
		} catch (IOException | GeneralSecurityException e) {
			return new AuthResult.Unreachable(e.getMessage());
		}
	}

	private PublicKey handshake(String username) throws IOException, GeneralSecurityException {
		if (clientKeys == null) {
			KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
			gen.initialize(2048);
			clientKeys = gen.generateKeyPair();
		}
		String body = "{\"algorithm\":\"RSA\",\"encoded\":\""
				+ Base64.getEncoder().encodeToString(clientKeys.getPublic().getEncoded()) + "\"}_NAME_" + username;
		HttpResponse<byte[]> r = post("/handshake", body.getBytes(StandardCharsets.UTF_8));
		if (r.statusCode() != 200) {
			throw new IOException("Handshake HTTP " + r.statusCode());
		}
		JsonObject key = Json.GSON.fromJson(new String(r.body(), StandardCharsets.UTF_8), JsonObject.class);
		if (!"RSA".equals(key.get("algorithm").getAsString())) {
			throw new GeneralSecurityException("Unexpected server key algorithm");
		}
		byte[] encoded = Base64.getDecoder().decode(key.get("encoded").getAsString());
		return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
	}

	private AuthResult parseSuccess(byte[] encrypted) throws GeneralSecurityException {
		Cipher c = Cipher.getInstance(RSA);
		c.init(Cipher.DECRYPT_MODE, clientKeys.getPrivate());
		String[] parts = new String(c.doFinal(encrypted), StandardCharsets.UTF_8).split("_RES_");
		if (parts.length != 4) {
			return new AuthResult.WrongCredentials();
		}
		return new AuthResult.Success(parts[0], parts[1], parts[2], parts[3]);
	}

	private static byte[] encrypt(PublicKey key, String text) throws GeneralSecurityException {
		Cipher c = Cipher.getInstance(RSA);
		c.init(Cipher.ENCRYPT_MODE, key);
		return c.doFinal(text.getBytes(StandardCharsets.UTF_8));
	}

	private HttpResponse<byte[]> post(String path, byte[] body) throws IOException {
		HttpRequest req = Http.request(baseUrl + path)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofByteArray(body))
				.build();
		return Http.sendRaw(req, HttpResponse.BodyHandlers.ofByteArray());
	}
}
