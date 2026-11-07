package fr.krosmocraft.launcher.app.ui;

import fr.krosmocraft.launcher.core.auth.ProfileInfo;
import fr.krosmocraft.launcher.core.config.LauncherSettings;
import fr.krosmocraft.launcher.core.preset.Preset;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import static fr.krosmocraft.launcher.app.ui.AbsolutePane.box;

public final class SettingsOverlay extends Overlay {

	public interface Actions {
		void save();

		void logout();

		void presetPicked(Preset preset, boolean reset);
	}

	private static final String[] GAME_SIZES = {"854x480", "1024x768", "1280x1024", "1366x768", "1600x900", "1920x1080", "2560x1440"};

	private final LauncherSettings settings;
	private final Actions actions;
	private final String pseudo;
	private final Image skin;
	private final List<Preset> presets;
	private ProfileInfo profile = ProfileInfo.EMPTY;
	private final List<Label> menu = new ArrayList<>();
	private Label selected;
	private AbsolutePane tab;
	private boolean switching;
	private boolean ending;
	private Label grade;
	private Label votes;
	private Label ogrines;

	public SettingsOverlay(LauncherSettings settings, String pseudo, Image skin, List<Preset> presets, Actions actions) {
		this.settings = settings;
		this.actions = actions;
		this.pseudo = pseudo;
		this.skin = skin;
		this.presets = presets;

		Label title = appear(Fx.label("Settings"), 1);
		title.setAlignment(Pos.CENTER);
		place(title, g -> box(g.r(10) * 12, g.r(10) * 9, g.r(10) * 12, g.r(10) * 4));
		font(title, g -> Fx.regular(g.fs() * 2));

		Label compte = menuItem("Compte", g -> box(g.r(10) * 15, g.r(10) * 15, g.r(10) * 7, g.r(10) * 4), 1);
		menuItem("Minecraft", g -> box(g.r(10) * 15, g.r(10) * 19 - g.r(5), g.r(10) * 7, g.r(10) * 4), 0.4);
		menuItem("Graphisme", g -> box(g.r(10) * 15, g.r(10) * 22, g.r(10) * 9, g.r(10) * 4), 0.4);
		menuItem("Java", g -> box(g.r(10) * 15, g.r(10) * 25 + g.r(5), g.r(10) * 7, g.r(10) * 4), 0.4);
		Glow glow = new Glow(0);
		compte.setEffect(glow);
		Fx.anim(glow.levelProperty(), 1, 500);
		selected = compte;

		ImageView line = appear(Fx.view("grayline.png"), 0.5);
		place(line, g -> box(g.r(10) * 14, g.r(10) * 30 + g.r(5), g.r(10) * 6 + g.r(5), 1));

		Label done = appear(Fx.label("Fermer"), 0.8);
		done.setAlignment(Pos.CENTER_LEFT);
		place(done, g -> box(g.r(10) * 15, g.r(10) * 33 - g.r(5), g.r(10) * 8, g.r(10) * 4));
		font(done, g -> Fx.regular(g.fs()));
		done.setOnMouseEntered(e -> {
			if (!ending) {
				Glow gl = new Glow(0);
				done.setEffect(gl);
				Fx.anim(gl.levelProperty(), 1, 200);
				Fx.fade(done, 1, 200);
			}
		});
		done.setOnMouseExited(e -> {
			if (!ending) {
				Glow gl = new Glow(1);
				done.setEffect(gl);
				Fx.anim(gl.levelProperty(), 0, 200);
				Fx.fade(done, 0.8, 200);
			}
		});
		afterIntro(() -> done.setOnMouseClicked(e -> {
			if (!ending) {
				ending = true;
				actions.save();
				close(400, null);
			}
		}));

		showTab("Compte");
	}

	public void setProfile(ProfileInfo info) {
		profile = info;
		if (grade != null) {
			grade.setText(info.grade());
			votes.setText(info.voteTokens());
			ogrines.setText(info.ogrines());
		}
	}

	private Label menuItem(String text, Function<Geo, Box> rule, double opacity) {
		Label l = appear(Fx.label(text), opacity);
		l.setAlignment(Pos.CENTER_LEFT);
		place(l, rule);
		font(l, g -> Fx.regular(g.fs()));
		menu.add(l);
		l.setOnMouseEntered(e -> {
			if (!ending && selected != l) {
				Glow gl = new Glow(0);
				l.setEffect(gl);
				Fx.anim(gl.levelProperty(), 1, 200);
				Fx.fade(l, 0.6, 200);
			}
		});
		l.setOnMouseExited(e -> {
			if (!ending && selected != l && !switching) {
				Glow gl = new Glow(1);
				l.setEffect(gl);
				Fx.anim(gl.levelProperty(), 0, 200);
				Fx.fade(l, 0.4, 200);
			}
		});
		l.setOnMouseClicked(e -> switchTo(l));
		return l;
	}

