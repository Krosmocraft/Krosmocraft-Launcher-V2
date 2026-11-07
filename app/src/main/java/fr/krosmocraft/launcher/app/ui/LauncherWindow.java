package fr.krosmocraft.launcher.app.ui;

import fr.krosmocraft.launcher.core.server.ServerEntry;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;
import java.util.function.Consumer;

import static fr.krosmocraft.launcher.app.ui.AbsolutePane.box;

public final class LauncherWindow {

	private final Stage stage;
	private final AbsolutePane root = new AbsolutePane();
	private final AbsolutePane screenLayer = new AbsolutePane();
	private final AbsolutePane overlayLayer = new AbsolutePane();
	private final Label title = Fx.label("Krosmocraft");
	private final ComboBox<ServerEntry> serverChoice = new ComboBox<>();
	private Rectangle2D restoreBounds;
	private boolean maximized;
	private MediaPlayer video;

	public LauncherWindow(Stage stage) {
		this.stage = stage;
		Fx.loadFonts();
		boolean transparent = Platform.isSupported(ConditionalFeature.TRANSPARENT_WINDOW);
		stage.initStyle(transparent ? StageStyle.TRANSPARENT : StageStyle.UNDECORATED);
		stage.setResizable(false);
		stage.setTitle("Launcher Krosmocraft");
		stage.getIcons().add(Fx.image("favicon.png"));
		root.setPickOnBounds(true);
		root.setStyle("-fx-background-color: transparent;");

		Rectangle clip = new Rectangle();
		root.setClip(clip);
		root.onLayout(g -> {
			clip.setWidth(g.w());
			clip.setHeight(g.h());
			clip.setArcWidth(maximized ? 0 : 15);
			clip.setArcHeight(maximized ? 0 : 15);
		});

		playVideo();
		root.place(screenLayer, g -> box(0, 0, g.w(), g.h()));
		buildChrome();
		root.place(overlayLayer, g -> box(0, 0, g.w(), g.h()));

		Scene scene = new Scene(root, Geo.BASE_W, Geo.BASE_H);
		scene.setFill(transparent ? Color.TRANSPARENT : Color.BLACK);
		enableDrag(scene);
		stage.setScene(scene);
		stage.setWidth(Geo.BASE_W);
		stage.setHeight(Geo.BASE_H);
		center();
		stage.setOnCloseRequest(e -> Platform.exit());
	}

	public Stage stage() {
		return stage;
	}

	public AbsolutePane screenLayer() {
		return screenLayer;
	}

	public AbsolutePane overlayLayer() {
		return overlayLayer;
	}

	public void show() {
		stage.show();
	}

	public void setServers(List<ServerEntry> servers, ServerEntry selected, Consumer<ServerEntry> onChange) {
		serverChoice.setOnAction(null);
		serverChoice.getItems().setAll(servers);
		serverChoice.setValue(selected);
		updateTitle(selected);
		serverChoice.setOnAction(e -> {
			ServerEntry s = serverChoice.getValue();
			if (s != null) {
				updateTitle(s);
				onChange.accept(s);
			}
		});
	}

	public ServerEntry selectedServer() {
		return serverChoice.getValue();
	}

	private void updateTitle(ServerEntry s) {
		title.setText("Krosmocraft" + (s == null || s.name().equals("Live") ? "" : " [" + s.name() + "]"));
	}

	private void playVideo() {
		try {
			video = new MediaPlayer(new Media(Fx.res("background.mp4")));
			MediaView view = new MediaView(video);
			view.setPreserveRatio(false);
			root.place(view, g -> box(0, 0, g.w(), g.h()));
			root.onLayout(g -> {
				view.setFitWidth(g.w());
				view.setFitHeight(g.h());
			});
			video.setOnReady(() -> {
				video.setCycleCount(MediaPlayer.INDEFINITE);
				video.play();
			});
		} catch (RuntimeException e) {
			// no media support (linux without gstreamer), black background
			System.err.println("Background video unavailable: " + e);
		}
	}

