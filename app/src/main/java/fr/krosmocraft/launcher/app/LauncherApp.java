package fr.krosmocraft.launcher.app;

import fr.krosmocraft.launcher.app.ui.ErrorOverlay;
import fr.krosmocraft.launcher.app.ui.Fx;
import fr.krosmocraft.launcher.app.ui.LauncherWindow;
import fr.krosmocraft.launcher.app.ui.LoginScreen;
import fr.krosmocraft.launcher.app.ui.MainScreen;
import fr.krosmocraft.launcher.app.ui.PresetOverlay;
import fr.krosmocraft.launcher.app.ui.PseudoOverlay;
import fr.krosmocraft.launcher.app.ui.SettingsOverlay;
import fr.krosmocraft.launcher.core.LaunchPipeline;
import fr.krosmocraft.launcher.core.auth.AuthResult;
import fr.krosmocraft.launcher.core.auth.KrosmoAuthClient;
import fr.krosmocraft.launcher.core.auth.ProfileInfo;
import fr.krosmocraft.launcher.core.config.LauncherSettings;
import fr.krosmocraft.launcher.core.config.PasswordVault;
import fr.krosmocraft.launcher.core.game.GameLayout;
import fr.krosmocraft.launcher.core.mods.ClientManifest;
import fr.krosmocraft.launcher.core.net.Downloader;
import fr.krosmocraft.launcher.core.net.Http;
import fr.krosmocraft.launcher.core.platform.Platform;
import fr.krosmocraft.launcher.core.preset.Preset;
import fr.krosmocraft.launcher.core.progress.ProgressListener;
import fr.krosmocraft.launcher.core.server.ServerEntry;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static fr.krosmocraft.launcher.app.ui.AbsolutePane.box;

public final class LauncherApp extends Application {

	private static final String AUTH_API = "http://launcher.krosmocraft.fr:24454";

	private final ExecutorService worker = Executors.newCachedThreadPool(r -> {
		Thread t = new Thread(r, "launcher-worker");
		t.setDaemon(true);
		return t;
	});
	private final Platform platform = Platform.current();
	private final KrosmoAuthClient auth = new KrosmoAuthClient(AUTH_API);

	private GameLayout layout;
	private LauncherSettings settings;
	private PasswordVault vault;
	private LaunchPipeline pipeline;
	private LauncherWindow window;

	private List<ServerEntry> servers = List.of(ServerEntry.LIVE_FALLBACK);
	private Task<LaunchPipeline.Prepared> prepareTask;
	private LaunchPipeline.Prepared prepared;
	private String preparedPresetId;
	private String progressText = "Préparation de la mise a jour...";
	private double progressValue;

	private LoginScreen loginScreen;
	private MainScreen mainScreen;
	private LoginScreen.Credentials credentials;
	private AuthResult.Success session;
	private Image skin;
	private ProfileInfo profile = ProfileInfo.EMPTY;
	private MediaPlayer music;

