package fr.krosmocraft.launcher.app.ui;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.TextAlignment;

import static fr.krosmocraft.launcher.app.ui.AbsolutePane.box;

public final class ErrorOverlay extends Overlay {

	public ErrorOverlay(String title, String message, Runnable onClose) {
		Label t = appear(Fx.label(title), 1);
		t.setAlignment(Pos.CENTER);
		place(t, g -> box(0, 0, g.w(), g.h() - g.r(10) * 7));
		font(t, g -> helveticaBold(g.fs2() * 2));

		Label m = appear(Fx.label(message), 1);
		m.setAlignment(Pos.CENTER);
		m.setTextAlignment(TextAlignment.CENTER);
		place(m, g -> box(0, 0, g.w(), g.h() + g.r(10) * 7));
		font(m, g -> Fx.regular(g.fs2()));

		Label retry = Fx.label("Réessayer >");
		retry.setAlignment(Pos.CENTER);
		retry.setStyle("-fx-border-color: white; -fx-border-width: 2px; -fx-background-color: transparent;");
		retry.setOpacity(0.8);
		retry.setEffect(new Glow(0.5));
		place(retry, g -> box(g.w() / 2 - g.w() / 20, g.h() / 2 + g.h() / 6, g.w() / 10, g.h() / 20));
		font(retry, g -> helveticaBold(g.fs2()));
		retry.setOnMouseEntered(e -> {
			Glow glow = new Glow(0.5);
			retry.setEffect(glow);
			Fx.fade(retry, 1, 200);
			Fx.anim(glow.levelProperty(), 1, 200);
		});
		retry.setOnMouseExited(e -> {
			Glow glow = new Glow(1);
			retry.setEffect(glow);
			Fx.fade(retry, 0.8, 200);
			Fx.anim(glow.levelProperty(), 0.5, 200);
		});

		afterIntro(() -> {
			boolean[] done = {false};
			EventHandler<KeyEvent> enter = new EventHandler<>() {
				@Override
				public void handle(KeyEvent e) {
					if (e.getCode() == KeyCode.ENTER && !done[0]) {
						e.consume();
						done[0] = true;
						getScene().removeEventFilter(KeyEvent.KEY_PRESSED, this);
						close(400, onClose);
					}
				}
			};
			if (getScene() != null) {
				getScene().addEventFilter(KeyEvent.KEY_PRESSED, enter);
			}
			retry.setOnMouseClicked(e -> {
				if (!done[0]) {
					done[0] = true;
					if (getScene() != null) {
						getScene().removeEventFilter(KeyEvent.KEY_PRESSED, enter);
					}
					close(400, onClose);
				}
			});
		});
	}
}