	public void selectTab(String name) {
		menu.stream().filter(m -> m.getText().equals(name)).findFirst().ifPresent(this::switchTo);
	}

	private void switchTo(Label l) {
		if (ending || selected == l || switching) {
			return;
		}
		switching = true;
		Glow off = new Glow(1);
		selected.setEffect(off);
		Fx.anim(off.levelProperty(), 0, 200);
		Fx.fade(selected, 0.4, 200);
		Glow on = new Glow(1);
		l.setEffect(on);
		Fx.fade(l, 1, 200);
		selected = l;
		showTab(l.getText());
		PauseTransition p = new PauseTransition(Duration.millis(400));
		p.setOnFinished(ev -> {
			switching = false;
			menu.stream().filter(m -> m != selected).forEach(m -> m.setEffect(null));
		});
		p.play();
	}

	private void showTab(String name) {
		AbsolutePane old = tab;
		tab = new AbsolutePane();
		place(tab, g -> box(0, 0, g.w(), g.h()));
		Runnable build = switch (name) {
			case "Minecraft" -> this::minecraftTab;
			case "Graphisme" -> this::graphicsTab;
			case "Java" -> this::javaTab;
			default -> this::accountTab;
		};
		if (old == null) {
			build.run();
			return;
		}
		for (Node n : old.getChildren()) {
			n.setDisable(true);
			Fx.fade(n, 0, 400);
		}
		PauseTransition p = new PauseTransition(Duration.millis(200));
		p.setOnFinished(e -> {
			build.run();
			PauseTransition cleanup = new PauseTransition(Duration.millis(200));
			cleanup.setOnFinished(ev -> remove(old));
			cleanup.play();
		});
		p.play();
	}

	private void accountTab() {
		heading("Paramètres du compte", "Gérez votre compte", g -> g.r(10) * 9 - g.r(5), g -> g.fs() + g.r(4), 20);
		Label frame = new Label();
		frame.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
		frame.setStyle("-fx-background-color: black;");
		add(frame, g -> box(g.r(10) * 33 - g.r(5), g.r(10) * 18 + g.r(3), g.r(10) * 50, g.r(10) * 14), 0.4);
		ImageView image = new ImageView(skin);
		add(image, g -> box(g.r(10) * 35 - g.r(5), g.r(10) * 20 - g.r(5), g.r(10) * 6, g.r(10) * 12), 1);

		field("Pseudo", pseudo, g -> g.r(10) * 44 - g.r(5), g -> g.r(10) * 20 - g.r(5), 15, 15);
		grade = field("Grade", profile.grade(), g -> g.r(10) * 44 - g.r(5), g -> g.r(10) * 25 - g.r(5), 4, 15);
		votes = field("Jetons de vote", profile.voteTokens(), g -> g.r(10) * 63 - g.r(3), g -> g.r(10) * 20 - g.r(5), 15, 15);
		ogrines = field("Ogrines", profile.ogrines(), g -> g.r(10) * 63 - g.r(3), g -> g.r(10) * 25 - g.r(5), 10, 10);

		Label hitbox = new Label();
		hitbox.setStyle("-fx-background-color: black;");
		hitbox.setOpacity(0);
		tab.place(hitbox, g -> box(g.r(10) * 33 - g.r(5), g.r(10) * 19 - g.r(7), g.r(10) * 50, g.r(10) * 14));
		Label logout = Fx.label("Déconnexion");
		logout.setAlignment(Pos.CENTER);
		logout.setStyle("-fx-border-color: red; -fx-border-width: 2px; -fx-border-radius: 3; -fx-background-color: transparent;");
		logout.setTextFill(Color.RED);
		logout.setOpacity(0);
		tab.place(logout, g -> g.small()
				? box(g.w() / 2 + g.w() / 6, g.h() / 2 - g.h() / 40, g.w() / 10, g.h() / 20)
				: box(g.w() - g.w() / 4 - g.r(10) * 5 - g.r(5), g.h() / 2, g.w() / 10, g.h() / 20));
		tab.font(logout, g -> Fx.regular(g.fs()));
		boolean[] onButton = {false};
		hitbox.setOnMouseEntered(e -> {
			if (!ending && !onButton[0]) {
				Fx.fade(logout, 0.7, 200);
			}
		});
		hitbox.setOnMouseExited(e -> {
			if (!ending && !onButton[0]) {
				Fx.fade(logout, 0, 200);
			}
		});
		logout.setOnMouseEntered(e -> {
			if (!ending) {
				onButton[0] = true;
				Glow gl = new Glow(0);
				logout.setEffect(gl);
				Fx.anim(gl.levelProperty(), 1, 200);
				Fx.fade(logout, 0.9, 200);
			}
		});
		logout.setOnMouseExited(e -> {
			if (!ending) {
				onButton[0] = false;
				Glow gl = new Glow(1);
				logout.setEffect(gl);
				Fx.anim(gl.levelProperty(), 0, 200);
				Fx.fade(logout, 0.7, 200);
			}
		});
		logout.setOnMouseClicked(e -> {
			if (!ending) {
				ending = true;
				close(400, actions::logout);
			}
		});
	}

