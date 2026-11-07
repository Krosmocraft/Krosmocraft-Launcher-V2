package fr.krosmocraft.launcher.app.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public final class LoginView extends VBox {

	public record Credentials(String username, String password, boolean remember) {
	}

	private final TextField username = new TextField();
	private final PasswordField password = new PasswordField();
	private final CheckBox remember = new CheckBox("Se souvenir de moi");
	private final Label error = new Label();
	private final BooleanProperty busy = new SimpleBooleanProperty();

	public LoginView(String savedUsername, boolean rememberMe, Consumer<Credentials> onLogin, Consumer<String> openUrl) {
		getStyleClass().add("login");
		setAlignment(Pos.CENTER);

		ImageView logo = new ImageView(new Image(LauncherWindow.res("logo.png")));
		logo.setPreserveRatio(true);
		logo.fitHeightProperty().bind(heightProperty().multiply(0.3));

		username.setPromptText("Nom d'utilisateur");
		username.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().length() <= 16 ? c : null));
		username.setText(savedUsername);
		password.setPromptText("Mot de passe");
		remember.setSelected(rememberMe);
		error.getStyleClass().add("error");
		error.setWrapText(true);

		Button login = new Button("Connexion");
		login.getStyleClass().add("primary");
		login.setDefaultButton(true);
		login.disableProperty().bind(busy.or(username.textProperty().isEmpty()).or(password.textProperty().isEmpty()));
		login.textProperty().bind(javafx.beans.binding.Bindings.when(busy).then("Connexion…").otherwise("Connexion"));
		login.setOnAction(e -> {
			error.setText("");
			onLogin.accept(new Credentials(username.getText().trim(), password.getText(), remember.isSelected()));
		});

		Hyperlink register = new Hyperlink("Créer un compte");
		register.setOnAction(e -> openUrl.accept("https://krosmocraft.fr/register/"));
		Hyperlink forgot = new Hyperlink("Mot de passe oublié ?");
		forgot.setOnAction(e -> openUrl.accept("https://krosmocraft.fr/resetting/request"));
		HBox links = new HBox(register, forgot);
		links.getStyleClass().add("links");
		links.setAlignment(Pos.CENTER);

		VBox form = new VBox(username, password, remember, login, error, links);
		form.getStyleClass().add("form");
		form.setMaxWidth(USE_PREF_SIZE);
		getChildren().addAll(logo, form);

		if (!savedUsername.isEmpty()) {
			password.requestFocus();
		}
	}

	public BooleanProperty busyProperty() {
		return busy;
	}

	public void showError(String message) {
		error.setText(message);
		password.clear();
		password.requestFocus();
	}
}
