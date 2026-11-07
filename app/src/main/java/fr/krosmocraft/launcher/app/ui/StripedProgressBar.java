package fr.krosmocraft.launcher.app.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.css.PseudoClass;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;

public final class StripedProgressBar extends ProgressBar {

	public void small() {
		pseudoClassStateChanged(PseudoClass.getPseudoClass("smol"), true);
		pseudoClassStateChanged(PseudoClass.getPseudoClass("big"), false);
	}

	public void big() {
		pseudoClassStateChanged(PseudoClass.getPseudoClass("smol"), false);
		pseudoClassStateChanged(PseudoClass.getPseudoClass("big"), true);
	}

	public void animate() {
		IntegerProperty status = new SimpleIntegerProperty(1);
		Timeline t = new Timeline(new KeyFrame(Duration.millis(700), new KeyValue(status, 12)));
		t.setCycleCount(Timeline.INDEFINITE);
		status.addListener((obs, old, now) -> {
			pseudoClassStateChanged(PseudoClass.getPseudoClass("status" + old.intValue()), false);
			pseudoClassStateChanged(PseudoClass.getPseudoClass("status" + now.intValue()), true);
		});
		t.play();
	}

	@Override
	public String getUserAgentStylesheet() {
		return Fx.res("progressbar.css");
	}
}