	private void minecraftTab() {
		heading("Paramètres Minecraft", "Options liées au lancement du jeu", g -> g.r(10) * 8 + g.r(5), g -> g.fs() + 4, 30);
		boldLabel("Résolution du mode fenêtré", g -> box(g.r(10) * 32 + g.r(5), g.r(10) * 17, g.r(10) * 20, g.r(10) * 4), 1);

		ComboBox<String> resolution = new ComboBox<>();
		resolution.getItems().addAll(GAME_SIZES);
		resolution.setValue(settings.gameSize);
		resolution.setVisibleRowCount(5);
		resolution.setBorder(new Border(new BorderStroke(Color.WHITE, BorderStrokeStyle.SOLID, new CornerRadii(4), BorderWidths.DEFAULT)));
		resolution.setStyle("-fx-background-color: black; -fx-text-fill: white;");
		resolution.setCellFactory(l -> resolutionCell(true));
		resolution.setButtonCell(resolutionCell(false));
		resolution.valueProperty().addListener((o, a, b) -> settings.gameSize = b);
		add(resolution, g -> box(g.r(10) * 32 + g.r(5), g.r(10) * 21 + g.r(5), g.r(10) * 13, g.r(10) * 3), 0.8);

		grayLine(g -> box(g.r(10) * 30 + g.r(2), g.r(10) * 27, g.r(10) * 50, 1));
		boldLabel("Démarrer la fenêtre Minecraft en plein écran", g -> box(g.r(10) * 32 + g.r(5), g.r(10) * 28, g.r(10) * 38, g.r(10) * 4), 1);
		toggle(settings.fullscreen, v -> settings.fullscreen = v, g -> g.r(10) * 32 + g.r(6));
		grayLine(g -> box(g.r(10) * 30 + g.r(2), g.r(10) * 37, g.r(10) * 50, 1));
		boldLabel("Mode plein écran sans bordures", g -> box(g.r(10) * 32 + g.r(5), g.r(10) * 38 - g.r(1), g.r(10) * 24, g.r(10) * 4), 1);
		toggle(settings.borderless, v -> settings.borderless = v, g -> g.r(10) * 42 + g.r(8));
	}

	private void graphicsTab() {
		heading("Paramètres graphiques", "Choisissez une option graphique adaptée", g -> g.r(10) * 8 + g.r(5), g -> g.fs() + 4, 35);
		if (presets == null || presets.isEmpty()) {
			Label none = Fx.label("Options graphiques indisponibles pour le moment.");
			add(none, g -> box(g.r(10) * 32 + g.r(5), g.r(10) * 18, g.r(10) * 50, g.r(10) * 4), 0.6);
			tab.font(none, g -> Fx.regular(g.fs()));
			return;
		}
		PresetGallery[] gallery = new PresetGallery[1];
		gallery[0] = new PresetGallery(tab, presets, settings.presetId, new PresetGallery.Slots() {
			@Override
			public double x(Geo g, int i, int n) {
				if (n <= 3) {
					return switch (i) {
						case 0 -> g.r(10) * 32 + g.r(5);
						case 1 -> g.r(10) * 50;
						default -> g.r(10) * 67 + g.r(5);
					};
				}
				double w = w(g, n);
				return g.r(10) * 32 + g.r(5) + i * (w + g.r(10) * 2.5);
			}

			@Override
			public double y(Geo g) {
				return g.r(10) * 18;
			}

			@Override
			public double w(Geo g, int n) {
				return n <= 3 ? g.r(10) * 15 : (g.r(10) * 50 - (n - 1) * g.r(10) * 2.5) / n;
			}

			@Override
			public double h(Geo g) {
				return g.r(10) * 10 + g.r(2);
			}
		}, g -> box(g.r(10) * 49 + g.r(6), g.r(10) * 30, g.r(10) * 15, g.r(10) * 4),
				g -> box(g.r(10) * 32 + g.r(2), g.r(10) * 32 + g.r(5), g.r(10) * 50, g.r(10) * 16),
				p -> {
					if (!p.id().equals(settings.presetId)) {
						gallery[0].select(p.id());
						actions.presetPicked(p, false);
					}
				});
		gallery[0].appear();

		Label reset = Fx.label("Réinitialiser mes réglages graphiques");
		reset.setAlignment(Pos.CENTER);
		tab.place(reset, g -> box(g.r(10) * 32 + g.r(2), g.r(10) * 48 + g.r(5), g.r(10) * 50, g.r(10) * 3));
		tab.font(reset, g -> Fx.regular(g.fs()));
		reset.setOpacity(0);
		Fx.fade(reset, 0.4, 500);
		PauseTransition p = new PauseTransition(Duration.millis(500));
		p.setOnFinished(e -> Fx.opacityHover(reset, 0.4, 0.9));
		p.play();
		reset.setOnMouseClicked(e -> presets.stream().filter(pr -> pr.id().equals(settings.presetId)).findFirst()
				.ifPresent(pr -> {
					actions.presetPicked(pr, true);
					reset.setText("Réglages réinitialisés au prochain lancement");
				}));
	}

