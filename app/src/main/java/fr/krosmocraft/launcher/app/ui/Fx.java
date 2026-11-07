package fr.krosmocraft.launcher.app.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.Objects;

public final class Fx {

	private Fx() {
	}

	public static String res(String name) {
		return Objects.requireNonNull(Fx.class.getResource("/fr/krosmocraft/launcher/app/" + name),
				"missing resource " + name).toExternalForm();
	}

	public static Image image(String name) {
		return new Image(res(name));
	}

	public static ImageView view(String name) {
		ImageView v = new ImageView(image(name));
		v.setSmooth(true);
		return v;
	}

	public static void loadFonts() {
		Font.loadFont(res("Comfortaa-Regular.ttf"), 14);
		Font.loadFont(res("Comfortaa-Bold.ttf"), 14);
	}

	public static Font regular(double size) {
		return Font.font("Comfortaa", size);
	}

	public static Font bold(double size) {
		return Font.font("Comfortaa", FontWeight.BOLD, size);
	}

	public static Label label(String text) {
		Label l = new Label(text);
		l.setTextFill(Color.WHITE);
		l.setStyle("-fx-background-color: transparent;");
		return l;
	}

	public static Timeline anim(DoubleProperty p, double to, double ms) {
		Timeline t = new Timeline(new KeyFrame(Duration.millis(ms), new KeyValue(p, to)));
		t.play();
		return t;
	}

	public static Timeline fade(Node n, double to, double ms) {
		return anim(n.opacityProperty(), to, ms);
	}

	public static Timeline spin(Node n) {
		Timeline t = new Timeline(new KeyFrame(Duration.millis(1200), new KeyValue(n.rotateProperty(), 360)));
		t.setCycleCount(Animation.INDEFINITE);
		t.play();
		return t;
	}

	public static void glowHover(Node n, double level) {
		n.setOnMouseEntered(e -> {
			Glow g = new Glow(0);
			n.setEffect(g);
			anim(g.levelProperty(), level, 200);
		});
		n.setOnMouseExited(e -> {
			Glow g = new Glow(level);
			n.setEffect(g);
			anim(g.levelProperty(), 0, 200);
		});
	}

	public static void opacityHover(Node n, double rest, double hover) {
		n.setOpacity(rest);
		n.setOnMouseEntered(e -> fade(n, hover, 200));
		n.setOnMouseExited(e -> fade(n, rest, 200));
	}

	public static void scaleHover(Node hitbox, Node... icons) {
		hitbox.setOnMouseEntered(e -> {
			for (Node i : icons) {
				anim(i.scaleXProperty(), 1.4, 100);
				anim(i.scaleYProperty(), 1.4, 100);
			}
		});
		hitbox.setOnMouseExited(e -> {
			for (Node i : icons) {
				anim(i.scaleXProperty(), 1, 100);
				anim(i.scaleYProperty(), 1, 100);
			}
		});
	}

	public static Label hitbox() {
		Label l = new Label();
		l.setStyle("-fx-background-color: transparent;");
		l.setCursor(javafx.scene.Cursor.DEFAULT);
		return l;
	}
}
