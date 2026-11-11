package fr.krosmocraft.launcher.app.ui;

import fr.krosmocraft.launcher.app.Tr;
import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.TextAlignment;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MainScreen extends AbsolutePane {

	public interface Actions {
		void play();

		void settings();

		void openUrl(String url);

		void muteChanged(boolean muted);
	}

	private final Label play = big(Tr.t("main.play"));
	private final Label loading = big(Tr.t("main.loading"));
	private final Label step = Fx.label(Tr.t("step.preparing"));
	private final Label players = Fx.label(Tr.t("main.players"));
	private final Label edit = Fx.label(Tr.t("main.edit"));
	private final Label percent = Fx.label("0%");
	private final StripedProgressBar bar = new StripedProgressBar();
	private final ImageView mute = Fx.view("mute.png");
	private final ImageView unmute = Fx.view("unmute.png");
	private final ImageView skin = new ImageView();
	private final Map<Node, Double> targets = new LinkedHashMap<>();
	private boolean ready;
	private boolean finishing;
	private double shown;
	private double target;
	private final AnimationTimer smoother = new AnimationTimer() {
		private long last;

		@Override
		public void start() {
			last = 0;
			super.start();
		}

		@Override
		public void handle(long now) {
			double dt = last == 0 ? 0 : (now - last) / 1e9;
			last = now;
			double gap = target - shown;
			shown = Math.min(target, shown + Math.max(gap * (1 - Math.exp(-6 * dt)), 0.4 * dt));
			bar.setProgress(shown);
			percent.setText(Math.round(shown * 100) + "%");
			if (shown >= target) {
				stop();
				if (finishing) {
					finishReady();
				}
			}
		}
	};
	private boolean muted;

	public MainScreen(String pseudo, String playerCount, String version, boolean muted, Actions actions) {
		this.muted = muted;

		image("topfade.png", g -> box(0, 0, g.w(), g.h() / 8), 1);
		image("fade.png", g -> box(0, g.h() - g.h() / 4, g.w(), g.h() / 4), 1);

		step.setAlignment(Pos.CENTER_LEFT);
		step.setTextAlignment(TextAlignment.CENTER);
		place(step, g -> box(g.r(10), g.r(10) * 5 + g.r(9), g.w(), g.r(10) * 4));
		font(step, g -> Font.font("Verdana", FontPosture.ITALIC, g.fs2() + g.r(4)));
		percent.setAlignment(Pos.CENTER_LEFT);
		place(percent, g -> box(g.r(10) * 22 + g.r(2), g.r(10) * 8 + 1, g.r(10) * 32, g.r(10) * 4));
		font(percent, g -> Fx.regular(g.fs2() + g.r(3)));
		place(bar, g -> box(g.r(10) + g.r(3), g.r(10) * 9 + g.r(6), g.r(10) * 20, g.r(10)));
		onLayout(g -> {
			if (g.small()) {
				bar.small();
			} else {
				bar.big();
			}
		});
		bar.animate();
		for (Node n : new Node[]{step, percent, bar}) {
			n.setOpacity(0);
		}

		place(loading, g -> box(0, g.h() - (g.h() / 11 + g.h() / 23), g.w(), g.h() / 6));
		font(loading, g -> Fx.bold(g.fs2() * 2));
		Fx.glowHover(loading, 1);
		place(play, g -> box(0, g.h() - (g.h() / 11 + g.h() / 23), g.w(), g.h() / 6));
		font(play, g -> Fx.bold(g.fs2() * 2));
		Fx.glowHover(play, 1);
		play.setOnMouseClicked(e -> {
			if (ready) {
				actions.play();
			}
		});
		play.setVisible(false);

		players.setAlignment(Pos.CENTER);
		place(players, g -> box(0, g.h() - g.w() / 17, playersWidth(g), g.h() / 6));
		font(players, g -> Fx.bold(g.fs2()));
		targets.put(players, 1.0);
		Label count = Fx.label(playerCount + "/300");
		count.setTextFill(Color.GRAY);
		count.setAlignment(Pos.CENTER_LEFT);
		place(count, g -> box(playersWidth(g) + g.r(3), g.h() - (g.w() / 34 + g.w() / 35), g.h() / 11, g.h() / 6));
		font(count, g -> Fx.bold(g.fs2() - 4));
		targets.put(count, 1.0);
		Label countHitbox = Fx.hitbox();
		place(countHitbox, g -> box(0, g.h() - g.h() / 20, playersWidth(g) - g.h() / 7 + g.h() / 5 + g.h() / 17, g.h() / 20));
		countHitbox.setOnMouseEntered(e -> {
			Glow glow = new Glow(0);
			players.setEffect(glow);
			count.setEffect(glow);
			Fx.anim(glow.levelProperty(), 1, 200);
		});
		countHitbox.setOnMouseExited(e -> {
			Glow glow = new Glow(1);
			players.setEffect(glow);
			count.setEffect(glow);
			Fx.anim(glow.levelProperty(), 0, 200);
		});

		Label versionLabel = Fx.label("Krosmocraft v" + version);
		versionLabel.setAlignment(Pos.CENTER_RIGHT);
		place(versionLabel, g -> box(g.w() - (g.h() / 4 + g.h() / 10), g.h() - g.w() / 17, g.h() / 6 * 2, g.h() / 6));
		font(versionLabel, g -> Fx.bold(g.fs2()));
		Fx.glowHover(versionLabel, 1);
		targets.put(versionLabel, 1.0);

		ImageView settings = image("settings.png", g -> box(ix(g), g.h() / 2 - (g.w() / 9 + g.w() / 47), g.h() / 32, g.h() / 32), 1);
		image("line.png", g -> box(ix(g), g.h() / 2 - (g.h() / 7 + g.h() / 20), g.h() / 32, g.h() / 24), 1);
		ImageView link = image("link.png", g -> box(ix(g), g.h() / 2 - g.h() / 7, g.h() / 32, g.h() / 32), 1);
		ImageView discord = image("discord.png", g -> box(ix(g), g.h() / 2 - (g.w() / 30 + g.h() / 30), g.h() / 32, g.h() / 32), 1);
		ImageView youtube = image("youtube.png", g -> box(ix(g), g.h() / 2 - (g.h() / 49 + g.h() / 46), g.h() / 32, g.h() / 34), 1);
		place(mute, g -> box(ix(g), g.h() / 2 + g.h() / 135, g.h() / 32, g.h() / 34));
		place(unmute, g -> box(ix(g), g.h() / 2 + g.h() / 135, g.h() / 32, g.h() / 34));
		mute.setOpacity(0);
		unmute.setOpacity(0);
		targets.put(muted ? unmute : mute, 1.0);

		iconHitbox(settings, g -> box(ix(g), g.h() / 2 - (g.w() / 9 + g.w() / 47), g.h() / 32, g.h() / 32), actions::settings);
		iconHitbox(link, g -> box(ix(g), g.h() / 2 - g.h() / 7, g.h() / 32, g.h() / 32),
				() -> actions.openUrl("https://krosmocraft.fr/"));
		iconHitbox(discord, g -> box(ix(g), g.h() / 2 - (g.w() / 30 + g.h() / 30), g.h() / 32, g.h() / 32),
				() -> actions.openUrl("https://discord.gg/dD7XtDjuK6"));
		iconHitbox(youtube, g -> box(ix(g), g.h() / 2 - (g.h() / 49 + g.h() / 46), g.h() / 32, g.h() / 32),
				() -> actions.openUrl("https://www.youtube.com/@krosmocraft/videos"));
		Label muteHitbox = Fx.hitbox();
		place(muteHitbox, g -> box(ix(g), g.h() / 2 + g.h() / 135, g.h() / 32, g.h() / 32));
		Fx.scaleHover(muteHitbox, mute, unmute);
		muteHitbox.setOnMouseClicked(e -> {
			this.muted = !this.muted;
			mute.setOpacity(this.muted ? 0 : 1);
			unmute.setOpacity(this.muted ? 1 : 0);
			actions.muteChanged(this.muted);
		});

		Label name = Fx.label(pseudo);
		name.setAlignment(Pos.CENTER_RIGHT);
		place(name, g -> box(g.w() - (g.w() / 4 + g.w() / 33), g.h() / 15 * 2, g.h() / 4, g.w() / 35));
		font(name, g -> Fx.bold(g.fs2()));
		DropShadow shadow = new DropShadow();
		shadow.setSpread(0.7);
		shadow.setRadius(15);
		Glow nameGlow = new Glow(0.7);
		nameGlow.setInput(shadow);
		name.setEffect(nameGlow);
		targets.put(name, 1.0);
		image("picBg.png", g -> box(g.w() - (g.h() / 8 + g.h() / 12), g.h() / 12, g.h() / 20 * 3, g.h() / 20 * 3), 1);
		place(skin, g -> box(g.w() - (g.h() / 12 + g.w() / 22), g.h() / 11 + 1, g.w() / 29, g.h() / 15 * 2));
		skin.setOpacity(0);
		targets.put(skin, 1.0);
		ImageView overlay = Fx.view("picOverlay.png");
		overlay.setOpacity(0);
		place(overlay, g -> box(g.w() - (g.h() / 8 + g.h() / 12), g.h() / 12, g.h() / 20 * 3, g.h() / 20 * 3));
		edit.setAlignment(Pos.CENTER);
		edit.setOpacity(0);
		place(edit, g -> box(g.w() - (g.h() / 8 + g.h() / 12), g.h() / 12, g.h() / 20 * 3, g.h() / 20 * 3));
		font(edit, g -> Fx.bold(g.fs2() + 2));
		edit.setOnMouseEntered(e -> {
			Fx.fade(overlay, 1, 200);
			Fx.fade(edit, 1, 200);
		});
		edit.setOnMouseExited(e -> {
			Fx.fade(overlay, 0, 200);
			Fx.fade(edit, 0, 200);
		});
		edit.setOnMouseClicked(e -> actions.openUrl("https://krosmocraft.fr/skin"));

		for (Node n : targets.keySet()) {
			n.setOpacity(0);
		}
		loading.setOpacity(0);
		play.setOpacity(0);
	}

	private static int ix(Geo g) {
		return g.w() - (g.h() / 11 + g.w() / 30);
	}

	public void setSkin(Image image) {
		skin.setImage(image);
	}

	public void show(boolean animate) {
		targets.forEach((n, to) -> {
			if (animate) {
				Fx.fade(n, to, 700);
			} else {
				n.setOpacity(to);
			}
		});
		Label button = ready ? play : loading;
		if (animate) {
			Fx.fade(button, 1, 700);
		} else {
			button.setOpacity(1);
		}
		if (!ready) {
			showProgress(animate);
		}
	}

	private void showProgress(boolean animate) {
		setProgressVisible(true);
		Fx.fade(step, 0.4, animate ? 700 : 1);
		Fx.fade(percent, 0.4, animate ? 700 : 1);
		Fx.fade(bar, 0.8, animate ? 700 : 1);
	}

	public void preparing() {
		ready = false;
		finishing = false;
		smoother.stop();
		shown = 0;
		target = 0;
		play.setVisible(false);
		loading.setVisible(true);
		loading.setOpacity(1);
		bar.setProgress(0);
		percent.setText("0%");
		step.setText(Tr.t("step.preparing"));
		showProgress(false);
	}

	public void progress(String text, double fraction) {
		step.setText(text);
		if (fraction > target) {
			target = Math.min(fraction, 1);
			smoother.start();
		}
	}

	public void ready() {
		finishing = true;
		target = 1;
		smoother.start();
	}

	private void finishReady() {
		finishing = false;
		ready = true;
		setProgressVisible(false);
		loading.setVisible(false);
		play.setText(Tr.t("main.play"));
		play.setVisible(true);
		play.setOpacity(1);
	}

	private void setProgressVisible(boolean visible) {
		step.setVisible(visible);
		percent.setVisible(visible);
		bar.setVisible(visible);
	}

	public void launching() {
		ready = false;
		play.setText(Tr.t("main.launching"));
	}

	// the old h / 7 slot, wider when the translation doesn't fit
	private double playersWidth(Geo g) {
		return Math.max(g.h() / 7, players.prefWidth(-1) + g.r(10) * 2);
	}

	public void retranslate() {
		loading.setText(Tr.t("main.loading"));
		players.setText(Tr.t("main.players"));
		edit.setText(Tr.t("main.edit"));
		play.setText(Tr.t(ready || !play.isVisible() ? "main.play" : "main.launching"));
	}

	public void launchFailed() {
		ready = true;
		play.setText(Tr.t("main.play"));
	}

	private ImageView image(String name, java.util.function.Function<Geo, Box> rule, double target) {
		ImageView v = Fx.view(name);
		place(v, rule);
		targets.put(v, target);
		return v;
	}

	private void iconHitbox(ImageView icon, java.util.function.Function<Geo, Box> rule, Runnable action) {
		Label hit = Fx.hitbox();
		place(hit, rule);
		Fx.scaleHover(hit, icon);
		hit.setOnMouseClicked(e -> action.run());
	}

	private static Label big(String text) {
		Label l = Fx.label(text);
		l.setAlignment(Pos.CENTER);
		l.setTextAlignment(TextAlignment.CENTER);
		return l;
	}
}
