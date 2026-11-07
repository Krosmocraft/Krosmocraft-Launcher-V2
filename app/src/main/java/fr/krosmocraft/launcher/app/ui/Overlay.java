package fr.krosmocraft.launcher.app.ui;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.List;

import static fr.krosmocraft.launcher.app.ui.AbsolutePane.box;

public abstract class Overlay extends AbsolutePane {

	protected Overlay() {
		setPickOnBounds(true); // block clicks to the screen below
		Rectangle blur = new Rectangle();
		blur.setFill(Color.rgb(10, 10, 10, 0.9));
		blur.setOpacity(0);
		place(blur, g -> box(0, 0, g.w(), g.h()));
		Fx.fade(blur, 1, 500);
	}

	public void open(LauncherWindow window) {
		window.overlayLayer().place(this, g -> box(0, 0, g.w(), g.h()));
	}

	protected <N extends Node> N appear(N n, double to) {
		n.setOpacity(0);
		Fx.fade(n, to, 500);
		return n;
	}

	public void close(double ms, Runnable after) {
		for (Node n : List.copyOf(getChildren())) {
			n.setDisable(true);
			Fx.fade(n, 0, ms);
		}
		PauseTransition p = new PauseTransition(Duration.millis(ms));
		p.setOnFinished(e -> {
			if (getParent() instanceof AbsolutePane parent) {
				parent.remove(this);
			}
			if (after != null) {
				after.run();
			}
		});
		p.play();
	}

	protected static Font helveticaBold(double size) {
		return Font.font("Helvetica", FontWeight.BOLD, size);
	}

	// handlers wired after 500 ms so a double click doesn't skip the popup
	protected static void afterIntro(Runnable r) {
		PauseTransition p = new PauseTransition(Duration.millis(500));
		p.setOnFinished(e -> r.run());
		p.play();
	}
}
