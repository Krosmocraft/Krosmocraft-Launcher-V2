package fr.krosmocraft.launcher.app.ui;

import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.text.Font;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Pos;

import java.util.List;
import java.util.function.Consumer;

import static fr.krosmocraft.launcher.app.ui.AbsolutePane.box;

public final class LoginScreen extends AbsolutePane {

	public record Credentials(String username, String password, boolean remember) {
	}

	private final Rectangle blur = new Rectangle();
	private final ImageView logo = Fx.view("logo.png");
	private final TextField username = new TextField();
	private final PasswordField password = new PasswordField();
	private final Label rememberMe = Fx.label("Se souvenir de moi");
	private final AnimatedCheckBox rememberBox;
	private final Label forgot = Fx.label("Mot de passe oublié");
	private final Label register = Fx.label("Inscription");
	private final ImageView spinner = Fx.view("loading.png");
	private final ImageView checked = new ImageView();
	private final Button login = new Button("Se connecter >");
	private final Consumer<Credentials> onLogin;
	private Timeline spinAnimation;
	private boolean busy;

	public LoginScreen(String savedUser, String savedPassword, boolean remember, Consumer<Credentials> onLogin,
					   Consumer<String> openUrl) {
		this.onLogin = onLogin;

		blur.setFill(Color.rgb(10, 10, 10, 0.9));
		place(blur, g -> box(0, 0, g.w(), g.h()));
		place(logo, g -> box(g.w() / 2 - g.w() / 14, g.h() / 12 + g.h() / 21, g.w() / 8 + g.w() / 50, g.h() / 5 + g.h() / 12));

		username.setTextFormatter(limit(16));
		username.setPromptText("Nom de compte");
		username.setText(savedUser);
		field(username, g -> box(g.w() / 2 - g.w() / 7, g.h() / 2 - g.h() / 10 + g.h() / 21 - g.h() / 100,
				g.w() / 4 + g.w() / 28, g.h() / 12));
		password.setTextFormatter(limit(Integer.MAX_VALUE));
		password.setPromptText("Mot de passe");
		password.setText(savedPassword);
		field(password, g -> box(g.w() / 2 - g.w() / 7, g.h() / 2 + g.h() / 21 - g.h() / 100,
				g.w() / 4 + g.w() / 28, g.h() / 12));
		// comfortaa has no dot glyph
		font(password, g -> password.getText().isEmpty() ? Fx.regular(g.fs2()) : Font.font("Arial", g.fs2()));
		password.textProperty().addListener((o, a, b) -> {
			if (a.isEmpty() != b.isEmpty()) {
				requestLayout();
			}
		});

		rememberMe.setAlignment(Pos.CENTER_LEFT);
		rememberMe.setOpacity(0.4);
		place(rememberMe, g -> box(g.w() / 3 + g.w() / 18, g.h() / 2 + g.h() / 9 + g.h() / 23 - g.h() / 100, g.w() / 8, g.h() / 20));
		font(rememberMe, g -> Fx.regular(g.fs()));
		rememberBox = new AnimatedCheckBox(remember, rememberMe);
		rememberMe.setOnMouseEntered(e -> {
			if (!rememberBox.checkedProperty().get()) {
				Fx.fade(rememberMe, 0.9, 200);
			}
		});
		rememberMe.setOnMouseExited(e -> {
			if (!rememberBox.checkedProperty().get()) {
				Fx.fade(rememberMe, 0.4, 200);
			}
		});
		rememberMe.setOnMouseClicked(e -> rememberBox.toggle(rememberMe, true));
		place(rememberBox, g -> box(g.w() / 3 + g.w() / 39, g.h() / 2 + g.h() / 9 + g.h() / 20 - g.h() / 100, g.w() / 50, g.w() / 50));
		onLayout(g -> rememberBox.setBoxSize(g.w() / 50));

		forgot.setAlignment(Pos.CENTER_RIGHT);
		link(forgot, () -> openUrl.accept("https://krosmocraft.fr/resetting/request"),
				g -> box(g.w() / 2 + g.w() / 70, g.h() / 2 + g.h() / 9 + g.h() / 10 - g.h() / 100, g.w() / 8, g.h() / 20));
		link(register, () -> openUrl.accept("https://krosmocraft.fr/register/"),
				g -> box(g.w() / 3 + g.w() / 39, g.h() / 2 + g.h() / 9 + g.h() / 10 - g.h() / 100, g.w() / 15, g.h() / 20));

		spinner.setOpacity(0);
		place(spinner, g -> box(g.w() / 2 - g.h() / 44, g.w() / 2 - g.h() / 17, g.h() / 24, g.h() / 24));

		login.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
		place(login, g -> box(g.w() / 2 - g.w() / 7, g.h() / 2 + g.h() / 6 + g.h() / 12 - g.h() / 100,
				g.w() / 4 + g.w() / 28, g.h() / 10));
		font(login, g -> Fx.regular(g.fs() * 2));
		login.setOnMouseEntered(e -> {
			if (valid()) {
				Glow glow = new Glow(0);
				login.setEffect(glow);
				Fx.fade(login, 0.9, 200);
				Fx.anim(glow.levelProperty(), 0.9, 200);
			}
		});
		login.setOnMouseExited(e -> {
			if (valid()) {
				Glow glow = new Glow(0.9);
				login.setEffect(glow);
				Fx.anim(glow.levelProperty(), 0, 200);
				Fx.fade(login, 0.9, 200);
			}
		});
		login.setOnAction(e -> submit());
		username.textProperty().addListener((o, a, b) -> refreshButton());
		password.textProperty().addListener((o, a, b) -> refreshButton());
		refreshButton();
	}

