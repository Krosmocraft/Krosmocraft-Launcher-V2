package fr.krosmocraft.launcher.app.ui;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

public final class LauncherWindow {

	public static final double WIDTH = 1067;
	public static final double HEIGHT = 600;
	private static final double BASE_FONT = 13;

	private final Stage stage;
	private final StackPane root = new StackPane();
	private final BorderPane frame = new BorderPane();
	private final BooleanProperty maximized = new SimpleBooleanProperty();
	private Rectangle2D restoreBounds;
	private MediaPlayer backgroundPlayer;

	public LauncherWindow(Stage stage) {
		this.stage = stage;
		boolean transparent = Platform.isSupported(ConditionalFeature.TRANSPARENT_WINDOW);
		stage.initStyle(transparent ? StageStyle.TRANSPARENT : StageStyle.UNDECORATED);
		stage.setTitle("Krosmocraft");
		stage.getIcons().add(new Image(res("favicon.png")));
		stage.setMinWidth(WIDTH);
		stage.setMinHeight(HEIGHT);

		root.getStyleClass().add("window");
		root.getChildren().add(frame);
		frame.setTop(titleBar());

		Rectangle clip = new Rectangle();
		clip.widthProperty().bind(root.widthProperty());
		clip.heightProperty().bind(root.heightProperty());
		clip.arcWidthProperty().bind(Bindings.when(maximized).then(0).otherwise(18));
		clip.arcHeightProperty().bind(clip.arcWidthProperty());
		root.setClip(clip);
		maximized.addListener((obs, was, now) -> root.pseudoClassStateChanged(
				javafx.css.PseudoClass.getPseudoClass("maximized"), now));

		root.styleProperty().bind(Bindings.createStringBinding(() -> {
			double h = stage.getHeight();
			double size = (Double.isNaN(h) || h <= 0 ? HEIGHT : h) / HEIGHT * BASE_FONT;
			return String.format(java.util.Locale.ROOT, "-fx-font-size: %.2fpx;", size);
		}, stage.heightProperty()));

		Scene scene = new Scene(root, WIDTH, HEIGHT);
		scene.setFill(transparent ? Color.TRANSPARENT : Color.BLACK);
		scene.getStylesheets().add(res("launcher.css"));
		javafx.scene.text.Font.loadFont(res("Comfortaa-Regular.ttf"), 12);
		javafx.scene.text.Font.loadFont(res("Comfortaa-Bold.ttf"), 12);
		stage.setScene(scene);
		stage.centerOnScreen();
	}

	public void show() {
		stage.show();
	}

	public Stage stage() {
		return stage;
	}

	public void setContent(Node content) {
		frame.setCenter(content);
	}

	public void playBackground(boolean muted) {
		try {
			backgroundPlayer = new MediaPlayer(new Media(res("background.mp4")));
			backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
			backgroundPlayer.setMute(muted);
			MediaView view = new MediaView(backgroundPlayer);
			view.setPreserveRatio(false);
			view.fitWidthProperty().bind(root.widthProperty());
			view.fitHeightProperty().bind(root.heightProperty());
			root.getChildren().addFirst(view);
			backgroundPlayer.play();
		} catch (RuntimeException e) {
		}
	}

	public void setMuted(boolean muted) {
		if (backgroundPlayer != null) {
			backgroundPlayer.setMute(muted);
		}
	}

	public void toggleMaximized() {
		if (maximized.get()) {
			stage.setX(restoreBounds.getMinX());
			stage.setY(restoreBounds.getMinY());
			stage.setWidth(restoreBounds.getWidth());
			stage.setHeight(restoreBounds.getHeight());
			maximized.set(false);
		} else {
			restoreBounds = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
			Screen screen = Screen.getScreensForRectangle(restoreBounds).stream().findFirst().orElse(Screen.getPrimary());
			Rectangle2D b = screen.getVisualBounds();
			stage.setX(b.getMinX());
			stage.setY(b.getMinY());
			stage.setWidth(b.getWidth());
			stage.setHeight(b.getHeight());
			maximized.set(true);
		}
	}

	private Node titleBar() {
		Label title = new Label("Krosmocraft");
		title.getStyleClass().add("title");
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		Button min = chromeButton("reduce.png", () -> stage.setIconified(true));
		Button max = chromeButton("maxi.png", this::toggleMaximized);
		Button close = chromeButton("close.png", Platform::exit);
		close.getStyleClass().add("close");
		HBox bar = new HBox(title, spacer, min, max, close);
		bar.getStyleClass().add("title-bar");

		double[] offset = new double[2];
		bar.setOnMousePressed(e -> {
			offset[0] = stage.getX() - e.getScreenX();
			offset[1] = stage.getY() - e.getScreenY();
		});
		bar.setOnMouseDragged(e -> {
			if (!maximized.get()) {
				stage.setX(e.getScreenX() + offset[0]);
				stage.setY(e.getScreenY() + offset[1]);
			}
		});
		bar.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2) {
				toggleMaximized();
			}
		});
		return bar;
	}

	private static Button chromeButton(String icon, Runnable action) {
		javafx.scene.image.ImageView view = new javafx.scene.image.ImageView(new Image(res(icon)));
		view.setFitHeight(12);
		view.setPreserveRatio(true);
		Button b = new Button(null, view);
		b.getStyleClass().add("chrome-button");
		b.setOnAction(e -> action.run());
		return b;
	}

	static String res(String name) {
		return Objects.requireNonNull(LauncherWindow.class.getResource("/fr/krosmocraft/launcher/app/" + name),
				"missing resource " + name).toExternalForm();
	}
}
