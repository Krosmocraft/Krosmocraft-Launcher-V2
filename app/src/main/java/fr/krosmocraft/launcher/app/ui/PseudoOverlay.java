package fr.krosmocraft.launcher.app.ui;

import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static fr.krosmocraft.launcher.app.ui.AbsolutePane.box;

public final class PseudoOverlay extends Overlay {

	private static final Pattern FORBIDDEN = Pattern.compile("[\\p{M}\\p{InGreek}\\p{InCyrillic}\\p{InArabic}"
			+ "\\p{InHebrew}\\p{InDevanagari}\\p{InCJK_Unified_Ideographs}\\p{InLatin_Extended_A}\\p{InLatin_Extended_B}"
			+ "\\p{Sc}\\p{P}\\x{1F600}-\\x{1F64F}]");

	private final TextField field = new TextField();
	private final Button send = new Button("Confirmer >");
	private final ImageView spinner = Fx.view("loading.png");
	private Timeline spin;
	private boolean busy;
	private boolean formatting;

	public PseudoOverlay(Consumer<String> onSubmit) {
		Label title = appear(Fx.label("Veuillez choisir un pseudo de compte"), 1);
		title.setAlignment(Pos.CENTER);
		place(title, g -> box(0, 0, g.w(), g.h() - g.r(10) * 16));
		font(title, g -> helveticaBold(g.fs2() * 2));
		Label m1 = appear(Fx.label("Ce pseudo permettra aux autres joueurs de vous reconnaître peu importe votre personnage"), 1);
		m1.setAlignment(Pos.CENTER);
		place(m1, g -> box(0, 0, g.w(), g.h() - g.r(10) * 6));
		font(m1, g -> Fx.regular(g.fs2()));
		Label m2 = appear(Fx.label("Attention, celui-ci est définitif"), 1);
		m2.setAlignment(Pos.CENTER);
		place(m2, g -> box(0, 0, g.w(), g.h() + g.r(10) * 2));
		font(m2, g -> Fx.regular(g.fs2()));

		field.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().length() <= 13 ? c : null));
		field.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4); -fx-text-fill: white;");
		field.setPromptText("Pseudo");
		field.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, new CornerRadii(3), BorderWidths.DEFAULT)));
		place(field, g -> box(g.w() / 2 - g.r(10) * 13 + g.r(6), g.h() / 2 + g.r(10) * 6, g.r(10) * 25, g.r(10) * 4));
		font(field, g -> Fx.regular(g.fs2()));
		field.textProperty().addListener((o, old, now) -> {
			if (formatting) {
				return;
			}
			if (!allowed(now)) {
				formatting = true;
				field.setText(old);
				formatting = false;
				return;
			}
			formatting = true;
			field.setText(format(now));
			formatting = false;
			send.setOpacity(field.getText().length() < 3 ? 0.2 : 0.9);
		});

		send.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
		send.setOpacity(0.2);
		send.setEffect(new Glow(0.5));
		place(send, g -> box(g.w() / 2 - g.w() / 7, g.h() / 2 + g.h() / 6 + g.h() / 21, g.w() / 4 + g.w() / 28, g.h() / 10));
		font(send, g -> Fx.regular(g.fs2() * 2));
		send.setOnMouseEntered(e -> {
			if (valid()) {
				Glow glow = new Glow(0.5);
				send.setEffect(glow);
				Fx.fade(send, 1, 200);
				Fx.anim(glow.levelProperty(), 1, 200);
			}
		});
		send.setOnMouseExited(e -> {
			if (valid()) {
				Glow glow = new Glow(1);
				send.setEffect(glow);
				Fx.fade(send, 0.8, 200);
				Fx.anim(glow.levelProperty(), 0.5, 200);
			}
		});
		spinner.setOpacity(0);
		place(spinner, g -> box(g.w() / 2 - g.w() / 60, g.h() - g.h() / 6, g.h() / 24, g.h() / 24));

		afterIntro(() -> {
			field.setOnKeyPressed(e -> {
				if (e.getCode() == KeyCode.ENTER) {
					submit(onSubmit);
				}
			});
			send.setOnAction(e -> submit(onSubmit));
		});
	}

	private boolean valid() {
		return field.getText().length() >= 3;
	}

	private void submit(Consumer<String> onSubmit) {
		if (!valid() || busy) {
			return;
		}
		busy = true;
		send.setText("Chargement...");
		field.setDisable(true);
		field.setOpacity(0.7);
		spinner.setOpacity(1);
		if (spin == null) {
			spin = Fx.spin(spinner);
		}
		onSubmit.accept(field.getText());
	}

	public void rejected() {
		busy = false;
		send.setText("Confirmer >");
		field.setDisable(false);
		field.setOpacity(1);
		spinner.setOpacity(0);
	}

	static boolean allowed(String s) {
		if (s.contains(" ") || FORBIDDEN.matcher(Normalizer.normalize(s, Normalizer.Form.NFD)).find()) {
			return false;
		}
		int digits = 0;
		int dashes = 0;
		Map<Character, Integer> letters = new HashMap<>();
		for (char c : s.toCharArray()) {
			if (!Character.isLetterOrDigit(c)) {
				if (c != '-') {
					return false;
				}
				dashes++;
			} else if (Character.isDigit(c)) {
				digits++;
			} else {
				letters.merge(Character.toLowerCase(c), 1, Integer::sum);
			}
			if (digits > 3 || dashes > 2 || letters.getOrDefault(Character.toLowerCase(c), 0) > 5) {
				return false;
			}
		}
		return true;
	}

	static String format(String text) {
		if (text.isEmpty()) {
			return text;
		}
		char[] chars = text.toCharArray();
		int upper = 0;
		if (Character.isLetter(chars[0])) {
			chars[0] = Character.toUpperCase(chars[0]);
			upper++;
		}
		for (int i = 1; i < chars.length; i++) {
			if (chars[i - 1] == '-') {
				if (Character.isLowerCase(chars[i])) {
					chars[i] = Character.toUpperCase(chars[i]);
				}
				if (!Character.isDigit(chars[i]) && chars[i] != '-') {
					upper++;
				}
			}
		}
		for (int i = 1; i < chars.length; i++) {
			if (Character.isUpperCase(chars[i]) && chars[i - 1] != '-') {
				if (upper >= 3) {
					chars[i] = Character.toLowerCase(chars[i]);
				}
				upper++;
			}
		}
		return new String(chars);
	}
}