	private void javaTab() {
		heading("Paramètres Java", "Gérez votre configuration Java (avancé)", g -> g.r(10) * 8 + g.r(5), g -> g.fs() + 4, 35);
		Label ramTitle = boldLabel("", g -> box(g.r(10) * 32 + g.r(5), g.r(10) * 16 + g.r(5), g.r(10) * 35, g.r(10) * 4), 1);
		tab.font(ramTitle, g -> Fx.bold(g.fs() + 3));

		Slider ram = new Slider(1, 8, Math.max(1, Math.min(8, Math.round(settings.memoryMb / 1024.0))));
		ram.setStyle("-fx-control-inner-background: rgba(46, 47, 48, 1);");
		ram.setBlockIncrement(1);
		ram.valueProperty().addListener((o, a, b) -> {
			long rounded = Math.round(b.doubleValue());
			if (rounded != b.doubleValue()) {
				ram.setValue(rounded);
				return;
			}
			settings.memoryMb = (int) rounded * 1024;
			ramTitle.setText("Ram allouée: " + (double) rounded + " Go");
		});
		ramTitle.setText("Ram allouée: " + ram.getValue() + " Go");
		add(ram, g -> box(g.r(10) * 32, g.r(10) * 22, g.r(10) * 20, AbsolutePane.PREF), 1);

		centered("RAM disponible", g -> box(g.r(10) * 55, g.r(10) * 18, g.r(10) * 20, g.r(10) * 4), g -> g.fs() + 2, 0.6);
		centered(bytes(physical(false)), g -> box(g.r(10) * 55, g.r(10) * 20, g.r(10) * 20, g.r(10) * 4), g -> g.fs() + 4, 1);
		centered("RAM total", g -> box(g.r(10) * 55 - g.r(2), g.r(10) * 23, g.r(10) * 20, g.r(10) * 4), g -> g.fs() + 2, 0.6);
		centered(bytes(physical(true)), g -> box(g.r(10) * 55, g.r(10) * 25, g.r(10) * 20, g.r(10) * 4), g -> g.fs() + 4, 1);

		Label rec1 = boldLabel("Il est recommendé d'allouer au minimum", g -> box(g.r(10) * 32 + g.r(5), g.r(10) * 24 + g.r(5), g.r(10) * 35, g.r(10) * 4), 1);
		tab.font(rec1, g -> Fx.bold(g.fs() - 1));
		Label rec2 = boldLabel("4 Go de RAM et à l'idéal 8 Go", g -> box(g.r(10) * 32 + g.r(5), g.r(10) * 26 + g.r(5), g.r(10) * 35, g.r(10) * 4), 1);
		tab.font(rec2, g -> Fx.bold(g.fs() - 1));
		grayLine(g -> box(g.r(10) * 30 + g.r(5), g.r(10) * 31 + g.r(8), g.r(10) * 50, 1));
		Label jvm = boldLabel("Arguments JVM", g -> box(g.r(10) * 32 + g.r(5), g.r(10) * 33, g.r(10) * 35, g.r(10) * 4), 1);
		tab.font(jvm, g -> Fx.bold(g.fs() + 2));

		TextField args = new TextField(settings.jvmArgs);
		args.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4); -fx-text-fill: white;");
		args.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, new CornerRadii(3), BorderWidths.DEFAULT)));
		args.textProperty().addListener((o, a, b) -> settings.jvmArgs = b);
		tab.place(args, g -> box(g.r(10) * 32 + g.r(4), g.r(10) * 38, g.r(10) * 25, g.r(10) * 3));
		tab.font(args, g -> Fx.regular(g.fs()));
	}

	private void heading(String title, String subtitle, java.util.function.ToIntFunction<Geo> titleY,
						 java.util.function.ToIntFunction<Geo> titleSize, int subtitleWidth) {
		Label t = Fx.label(title);
		t.setAlignment(Pos.CENTER_LEFT);
		add(t, g -> box(g.r(10) * 32 + g.r(5), titleY.applyAsInt(g), g.r(10) * 20, g.r(10) * 4), 1);
		tab.font(t, g -> Fx.bold(titleSize.applyAsInt(g)));
		Label s = Fx.label(subtitle);
		s.setAlignment(Pos.CENTER_LEFT);
		add(s, g -> box(g.r(10) * 32 + g.r(5), g.r(10) * 12, g.r(10) * subtitleWidth, g.r(10) * 4), 1);
		tab.font(s, g -> Fx.regular(g.fs()));
	}

	private Label field(String label, String value, java.util.function.ToIntFunction<Geo> x,
						java.util.function.ToIntFunction<Geo> y, int labelWidth, int valueWidth) {
		Label l = Fx.label(label);
		l.setAlignment(Pos.CENTER_LEFT);
		add(l, g -> box(x.applyAsInt(g), y.applyAsInt(g), g.r(10) * labelWidth, g.r(10) * 4), 0.5);
		tab.font(l, g -> Fx.bold(g.fs()));
		Label v = Fx.label(value);
		v.setAlignment(Pos.CENTER_LEFT);
		add(v, g -> box(x.applyAsInt(g), y.applyAsInt(g) + g.r(10) * 2, g.r(10) * valueWidth, g.r(10) * 4), 1);
		tab.font(v, g -> Fx.regular(g.fs()));
		return v;
	}

	private Label boldLabel(String text, Function<Geo, Box> rule, double opacity) {
		Label l = Fx.label(text);
		l.setAlignment(Pos.CENTER_LEFT);
		add(l, rule, opacity);
		tab.font(l, g -> Fx.bold(g.fs() + 1));
		return l;
	}

	private void centered(String text, Function<Geo, Box> rule, java.util.function.ToIntFunction<Geo> size, double opacity) {
		Label l = Fx.label(text);
		l.setAlignment(Pos.CENTER);
		add(l, rule, opacity);
		tab.font(l, g -> Fx.bold(size.applyAsInt(g)));
	}

	private void grayLine(Function<Geo, Box> rule) {
		add(Fx.view("grayline.png"), rule, 0.5);
	}

	private void toggle(boolean value, java.util.function.Consumer<Boolean> onChange, java.util.function.ToIntFunction<Geo> y) {
		ToggleSwitch t = new ToggleSwitch();
		t.selectedProperty().set(value);
		t.selectedProperty().addListener((o, a, b) -> onChange.accept(b));
		add(t, g -> box(g.small() ? g.r(10) * 30 + g.r(5) : g.r(10) * 31 + g.r(6), y.applyAsInt(g),
				AbsolutePane.PREF, AbsolutePane.PREF), 1);
		tab.onLayout(g -> t.setWindowWidth(g.w()));
	}

	private ListCell<String> resolutionCell(boolean filled) {
		return new ListCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				setText(item);
				setStyle(tab.geo().small() ? "-fx-font-size:13" : "-fx-font-size:25");
				if (filled) {
					setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
				}
				setTextFill(Color.WHITE);
			}
		};
	}

	private <N extends Node> void add(N node, Function<Geo, Box> rule, double opacity) {
		node.setOpacity(0);
		tab.place(node, rule);
		Fx.fade(node, opacity, 500);
	}

	private static long physical(boolean total) {
		if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
			return total ? os.getTotalMemorySize() : os.getFreeMemorySize();
		}
		return 0;
	}

	static String bytes(long bytes) {
		if (-1000 < bytes && bytes < 1000) {
			return bytes + " o";
		}
		String units = "kMGTPE";
		int i = 0;
		while (bytes <= -999_950 || bytes >= 999_950) {
			bytes /= 1000;
			i++;
		}
		return String.format(Locale.FRANCE, "%.1f %co", bytes / 1000.0, units.charAt(i));
	}
}