	private void buildChrome() {
		title.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
		root.place(title, g -> box(g.w() / 100, g.h() / 120, g.w() / 4, g.h() / 50));
		root.font(title, g -> Fx.bold(g.fs2()));
		Fx.glowHover(title, 0.9);
		title.setOnMouseClicked(e -> {
			if (!serverChoice.getItems().isEmpty()) {
				serverChoice.show();
			}
		});

		serverChoice.setVisibleRowCount(5);
		serverChoice.setOpacity(0);
		serverChoice.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: transparent;");
		serverChoice.getStyleClass().add("server-choice");
		serverChoice.getStylesheets().add(Fx.res("servers.css"));
		serverChoice.setCellFactory(l -> new ListCell<>() {
			@Override
			protected void updateItem(ServerEntry item, boolean empty) {
				super.updateItem(item, empty);
				setText(null);
				if (item == null || empty) {
					setGraphic(null);
					return;
				}
				double size = root.geo().h() / 50 * 0.9;
				Label name = new Label(item.name());
				name.getStyleClass().add("name");
				name.setFont(Fx.bold(size));
				Label badge = new Label(item.channel().toUpperCase());
				badge.getStyleClass().addAll("badge", item.channel());
				badge.setFont(Fx.bold(size * 0.7));
				Region gap = new Region();
				HBox.setHgrow(gap, Priority.ALWAYS);
				HBox row = new HBox(name, gap, badge);
				row.setAlignment(Pos.CENTER_LEFT);
				setGraphic(row);
			}
		});
		root.place(serverChoice, g -> box(g.w() / 100, g.h() / 120 * 2.5, g.w() / 4, g.h() / 50));

		chromeButton("close.png", g -> g.w() - g.w() / 30, Platform::exit);
		chromeButton("reduce.png", g -> g.w() - g.w() / 10, () -> stage.setIconified(true));
		chromeButton("maxi.png", g -> g.w() - g.w() / 15, this::toggleMaximized);
	}

	private void chromeButton(String icon, java.util.function.ToIntFunction<Geo> x, Runnable action) {
		ImageView img = Fx.view(icon);
		Button b = new Button(null, img);
		b.setBackground(null);
		b.setOnMouseEntered(e -> b.setOpacity(0.8));
		b.setOnMouseExited(e -> b.setOpacity(1));
		b.setOnAction(e -> action.run());
		root.place(b, g -> box(x.applyAsInt(g), 2, g.h() / 20, g.h() / 40));
		root.onLayout(g -> {
			img.setFitWidth(g.h() / 40);
			img.setFitHeight(g.h() / 40);
		});
	}

	public void toggleMaximized() {
		if (maximized) {
			stage.setX(restoreBounds.getMinX());
			stage.setY(restoreBounds.getMinY());
			stage.setWidth(Geo.BASE_W);
			stage.setHeight(Geo.BASE_H);
			maximized = false;
		} else {
			restoreBounds = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
			Screen screen = Screen.getScreensForRectangle(restoreBounds).stream().findFirst().orElse(Screen.getPrimary());
			Rectangle2D b = screen.getVisualBounds();
			stage.setX(b.getMinX());
			stage.setY(b.getMinY());
			stage.setWidth(b.getWidth());
			stage.setHeight(b.getHeight());
			maximized = true;
		}
		root.requestLayout();
	}

	private void center() {
		Rectangle2D b = Screen.getPrimary().getVisualBounds();
		stage.setX(b.getMinX() + (b.getWidth() - Geo.BASE_W) / 2);
		stage.setY(b.getMinY() + (b.getHeight() - Geo.BASE_H) / 2);
	}

	private void enableDrag(Scene scene) {
		double[] delta = new double[2];
		boolean[] dragging = new boolean[1];
		scene.setOnMousePressed(e -> {
			dragging[0] = !maximized && !isInput(e.getPickResult().getIntersectedNode());
			delta[0] = stage.getX() - e.getScreenX();
			delta[1] = stage.getY() - e.getScreenY();
		});
		scene.setOnMouseDragged(e -> {
			if (dragging[0]) {
				stage.setX(e.getScreenX() + delta[0]);
				stage.setY(e.getScreenY() + delta[1]);
			}
		});
	}

	private static boolean isInput(Node n) {
		for (; n != null; n = n.getParent()) {
			if (n instanceof Control c && !(c instanceof Label)) {
				return true;
			}
		}
		return false;
	}
}