	public void submit() {
		if (valid() && !busy) {
			setBusy(true);
			onLogin.accept(new Credentials(username.getText(), password.getText(), rememberBox.checkedProperty().get()));
		}
	}

	private boolean valid() {
		return username.getText().length() >= 3 && password.getText().length() >= 6;
	}

	private void refreshButton() {
		login.setOpacity(valid() ? 0.9 : 0.2);
	}

	private void setBusy(boolean b) {
		busy = b;
		login.setText(b ? "Connexion..." : "Se connecter >");
		username.setDisable(b);
		password.setDisable(b);
		username.setOpacity(b ? 0.7 : 1);
		password.setOpacity(b ? 0.7 : 1);
		spinner.setOpacity(b ? 1 : 0);
		if (b && spinAnimation == null) {
			spinAnimation = Fx.spin(spinner);
		}
	}

	public void failed() {
		setBusy(false);
	}

	public void succeeded() {
		spinner.setOpacity(0);
		login.setText("Succès !");
		Geo g = geo();
		checked.setImage(new Image(Fx.res("checked.gif"), g.h() / 20, g.h() / 20, true, true));
		place(checked, gg -> box(gg.w() / 2 - gg.h() / 37, gg.h() * 0.825, gg.h() / 20, gg.h() / 20));
	}

	public void fadeOut() {
		for (Node n : List.copyOf(getChildren())) {
			n.setDisable(true);
			Fx.fade(n, 0, 700);
		}
	}

	public String username() {
		return username.getText();
	}

	private static TextFormatter<String> limit(int max) {
		return new TextFormatter<>(c -> {
			if (c.getText().length() > 1) {
				// paste, drop spaces and cut what doesn't fit
				String text = c.getText().strip();
				int room = max - (c.getControlText().length() - (c.getRangeEnd() - c.getRangeStart()));
				c.setText(text.substring(0, Math.max(0, Math.min(text.length(), room))));
			}
			return c.getControlNewText().length() <= max ? c : null;
		});
	}

	private void field(TextInputControl f, java.util.function.Function<Geo, Box> rule) {
		f.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4); -fx-text-fill: white;");
		f.setOnMouseEntered(e -> {
			if (!f.isDisabled()) {
				f.setOpacity(0.8);
			}
		});
		f.setOnMouseExited(e -> {
			if (!f.isDisabled()) {
				f.setOpacity(1);
			}
		});
		place(f, rule);
		font(f, g -> Fx.regular(g.fs2()));
	}

	private void link(Label l, Runnable action, java.util.function.Function<Geo, Box> rule) {
		Fx.opacityHover(l, 0.4, 0.9);
		l.setOnMouseClicked(e -> action.run());
		place(l, rule);
		font(l, g -> Fx.regular(g.fs()));
	}
}
