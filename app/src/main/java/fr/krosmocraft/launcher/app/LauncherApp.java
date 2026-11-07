package fr.krosmocraft.launcher.app;

import fr.krosmocraft.launcher.app.ui.HomeView;
import fr.krosmocraft.launcher.app.ui.LauncherWindow;
import fr.krosmocraft.launcher.app.ui.LoginView;
import fr.krosmocraft.launcher.app.ui.PresetPickerView;
import fr.krosmocraft.launcher.core.LaunchPipeline;
import fr.krosmocraft.launcher.core.auth.AuthResult;
import fr.krosmocraft.launcher.core.auth.KrosmoAuthClient;
import fr.krosmocraft.launcher.core.config.LauncherSettings;
import fr.krosmocraft.launcher.core.game.GameLayout;
import fr.krosmocraft.launcher.core.mods.ClientManifest;
import fr.krosmocraft.launcher.core.net.Downloader;
import fr.krosmocraft.launcher.core.platform.Platform;
import fr.krosmocraft.launcher.core.preset.Preset;
import fr.krosmocraft.launcher.core.progress.ProgressListener;
import fr.krosmocraft.launcher.core.server.ServerEntry;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

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
	private LauncherWindow window;
	private List<ServerEntry> servers = List.of(ServerEntry.LIVE_FALLBACK);
	private ClientManifest manifest;
	private AuthResult.Success session;
	private LoginView.Credentials credentials;
	private HomeView home;

	@Override
	public void start(Stage stage) {
		layout = new GameLayout(dataDir());
		settings = LauncherSettings.load(layout.settingsFile());

		window = new LauncherWindow(stage);
		window.playBackground(settings.muted);
		showLogin();
		window.show();

		run(() -> ServerEntry.fetch(LaunchPipeline.CDN + "servers.cfg"), list -> servers = list, e -> { });
	}

	@Override
	public void stop() {
		worker.shutdownNow();
	}

	private void showLogin() {
		LoginView view = new LoginView(settings.rememberMe ? settings.username : "", settings.rememberMe,
				this::login, url -> getHostServices().showDocument(url));
		window.setContent(view);
	}

	private void login(LoginView.Credentials c) {
		LoginView view = (LoginView) currentContent();
		view.busyProperty().set(true);
		credentials = c;
		run(() -> auth.login(c.username(), c.password()), result -> {
			view.busyProperty().set(false);
			handleAuth(result, view);
		}, e -> {
			view.busyProperty().set(false);
			view.showError("Erreur : " + e.getMessage());
		});
	}

	private void handleAuth(AuthResult result, LoginView view) {
		switch (result) {
			case AuthResult.Success s -> onLoggedIn(s);
			case AuthResult.NeedsPseudo n -> askPseudo(view, null);
			case AuthResult.PseudoRejected r -> askPseudo(view, switch (r.reason()) {
				case TAKEN -> "Ce pseudo est déjà pris.";
				case INVALID -> "Ce pseudo contient des caractères interdits.";
				case BAD_SIZE -> "Le pseudo doit faire entre 3 et 16 caractères.";
			});
			case AuthResult.WrongCredentials w -> view.showError("Identifiants incorrects.");
			case AuthResult.Unreachable u -> view.showError("Serveur d'authentification injoignable. Réessayez plus tard.");
		}
	}

	private void askPseudo(LoginView view, String error) {
		TextInputDialog dialog = new TextInputDialog();
		dialog.initOwner(window.stage());
		dialog.setTitle("Krosmocraft");
		dialog.setHeaderText(error != null ? error : "Bienvenue ! Choisissez votre pseudo en jeu (définitif).");
		dialog.showAndWait().map(String::trim).filter(s -> !s.isEmpty()).ifPresent(pseudo -> {
			view.busyProperty().set(true);
			run(() -> auth.choosePseudo(credentials.username(), credentials.password(), pseudo), r -> {
				view.busyProperty().set(false);
				handleAuth(r, view);
			}, e -> view.busyProperty().set(false));
		});
	}

	private void onLoggedIn(AuthResult.Success s) {
		session = s;
		settings.rememberMe = credentials.remember();
		settings.username = credentials.remember() ? credentials.username() : "";
		saveSettings();
		ServerEntry server = serverByName(settings.server);
		run(() -> LaunchPipeline.fetchManifest(server), m -> {
			manifest = m;
			if (settings.presetId == null) {
				showPresets(true);
			} else {
				showHome();
			}
		}, e -> showHome());
	}

	private void showHome() {
		home = new HomeView(session.pseudo(), session.playerCount(), servers, settings.server,
				this::play, () -> showPresets(false), () -> {
					session = null;
					showLogin();
				});
		window.setContent(home);
	}

	private void showPresets(boolean firstLaunch) {
		if (manifest == null) {
			alert(Alert.AlertType.WARNING, "Impossible de récupérer la liste des graphismes.", null);
			return;
		}
		window.setContent(new PresetPickerView(manifest.presets(), settings.presetId, firstLaunch, (preset, reset) -> {
			settings.presetId = preset.id();
			if (reset) {
				settings.appliedPreset = null;
			}
			saveSettings();
			showHome();
		}, this::showHome));
	}

	private void play() {
		ServerEntry server = home.selectedServer();
		settings.server = server.name();
		saveSettings();

		Task<Process> task = new Task<>() {
			@Override
			protected Process call() throws Exception {
				ClientManifest m = LaunchPipeline.fetchManifest(server);
				if (settings.presetId == null) {
					settings.presetId = m.defaultPreset();
				}
				ProgressListener listener = new ProgressListener() {
					@Override
					public void step(String key) {
						updateMessage(Texts.step(key));
						updateProgress(-1, 1);
					}

					@Override
					public void progress(long done, long total) {
						updateProgress(done, Math.max(total, 1));
					}
				};
				return new LaunchPipeline(layout, platform).prepareAndLaunch(m, server, session, settings, false,
						Downloader.Verify.FAST, listener);
			}
		};
		home.busyProperty().set(true);
		home.progressBar().progressProperty().bind(task.progressProperty());
		home.statusLabel().textProperty().bind(task.messageProperty());
		task.setOnSucceeded(e -> watchGame(task.getValue()));
		task.setOnFailed(e -> {
			home.busyProperty().set(false);
			alert(Alert.AlertType.ERROR, "La préparation du jeu a échoué.", String.valueOf(task.getException()));
		});
		worker.submit(task);
	}

	private void watchGame(Process game) {
		window.stage().hide();
		run(game::waitFor, code -> {
			if (code == 0) {
				javafx.application.Platform.exit();
				return;
			}
			home.busyProperty().set(false);
			window.stage().show();
			alert(Alert.AlertType.ERROR, "Minecraft s'est arrêté (code " + code + ").", tail(layout.logsDir().resolve("game-stdout.log")));
		}, e -> javafx.application.Platform.exit());
	}

	private <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
		Task<T> task = new Task<>() {
			@Override
			protected T call() throws Exception {
				return work.call();
			}
		};
		task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
		task.setOnFailed(e -> onError.accept(task.getException()));
		worker.submit(task);
	}

	private javafx.scene.Node currentContent() {
		return ((javafx.scene.layout.BorderPane) ((javafx.scene.layout.StackPane) window.stage().getScene().getRoot())
				.getChildren().getLast()).getCenter();
	}

	private ServerEntry serverByName(String name) {
		return servers.stream().filter(s -> s.name().equals(name)).findFirst().orElse(servers.getFirst());
	}

	private void saveSettings() {
		try {
			settings.save(layout.settingsFile());
		} catch (IOException ignored) {
		}
	}

	private void alert(Alert.AlertType type, String header, String details) {
		Alert a = new Alert(type);
		a.initOwner(window.stage());
		a.setTitle("Krosmocraft");
		a.setHeaderText(header);
		if (details != null) {
			TextArea area = new TextArea(details);
			area.setEditable(false);
			area.setWrapText(true);
			a.getDialogPane().setExpandableContent(area);
			a.getDialogPane().setExpanded(true);
		}
		a.show();
	}

	private static String tail(Path log) {
		try {
			List<String> lines = Files.readAllLines(log);
			return String.join("\n", lines.subList(Math.max(0, lines.size() - 60), lines.size()));
		} catch (IOException e) {
			return "(log illisible : " + log + ")";
		}
	}

	private Path dataDir() {
		for (String arg : getParameters().getRaw()) {
			if (arg.startsWith("--data-dir=")) {
				return Path.of(arg.substring("--data-dir=".length()));
			}
		}
		return platform.dataDir("krosmocraft");
	}
}
