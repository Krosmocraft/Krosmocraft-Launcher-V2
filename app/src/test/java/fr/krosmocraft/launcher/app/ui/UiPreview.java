package fr.krosmocraft.launcher.app.ui;

import fr.krosmocraft.launcher.app.Tr;
import fr.krosmocraft.launcher.core.auth.ProfileInfo;
import fr.krosmocraft.launcher.core.config.LauncherSettings;
import fr.krosmocraft.launcher.core.preset.Preset;
import javafx.application.Platform;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public final class UiPreview {

	private static File out;

	public static void main(String[] args) throws Exception {
		out = new File(args.length > 0 ? args[0] : "build/ui-preview");
		String lang = System.getProperty("preview.lang", "fr_fr");
		Tr.setLanguage(lang);
		if (!lang.equals("fr_fr")) {
			out = new File(out, lang);
		}
		out.mkdirs();
		CountDownLatch started = new CountDownLatch(1);
		Platform.startup(started::countDown);
		started.await();

		List<Preset> presets = List.of(
				preset("performance", "Faible", Preset.FpsImpact.LOW, "low.png", "Graphismes réduits\n\nGarde tout de même l'aspect graphique général de Krosmocraft mais\nsacrifie de la qualité pour gagner en performance"),
				preset("normal", "Normal", Preset.FpsImpact.MEDIUM, "med.png", "Graphismes optimisés\n\nLe look de base de Krosmocraft en optimisant un maximum les\nparamètres pour garantir de bonnes performances"),
				preset("hd", "HD", Preset.FpsImpact.HIGH, "high.png", "Graphismes HD\n\nKrosmocraft, encore plus beau. Si vous avez confiance en votre PC, il\nest conseillé de choisir cette option, après tout, vous le valez bien ♥"));
		LauncherSettings settings = new LauncherSettings();
		settings.presetId = "normal";

		shoot("01-login", false, w -> w.screenLayer().place(new LoginScreen("SaintPepsi", "", false, c -> { }, u -> { }),
				g -> AbsolutePane.box(0, 0, g.w(), g.h())));
		shoot("02-main-preparing", false, w -> main(w, false));
		shoot("03-main-ready", false, w -> main(w, true));
		shoot("04-main-ready-maximized", true, w -> main(w, true));
		shoot("05-presets-first-launch", false, w -> new PresetOverlay(presets, p -> { }).open(w));
		for (String tab : new String[]{"account", "minecraft", "graphics", "java"}) {
			shoot("06-settings-" + tab.toLowerCase(), false, w -> {
				main(w, true);
				SettingsOverlay s = new SettingsOverlay(settings, "SaintPepsi", null, presets, new SettingsOverlay.Actions() {
					@Override
					public void save() {
					}

					@Override
					public void logout() {
					}

					@Override
					public void presetPicked(Preset preset, boolean reset) {
					}

					@Override
					public void languageChanged() {
					}
				});
				s.setProfile(new ProfileInfo("Fondateur", "12", "3400"));
				s.open(w);
				s.selectTab(tab);
			});
		}
		shoot("07-error", false, w -> new ErrorOverlay("Authentification échouée !",
				"Veuillez vérifier le nom de compte et le mot de passe saisi", null).open(w));
		shoot("08-pseudo", false, w -> new PseudoOverlay(p -> { }).open(w));
		Platform.exit();
	}

	private static void main(LauncherWindow w, boolean ready) {
		MainScreen m = new MainScreen("SaintPepsi", "42", "1.0.0", false, new MainScreen.Actions() {
			@Override
			public void play() {
			}

			@Override
			public void settings() {
			}

			@Override
			public void openUrl(String url) {
			}

			@Override
			public void muteChanged(boolean muted) {
			}
		});
		w.screenLayer().place(m, g -> AbsolutePane.box(0, 0, g.w(), g.h()));
		if (ready) {
			m.ready();
		} else {
			m.preparing();
			m.progress("Téléchargement des ressources...", 0.42);
		}
		m.show(false);
	}

	private static java.awt.image.BufferedImage toAwt(WritableImage img) {
		int w = (int) img.getWidth();
		int h = (int) img.getHeight();
		java.awt.image.BufferedImage b = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				b.setRGB(x, y, img.getPixelReader().getArgb(x, y));
			}
		}
		return b;
	}

	private static Preset preset(String id, String name, Preset.FpsImpact impact, String img, String desc) {
		return new Preset(id, 1, name, desc, impact, "builtin:" + img, List.of(), List.of(), Map.of(), 0);
	}

	private static void shoot(String name, boolean maximized, Consumer<LauncherWindow> setup) throws Exception {
		CountDownLatch built = new CountDownLatch(1);
		LauncherWindow[] window = new LauncherWindow[1];
		Platform.runLater(() -> {
			window[0] = new LauncherWindow(new Stage());
			if (maximized) {
				window[0].stage().setWidth(1920);
				window[0].stage().setHeight(1040);
			}
			window[0].show();
			setup.accept(window[0]);
			built.countDown();
		});
		built.await();
		Thread.sleep(1500); // wait for the fades
		CountDownLatch saved = new CountDownLatch(1);
		Platform.runLater(() -> {
			try {
				WritableImage img = window[0].stage().getScene().snapshot(null);
				ImageIO.write(toAwt(img), "png", new File(out, name + ".png"));
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				window[0].stage().close();
				saved.countDown();
			}
		});
		saved.await();
		System.out.println("wrote " + name);
	}
}
