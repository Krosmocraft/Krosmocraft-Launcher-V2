package fr.krosmocraft.launcher.app.ui;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;

public final class AnimatedCheckBox extends StackPane {

	private final Rectangle background = new Rectangle();
	private final Label checkMark = new Label("✔");
	private final BooleanProperty checked = new SimpleBooleanProperty();

	public AnimatedCheckBox(boolean initial, Label linkedLabel) {
		background.setFill(Color.BLACK);
		background.setArcWidth(5);
		background.setArcHeight(5);
		background.setOpacity(0.6);
		checkMark.setTextFill(Color.WHITE);
		checkMark.setOpacity(0);
		getChildren().addAll(background, checkMark);

		setOnMouseClicked(e -> toggle(linkedLabel, true));
		setOnMouseEntered(e -> {
			if (!checked.get()) {
				Fx.fade(linkedLabel, 0.9, 200);
			}
		});
		setOnMouseExited(e -> {
			if (!checked.get()) {
				Fx.fade(linkedLabel, 0.4, 200);
			}
		});
		if (initial) {
			toggle(linkedLabel, false);
		}
	}

	public void setBoxSize(double size) {
		background.setWidth(size);
		background.setHeight(size);
		checkMark.setFont(new Font(size * 0.6));
	}

	public BooleanProperty checkedProperty() {
		return checked;
	}

	public void toggle(Label linkedLabel, boolean animate) {
		boolean now = checkMark.getOpacity() <= 0;
		checked.set(now);
		Duration d = Duration.millis(animate ? 200 : 1);
		FadeTransition fade = new FadeTransition(d, checkMark);
		fade.setFromValue(now ? 0 : 1);
		fade.setToValue(now ? 1 : 0);
		ScaleTransition scale = new ScaleTransition(d, checkMark);
		scale.setFromX(now ? 0 : 1);
		scale.setFromY(now ? 0 : 1);
		scale.setToX(now ? 1 : 0);
		scale.setToY(now ? 1 : 0);
		if (now) {
			linkedLabel.setOpacity(0.9);
		}
		fade.play();
		scale.play();
	}
}
