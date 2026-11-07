package fr.krosmocraft.launcher.app.ui;

import javafx.animation.Animation;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.scene.control.Labeled;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public final class ToggleSwitch extends Labeled {

	private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

	private final BooleanProperty selected = new SimpleBooleanProperty() {
		@Override
		protected void invalidated() {
			pseudoClassStateChanged(SELECTED, get());
		}
	};
	private double windowWidth = Geo.BASE_W;

	public ToggleSwitch() {
		getStyleClass().setAll("toggle-switch");
	}

	public BooleanProperty selectedProperty() {
		return selected;
	}

	public void setWindowWidth(int w) {
		if (w != windowWidth) {
			windowWidth = w;
			if (getSkin() instanceof SwitchSkin s) {
				s.resizeParts();
			}
		}
	}

	@Override
	protected Skin<?> createDefaultSkin() {
		return new SwitchSkin(this);
	}

	@Override
	public String getUserAgentStylesheet() {
		return Fx.res("toggleswitch.css");
	}

	private static final class SwitchSkin extends SkinBase<ToggleSwitch> {
		private final StackPane thumb = new StackPane();
		private final StackPane thumbArea = new StackPane();
		private final TranslateTransition transition = new TranslateTransition(Duration.millis(200), thumb);

		SwitchSkin(ToggleSwitch control) {
			super(control);
			thumb.getStyleClass().setAll("thumb");
			thumbArea.getStyleClass().setAll("thumb-area");
			getChildren().addAll(thumbArea, thumb);
			resizeParts();
			transition.setFromX(0);
			thumbArea.setOnMouseReleased(e -> control.selected.set(!control.selected.get()));
			thumb.setOnMouseReleased(e -> control.selected.set(!control.selected.get()));
			control.selected.addListener((obs, was, now) -> {
				transition.stop();
				if (now) {
					transition.setRate(1);
					transition.jumpTo(Duration.ZERO);
				} else {
					transition.setRate(-1);
					transition.jumpTo(transition.getDuration());
				}
				transition.play();
			});
		}

		void resizeParts() {
			double w = getSkinnable().windowWidth;
			thumb.setPrefSize((int) w / 70, (int) w / 70);
			thumbArea.setPrefSize((int) w / 53 * 4, (int) w / 53);
			getSkinnable().requestLayout();
		}

		@Override
		protected void layoutChildren(double x, double y, double width, double height) {
			double tw = snapSizeX(thumb.prefWidth(-1));
			double th = snapSizeY(thumb.prefHeight(-1));
			thumb.resize(tw, th);
			double aw = snapSizeX(thumbArea.prefWidth(-1));
			double ah = snapSizeY(thumbArea.prefHeight(-1));
			double ay = snapPositionY(y + height / 2 - ah / 2);
			thumbArea.resize(aw, ah);
			thumbArea.setLayoutX(width - aw);
			thumbArea.setLayoutY(ay);
			thumb.setLayoutX(thumbArea.getLayoutX() + 5);
			thumb.setLayoutY(ay + (ah - th) / 2);
			double target = aw - tw - 10;
			transition.setToX(target);
			if (transition.getStatus() == Animation.Status.RUNNING) {
				Duration current = transition.getCurrentTime();
				transition.stop();
				transition.playFrom(current);
			} else {
				thumb.setTranslateX(getSkinnable().selected.get() ? target : 0);
			}
		}

		@Override
		protected double computePrefWidth(double h, double top, double right, double bottom, double left) {
			return left + 20 + thumbArea.prefWidth(-1) + right;
		}

		@Override
		protected double computePrefHeight(double w, double top, double right, double bottom, double left) {
			return top + thumb.prefHeight(-1) + bottom;
		}

		@Override
		protected double computeMaxWidth(double h, double top, double right, double bottom, double left) {
			return getSkinnable().prefWidth(h);
		}

		@Override
		protected double computeMaxHeight(double w, double top, double right, double bottom, double left) {
			return getSkinnable().prefHeight(w);
		}
	}
}