	@Override
	public void start(Stage stage) {
		layout = new GameLayout(dataDir());
		settings = LauncherSettings.load(layout.settingsFile());
		vault = new PasswordVault(layout.keyFile());
		pipeline = new LaunchPipeline(layout, platform);

		window = new LauncherWindow(stage);
		window.setServers(servers, ServerEntry.LIVE_FALLBACK, this::serverChanged);
		stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.ENTER && loginScreen != null && session == null
					&& window.overlayLayer().getChildren().isEmpty()) {
				loginScreen.submit();
			}
		});
		showLogin();
		window.show();

		run(() -> ServerEntry.fetch(LaunchPipeline.CDN + "servers.cfg"), list -> {
			servers = list;
			ServerEntry selected = server(settings.server);
			window.setServers(servers, selected, this::serverChanged);
			prepare();
		}, e -> prepare());
	}

	@Override
	public void stop() {
		worker.shutdownNow();
	}

	private void prepare() {
		if (prepareTask != null) {
			prepareTask.cancel(true);
		}
		prepared = null;
		progressText = "Préparation de la mise a jour...";
		progressValue = 0;
		if (mainScreen != null) {
			mainScreen.preparing();
		}
		ServerEntry server = window.selectedServer() != null ? window.selectedServer() : server(settings.server);
		Task<LaunchPipeline.Prepared> task = new ProgressTask<>() {
			@Override
			protected LaunchPipeline.Prepared call() throws Exception {
				ClientManifest manifest = LaunchPipeline.fetchManifest(server);
				String presetId = settings.presetId != null ? settings.presetId : manifest.defaultPreset();
				preparedPresetId = presetId;
				return pipeline.prepare(manifest, presetId, Downloader.Verify.FAST, this);
			}
		};
		task.messageProperty().addListener((o, a, b) -> progress(b, progressValue));
		task.progressProperty().addListener((o, a, b) -> progress(progressText, b.doubleValue()));
		task.setOnSucceeded(e -> {
			if (task != prepareTask) {
				return;
			}
			prepared = task.getValue();
			if (mainScreen != null) {
				mainScreen.ready();
			}
		});
		task.setOnFailed(e -> {
			if (task != prepareTask) {
				return;
			}
			System.err.println("Preparation failed: " + task.getException());
			if (task.getException() != null) {
				task.getException().printStackTrace();
			}
			if (mainScreen != null) {
				error("Échec de la mise à jour !", "Vérifiez votre connexion Internet puis réessayez", this::prepare);
			}
		});
		prepareTask = task;
		worker.submit(task);
	}

	private void progress(String text, double value) {
		progressText = text == null ? progressText : text;
		progressValue = value;
		if (mainScreen != null && prepared == null) {
			mainScreen.progress(progressText, value);
		}
	}

	private abstract static class ProgressTask<T> extends Task<T> implements ProgressListener {
		@Override
		public void step(String key) {
			updateMessage(Texts.step(key));
		}

		@Override
		public void progress(long done, long total) {
			updateProgress(done, Math.max(total, 1));
		}
	}

	private void serverChanged(ServerEntry s) {
		settings.server = s.name();
		saveSettings();
		prepare();
	}

	private void showLogin() {
		String savedPassword = settings.rememberMe ? vault.decrypt(settings.savedPassword) : "";
		loginScreen = new LoginScreen(settings.username, savedPassword, settings.rememberMe, this::login,
				url -> getHostServices().showDocument(url));
		window.screenLayer().place(loginScreen, g -> box(0, 0, g.w(), g.h()));
		// focus nothing so both prompts show
		javafx.application.Platform.runLater(loginScreen::requestFocus);
	}

	private void login(LoginScreen.Credentials c) {
		credentials = c;
		settings.username = c.username();
		settings.rememberMe = c.remember();
		saveSettings();
		run(() -> auth.login(c.username(), c.password()), this::onAuth, e -> {
			loginScreen.failed();
			error("Authentification échouée !", "Le serveur est hors ligne ou en maintenance, réessayer plus tard", null);
		});
	}

	private void onAuth(AuthResult result) {
		switch (result) {
			case AuthResult.Success s -> {
				session = s;
				loggedIn();
			}
			case AuthResult.NeedsPseudo n -> loggedIn();
			case AuthResult.WrongCredentials w -> {
				loginScreen.failed();
				error("Authentification échouée !", "Veuillez vérifier le nom de compte et le mot de passe saisi", null);
			}
			case AuthResult.Unreachable u -> {
				loginScreen.failed();
				error("Authentification échouée !", "Le serveur est hors ligne ou en maintenance, réessayer plus tard", null);
			}
			case AuthResult.PseudoRejected r -> loginScreen.failed(); // not sent by /login
		}
	}

	private void loggedIn() {
		loginScreen.succeeded();
		settings.savedPassword = credentials.remember() ? vault.encrypt(credentials.password()) : "";
		saveSettings();
		String user = credentials.username();
		run(() -> {
			byte[] png = Http.getBytes("https://krosmocraft.fr/skin/3d?user=" + URLEncoder.encode(user, StandardCharsets.UTF_8));
			return new Image(new ByteArrayInputStream(png));
		}, img -> {
			skin = img;
			phase2();
		}, e -> phase2());
	}

	private void phase2() {
		loginScreen.fadeOut();
		PauseTransition p = new PauseTransition(Duration.millis(session == null ? 500 : settings.presetId == null ? 600 : 800));
		p.setOnFinished(e -> afterLogin());
		p.play();
	}

	private void afterLogin() {
		if (session == null) {
			askPseudo();
			return;
		}
		run(() -> ProfileInfo.fetch(session.pseudo()), info -> profile = info, e -> { });
		if (settings.presetId == null) {
			withManifest(manifest -> {
				if (manifest == null || manifest.presets() == null || manifest.presets().isEmpty()) {
					showMain(true);
					return;
				}
				new PresetOverlay(manifest.presets(), preset -> {
					presetChosen(preset, false);
					showMain(true);
				}).open(window);
			});
			return;
		}
		showMain(true);
	}

	private void askPseudo() {
		PseudoOverlay[] overlay = new PseudoOverlay[1];
		overlay[0] = new PseudoOverlay(pseudo -> run(
				() -> auth.choosePseudo(credentials.username(), credentials.password(), pseudo),
				result -> {
					switch (result) {
						case AuthResult.Success s -> {
							session = s;
							overlay[0].close(400, this::afterLogin);
						}
						case AuthResult.PseudoRejected r -> {
							overlay[0].rejected();
							error("Pseudo invalide !", switch (r.reason()) {
								case BAD_SIZE -> "Votre pseudo doit comprendre entre 3 et 13 caractères";
								case INVALID -> "Le pseudo choisi est invalide";
								case TAKEN -> "Ce pseudo n'est pas disponible";
							}, null);
						}
						default -> {
							overlay[0].rejected();
							error("Authentification échouée !", "Le serveur est hors ligne ou en maintenance, réessayer plus tard", null);
						}
					}
				},
				e -> {
					overlay[0].rejected();
					error("Authentification échouée !", "Le serveur est hors ligne ou en maintenance, réessayer plus tard", null);
				}));
		overlay[0].open(window);
	}

	private void showMain(boolean animate) {
		window.screenLayer().remove(loginScreen);
		loginScreen = null;
		mainScreen = new MainScreen(session.pseudo(), session.playerCount(), session.version(), settings.muted,
				new MainScreen.Actions() {
					@Override
					public void play() {
						LauncherApp.this.play();
					}

					@Override
					public void settings() {
						openSettings();
					}

					@Override
					public void openUrl(String url) {
						getHostServices().showDocument(url);
					}

					@Override
					public void muteChanged(boolean muted) {
						settings.muted = muted;
						saveSettings();
						if (music != null) {
							music.setVolume(muted ? 0 : 0.5);
						}
					}
				});
		mainScreen.setSkin(skin);
		window.screenLayer().place(mainScreen, g -> box(0, 0, g.w(), g.h()));
		if (prepared != null) {
			mainScreen.ready();
		} else {
			mainScreen.preparing();
			mainScreen.progress(progressText, progressValue);
		}
		mainScreen.show(animate);
		startMusic();
	}

	private void startMusic() {
		if (music == null) {
			try {
				music = new MediaPlayer(new Media(Fx.res("mainmenu.mp3")));
				music.setCycleCount(MediaPlayer.INDEFINITE);
			} catch (RuntimeException e) {
				return;
			}
		}
		music.setVolume(settings.muted ? 0 : 0.5);
		music.play();
	}

	private void openSettings() {
		List<Preset> presets = prepared != null ? prepared.manifest().presets() : null;
		SettingsOverlay overlay = new SettingsOverlay(settings, session.pseudo(), skin, presets, new SettingsOverlay.Actions() {
			@Override
			public void save() {
				saveSettings();
			}

			@Override
			public void logout() {
				LauncherApp.this.logout();
			}

			@Override
			public void presetPicked(Preset preset, boolean reset) {
				presetChosen(preset, reset);
			}
		});
		overlay.setProfile(profile);
		if (profile == ProfileInfo.EMPTY) {
			run(() -> ProfileInfo.fetch(session.pseudo()), info -> {
				profile = info;
				overlay.setProfile(info);
			}, e -> { });
		}
		overlay.open(window);
	}

	private void presetChosen(Preset preset, boolean reset) {
		settings.presetId = preset.id();
		if (reset) {
			settings.appliedPreset = null;
		}
		if (preset.minMemoryMb() > settings.memoryMb) {
			settings.memoryMb = preset.minMemoryMb();
		}
		saveSettings();
		if (prepared != null && !preset.id().equals(preparedPresetId)) {
			LaunchPipeline.Prepared current = prepared;
			prepared = null;
			if (mainScreen != null) {
				mainScreen.preparing();
			}
			Task<Void> sync = new ProgressTask<>() {
				@Override
				protected Void call() throws Exception {
					pipeline.syncPresetFiles(current.manifest(), preset.id(), this);
					return null;
				}
			};
			sync.messageProperty().addListener((o, a, b) -> progress(b, progressValue));
			sync.progressProperty().addListener((o, a, b) -> progress(progressText, b.doubleValue()));
			sync.setOnSucceeded(e -> {
				preparedPresetId = preset.id();
				prepared = current;
				if (mainScreen != null) {
					mainScreen.ready();
				}
			});
			sync.setOnFailed(e -> error("Échec de la mise à jour !", "Vérifiez votre connexion Internet puis réessayez", this::prepare));
			worker.submit(sync);
		}
	}

	private void play() {
		if (prepared == null || session == null) {
			return;
		}
		mainScreen.launching();
		ServerEntry server = window.selectedServer() != null ? window.selectedServer() : server(settings.server);
		LaunchPipeline.Prepared p = prepared;
		run(() -> pipeline.launch(p, server, session, settings), this::watchGame, e -> {
			mainScreen.launchFailed();
			error("Échec du lancement du jeu !", String.valueOf(e.getMessage()), null);
		});
	}

	private void watchGame(Process game) {
		window.stage().hide();
		if (music != null) {
			music.pause();
		}
		run(game::waitFor, code -> {
			if (code == 0) {
				javafx.application.Platform.exit();
				return;
			}
			mainScreen.launchFailed();
			window.stage().show();
			startMusic();
			error("Échec du lancement du jeu !", "Minecraft s'est arrêté (code " + code + ")\nDétails : "
					+ layout.logsDir().resolve("game-stdout.log"), null);
		}, e -> javafx.application.Platform.exit());
	}

	private void logout() {
		session = null;
		credentials = null;
		if (music != null) {
			music.stop();
		}
		window.screenLayer().remove(mainScreen);
		mainScreen = null;
		showLogin();
	}

	private void withManifest(Consumer<ClientManifest> then) {
		if (prepared != null) {
			then.accept(prepared.manifest());
			return;
		}
		ServerEntry server = window.selectedServer() != null ? window.selectedServer() : server(settings.server);
		run(() -> LaunchPipeline.fetchManifest(server), then, e -> then.accept(null));
	}

	private void error(String title, String message, Runnable onClose) {
		new ErrorOverlay(title, message, onClose).open(window);
	}

	private <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
		Task<T> task = new Task<>() {
			@Override
			protected T call() throws Exception {
				return work.call();
			}
		};
		task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
		task.setOnFailed(e -> {
			Throwable t = task.getException();
			if (t != null) {
				t.printStackTrace();
			}
			onError.accept(t);
		});
		worker.submit(task);
	}

	private ServerEntry server(String name) {
		return servers.stream().filter(s -> s.name().equals(name)).findFirst().orElse(servers.getFirst());
	}

	private void saveSettings() {
		try {
			settings.save(layout.settingsFile());
		} catch (IOException e) {
			System.err.println("Could not save settings: " + e);
		}
	}

	private Path dataDir() {
		for (String arg : getParameters().getRaw()) {
			if (arg.startsWith("--data-dir=")) {
				return Path.of(arg.substring("--data-dir=".length()));
			}
		}
		String override = System.getenv("KROSMOCRAFT_HOME");
		return override != null && !override.isBlank() ? Path.of(override) : platform.dataDir("krosmocraft");
	}
}
