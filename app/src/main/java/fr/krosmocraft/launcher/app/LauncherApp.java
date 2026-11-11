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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static fr.krosmocraft.launcher.app.ui.AbsolutePane.box;

public final class LauncherApp extends Application {

	private final ExecutorService worker = Executors.newCachedThreadPool(r -> {
		Thread t = new Thread(r, "launcher-worker");
		t.setDaemon(true);
		return t;
	});
	// One at a time: a cancelled sync fully stops before the next one starts
	private final ExecutorService downloads = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "launcher-downloads");
		t.setDaemon(true);
		return t;
	});
	private final Platform platform = Platform.current();

	private GameLayout layout;
	private LauncherSettings settings;
	private PasswordVault vault;
	private LaunchPipeline pipeline;
	private LauncherWindow window;

	private List<ServerEntry> servers = List.of(ServerEntry.LIVE_FALLBACK);
	private Task<LaunchPipeline.Prepared> prepareTask;
	private Task<Void> syncTask;
	private LaunchPipeline.Prepared prepared;
	private String preparedPresetId;
	private String progressText = "Préparation de la mise a jour...";
	private double progressValue;

	private LoginScreen loginScreen;
	private MainScreen mainScreen;
	private LoginScreen.Credentials credentials;
	private AuthResult.Success session;
	private ServerEntry sessionServer;
	// set while a login is running
	private ServerEntry loginServer;
	private Image skin;
	private ProfileInfo profile = ProfileInfo.EMPTY;
	private MediaPlayer music;

	@Override
	public void start(Stage stage) {
		layout = new GameLayout(dataDir());
		settings = LauncherSettings.load(layout.settingsFile());
		settings.syncKrosmoLanguage(layout.gameDir());
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

		run(ServerEntry::fetch, list -> {
			servers = list;
			if (loginServer == null && session == null) {
				window.setServers(servers, server(settings.server), this::serverChanged);
			}
		}, e -> { });
	}

	@Override
	public void stop() {
		worker.shutdownNow();
		downloads.shutdownNow();
	}

	// Downloads start only once logged in, for the committed server
	private void prepare() {
		cancelDownloads();
		if (mainScreen != null) {
			mainScreen.preparing();
			mainScreen.progress(progressText, progressValue);
		}
		ServerEntry server = sessionServer;
		Task<LaunchPipeline.Prepared> task = new ProgressTask<>(ProgressTask.FULL_UPDATE) {
			@Override
			protected LaunchPipeline.Prepared call() throws Exception {
				ClientManifest manifest = LaunchPipeline.fetchManifest(server);
				String presetId = settings.presetId != null ? settings.presetId : manifest.defaultPreset();
				preparedPresetId = presetId;
				return pipeline.prepare(manifest, presetId, Downloader.Verify.FAST, this);
			}
		};
		task.messageProperty().addListener((o, a, b) -> {
			if (task == prepareTask) {
				progress(b, progressValue);
			}
		});
		task.progressProperty().addListener((o, a, b) -> {
			if (task == prepareTask) {
				progress(progressText, b.doubleValue());
			}
		});
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
		downloads.submit(task);
	}

	// Interrupts running syncs and resets the download state
	private void cancelDownloads() {
		if (prepareTask != null) {
			prepareTask.cancel(true);
			prepareTask = null;
		}
		if (syncTask != null) {
			syncTask.cancel(true);
			syncTask = null;
		}
		prepared = null;
		preparedPresetId = null;
		progressText = "Préparation de la mise a jour...";
		progressValue = 0;
	}

	private void progress(String text, double value) {
		progressText = text == null ? progressText : text;
		progressValue = value;
		if (mainScreen != null && prepared == null) {
			mainScreen.progress(progressText, value);
		}
	}

	private abstract static class ProgressTask<T> extends Task<T> implements ProgressListener {
		static final List<String> FULL_UPDATE =
				List.of("install.versions", "install.libraries", "install.assets", "install.java", "install.mods");
		private static final Map<String, Double> WEIGHTS = Map.of("install.versions", 5.0, "install.libraries", 20.0,
				"install.assets", 35.0, "install.java", 25.0, "install.mods", 15.0);

		private final Map<String, double[]> slices = new HashMap<>();
		private double from;
		private double to;
		private double shown;

		ProgressTask(List<String> steps) {
			double total = steps.stream().mapToDouble(WEIGHTS::get).sum();
			double at = 0;
			for (String s : steps) {
				double end = at + WEIGHTS.get(s) / total;
				slices.put(s, new double[]{at, end});
				at = end;
			}
		}

		@Override
		public synchronized void step(String key) {
			updateMessage(Texts.step(key));
			double[] slice = slices.get(key);
			if (slice != null) {
				from = slice[0];
				to = slice[1];
				report(from);
			}
		}

		@Override
		public synchronized void progress(long done, long total) {
			double fraction = total <= 0 ? 1 : (double) done / total;
			report(from + (to - from) * Math.min(fraction, 1));
		}

		private void report(double value) {
			shown = Math.max(shown, value);
			updateProgress(shown, 1);
		}
	}

	// only reachable while logged out (selector locked otherwise)
	private void serverChanged(ServerEntry s) {
		settings.server = s.name();
		saveSettings();
	}

	private void useSession(ServerEntry server, AuthResult.Success s) {
		session = s;
		sessionServer = server;
		profile = ProfileInfo.EMPTY;
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
		ServerEntry server = currentServer();
		loginServer = server;
		window.setServerLocked(true);
		KrosmoAuthClient auth = new KrosmoAuthClient(server.authUrl());
		long start = System.nanoTime();
		run(() -> auth.login(c.username(), c.password()), result -> afterMinDelay(start, () -> onAuth(result)),
				e -> afterMinDelay(start, () -> loginFailed("Le serveur est hors ligne ou en maintenance, réessayer plus tard")));
	}

	// a login answered instantly feels fake: show the spinner at least this long
	private static final long LOGIN_MIN_MS = 800;

	private void afterMinDelay(long start, Runnable then) {
		long left = LOGIN_MIN_MS - (System.nanoTime() - start) / 1_000_000;
		if (left <= 0) {
			then.run();
			return;
		}
		PauseTransition p = new PauseTransition(Duration.millis(left));
		p.setOnFinished(e -> then.run());
		p.play();
	}

	private void loginFailed(String message) {
		loginServer = null;
		window.setServerLocked(false);
		loginScreen.failed();
		if (message != null) {
			error("Authentification échouée !", message, null);
		}
	}

	private void onAuth(AuthResult result) {
		switch (result) {
			case AuthResult.Success s -> {
				useSession(loginServer, s);
				loggedIn();
			}
			case AuthResult.NeedsPseudo n -> loggedIn();
			case AuthResult.WrongCredentials w -> loginFailed("Veuillez vérifier le nom de compte et le mot de passe saisi");
			case AuthResult.NotAllowed n -> loginFailed("Ce serveur est réservé à l'équipe de développement");
			case AuthResult.Unreachable u -> loginFailed("Le serveur est hors ligne ou en maintenance, réessayer plus tard");
			case AuthResult.PseudoRejected r -> loginFailed(null); // not sent by /login
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
			if (session == null) {
				phase2();
				return;
			}
			// let the success check show
			PauseTransition p = new PauseTransition(Duration.millis(1000));
			p.setOnFinished(e -> phase2());
			p.play();
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
		ServerEntry server = loginServer;
		KrosmoAuthClient auth = new KrosmoAuthClient(server.authUrl());
		PseudoOverlay[] overlay = new PseudoOverlay[1];
		overlay[0] = new PseudoOverlay(pseudo -> run(
				() -> auth.choosePseudo(credentials.username(), credentials.password(), pseudo),
				result -> {
					switch (result) {
						case AuthResult.Success s -> {
							useSession(server, s);
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
		loginServer = null;
		prepare();
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
			Task<Void> sync = new ProgressTask<>(List.of("install.mods")) {
				@Override
				protected Void call() throws Exception {
					pipeline.syncPresetFiles(current.manifest(), preset.id(), this);
					return null;
				}
			};
			sync.messageProperty().addListener((o, a, b) -> {
				if (sync == syncTask) {
					progress(b, progressValue);
				}
			});
			sync.progressProperty().addListener((o, a, b) -> {
				if (sync == syncTask) {
					progress(progressText, b.doubleValue());
				}
			});
			sync.setOnSucceeded(e -> {
				if (sync != syncTask) {
					return;
				}
				syncTask = null;
				preparedPresetId = preset.id();
				prepared = current;
				if (mainScreen != null) {
					mainScreen.ready();
				}
			});
			sync.setOnFailed(e -> {
				if (sync == syncTask) {
					error("Échec de la mise à jour !", "Vérifiez votre connexion Internet puis réessayez", this::prepare);
				}
			});
			syncTask = sync;
			downloads.submit(sync);
		}
	}

	private void play() {
		ServerEntry server = sessionServer;
		AuthResult.Success s = session;
		if (prepared == null || s == null) {
			return;
		}
		mainScreen.launching();
		LaunchPipeline.Prepared p = prepared;
		run(() -> pipeline.launch(p, server, s, settings), this::watchGame, e -> {
			mainScreen.launchFailed();
			error("Échec du lancement du jeu !", String.valueOf(e.getMessage()), null);
		});
	}

	private void watchGame(Process game) {
		// keep "Lancement du jeu..." on screen a moment, so closing doesn't look like a crash
		PauseTransition delay = new PauseTransition(Duration.millis(1500));
		delay.setOnFinished(e -> {
			if (!game.isAlive()) {
				return; // exit handler below takes over
			}
			window.stage().hide();
			if (music != null) {
				music.pause();
			}
		});
		delay.play();
		run(game::waitFor, code -> {
			delay.stop();
			if (code == 0) {
				javafx.application.Platform.exit();
				return;
			}
			mainScreen.launchFailed();
			settings.syncKrosmoLanguage(layout.gameDir());
			window.stage().show();
			startMusic();
			error("Échec du lancement du jeu !", "Minecraft s'est arrêté (code " + code + ")\nDétails : "
					+ layout.logsDir().resolve("game-stdout.log"), null);
		}, e -> javafx.application.Platform.exit());
	}

	// disconnect: stop downloads cleanly, forget the session, free the server choice
	private void logout() {
		cancelDownloads();
		session = null;
		sessionServer = null;
		loginServer = null;
		window.setServers(servers, server(settings.server), this::serverChanged);
		window.setServerLocked(false);
		credentials = null;
		skin = null;
		profile = ProfileInfo.EMPTY;
		window.overlayLayer().getChildren().clear();
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
		ServerEntry server = currentServer();
		run(() -> LaunchPipeline.fetchManifest(server), then, e -> then.accept(null));
	}

	private ServerEntry currentServer() {
		if (sessionServer != null) {
			return sessionServer;
		}
		if (loginServer != null) {
			return loginServer;
		}
		return window.selectedServer() != null ? window.selectedServer() : server(settings.server);
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
