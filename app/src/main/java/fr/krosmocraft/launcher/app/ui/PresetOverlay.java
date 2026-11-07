package fr.krosmocraft.launcher.app.ui;

import fr.krosmocraft.launcher.core.preset.Preset;
import javafx.geometry.Pos;
import javafx.scene.control.Label;

import java.util.List;
import java.util.function.Consumer;

import static fr.krosmocraft.launcher.app.ui.AbsolutePane.box;

public final class PresetOverlay extends Overlay {

	private boolean chosen;

	public PresetOverlay(List<Preset> presets, Consumer<Preset> onChoose) {
		Label title = appear(Fx.label("Paramètres graphiques"), 1);
		title.setAlignment(Pos.CENTER);
		place(title, g -> box(0, 12 * g.r(10), g.w(), 30));
		font(title, g -> helveticaBold(g.fs() * 2));
		Label subtitle = appear(Fx.label("Choisissez une option graphique adaptée"), 1);
		subtitle.setAlignment(Pos.CENTER);
		place(subtitle, g -> box(0, 16 * g.r(10), g.w(), 30));
		font(subtitle, g -> Fx.regular(g.fs()));

		PresetGallery gallery = new PresetGallery(this, presets, null, new PresetGallery.Slots() {
			@Override
			public double x(Geo g, int i, int n) {
				if (n == 3) {
					return switch (i) {
						case 0 -> g.w() / 3 - g.w() / 14 + 1;
						case 1 -> g.w() / 2 - g.w() / 15 - g.r(2);
						default -> g.w() / 2 + g.w() / 10 + 1;
					};
				}
				return PresetGallery.centred(g.w() / 2.0, g.r(10) * 15, g.r(10) * 2.5, i, n);
			}

			@Override
			public double y(Geo g) {
				return g.h() / 2 - g.h() / 8 - g.r(5);
			}

			@Override
			public double w(Geo g, int n) {
				return g.r(10) * 15;
			}

			@Override
			public double h(Geo g) {
				return g.r(10) * 10 + g.r(2);
			}
		}, g -> box(0, g.h() / 2 + g.h() / 18, g.w(), g.r(10) * 4),
				g -> box(0, g.h() / 2 + g.h() / 12, g.w(), g.r(10) * 16),
				preset -> {
					if (!chosen) {
						chosen = true;
						close(700, () -> onChoose.accept(preset));
					}
				});
		gallery.appear();
	}
}
