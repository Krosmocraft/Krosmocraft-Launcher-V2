package fr.krosmocraft.launcher.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

/** Launcher texts in the Krosmocraft language, French as fallback. */
public final class Tr {

	private static final String SOURCE = "fr_fr";
	private static final Properties FALLBACK = load(SOURCE);

	private static Properties texts = FALLBACK;
	private static String code = SOURCE;

	private Tr() {
	}

	public static void setLanguage(String languageCode) {
		code = languageCode;
		texts = languageCode.equals(SOURCE) ? FALLBACK : load(languageCode);
	}

	public static String code() {
		return code;
	}

	public static Locale locale() {
		return switch (code) {
			case "fr_fr" -> Locale.FRANCE;
			case "es_es" -> Locale.of("es", "ES");
			default -> Locale.US;
		};
	}

	public static String t(String key, Object... args) {
		String text = texts.getProperty(key, FALLBACK.getProperty(key, key));
		return args.length == 0 ? text : String.format(locale(), text, args);
	}

	// For texts that already arrive in French: our translation if this language has one, else the text as is
	public static String or(String key, String french) {
		return texts != FALLBACK && texts.containsKey(key) ? texts.getProperty(key) : french;
	}

	private static Properties load(String languageCode) {
		Properties p = new Properties();
		try (InputStream in = Tr.class.getResourceAsStream("lang/" + languageCode + ".properties")) {
			if (in != null) {
				p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
			}
		} catch (IOException e) {
			System.err.println("Could not load launcher texts " + languageCode + ": " + e);
		}
		return p;
	}
}
