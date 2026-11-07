package fr.krosmocraft.launcher.core.config;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordVault {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final Path keyFile;

	public PasswordVault(Path keyFile) {
		this.keyFile = keyFile;
	}

	public String encrypt(String password) {
		try {
			byte[] iv = new byte[12];
			RANDOM.nextBytes(iv);
			Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
			c.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
			byte[] data = c.doFinal(password.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + data.length).put(iv).put(data).array());
		} catch (GeneralSecurityException | IOException e) {
			return "";
		}
	}

	public String decrypt(String stored) {
		if (stored == null || stored.isEmpty()) {
			return "";
		}
		try {
			byte[] all = Base64.getDecoder().decode(stored);
			Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
			c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, all, 0, 12));
			return new String(c.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
			return "";
		}
	}

	private SecretKeySpec key() throws IOException {
		if (!Files.isRegularFile(keyFile)) {
			byte[] k = new byte[32];
			RANDOM.nextBytes(k);
			Files.createDirectories(keyFile.getParent());
			Files.write(keyFile, k);
		}
		return new SecretKeySpec(Files.readAllBytes(keyFile), "AES");
	}
}
