package fr.krosmocraft.launcher.app.ui;

import fr.krosmocraft.launcher.app.Tr;
import fr.krosmocraft.launcher.core.preset.Preset;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static fr.krosmocraft.launcher.app.ui.AbsolutePane.box;

final class PresetGallery {

	interface Slots {
		double x(Geo g, int index, int count);

		double y(Geo g);

		double w(Geo g, int count);

		double h(Geo g);
	}

	private final List<Preset> presets;
	private final List<ImageView> images = new ArrayList<>();
	private final Label name = Fx.label("");
	private final Label description = Fx.label("");
	private String selectedId;

	PresetGallery(AbsolutePane pane, List<Preset> presets, String selectedId, Slots slots,
				  java.util.function.Function<Geo, AbsolutePane.Box> nameBox,
				  java.util.function.Function<Geo, AbsolutePane.Box> descriptionBox,
				  Consumer<Preset> onClick) {
		this.presets = presets;
		this.selectedId = selectedId;
		int n = presets.size();
		for (int i = 0; i < n; i++) {
			Preset p = presets.get(i);
			int index = i;
			ImageView img = new ImageView(screenshot(p));
			img.setOpacity(0);
			images.add(img);
			pane.place(img, g -> box(slots.x(g, index, n), slots.y(g), slots.w(g, n), slots.h(g)));
			img.setOnMouseEntered(e -> {
				Fx.fade(img, 1, 100);
				show(p);
			});
			img.setOnMouseExited(e -> {
				if (!p.id().equals(this.selectedId)) {
					Fx.fade(img, 0.2, 100);
				}
				show(current());
			});
			img.setOnMouseClicked(e -> onClick.accept(p));
		}
		name.setAlignment(Pos.CENTER);
		pane.place(name, nameBox);
		pane.font(name, g -> Fx.bold(g.fs() + 4));
		description.setAlignment(Pos.CENTER);
		description.setTextAlignment(TextAlignment.CENTER);
		pane.place(description, descriptionBox);
		pane.font(description, g -> Fx.regular(g.fs()));
	}

	void appear() {
		for (int i = 0; i < images.size(); i++) {
			Fx.fade(images.get(i), presets.get(i).id().equals(selectedId) ? 1 : 0.2, 500);
		}
		name.setOpacity(0);
		description.setOpacity(0);
		Fx.fade(name, 1, 500);
		Fx.fade(description, 1, 500);
		show(current());
	}

	void select(String id) {
		selectedId = id;
		for (int i = 0; i < images.size(); i++) {
			Fx.fade(images.get(i), presets.get(i).id().equals(id) ? 1 : 0.2, 100);
		}
		show(current());
	}

	private Preset current() {
		return presets.stream().filter(p -> p.id().equals(selectedId)).findFirst().orElse(null);
	}

	private void show(Preset p) {
		name.setText(p == null ? "" : Tr.or("preset." + p.id() + ".name", p.name()));
		description.setText(p == null ? "" : Tr.or("preset." + p.id() + ".description", p.description()) + "\n\n" + impact(p.fpsImpact()));
	}

	static String impact(Preset.FpsImpact impact) {
		return switch (impact == null ? Preset.FpsImpact.MEDIUM : impact) {
			case LOW -> Tr.t("preset.impact.low");
			case MEDIUM -> Tr.t("preset.impact.medium");
			case HIGH -> Tr.t("preset.impact.high");
		};
	}

	static Image screenshot(Preset p) {
		String s = p.screenshot() == null ? "" : p.screenshot();
		if (s.startsWith("builtin:")) {
			return Fx.image(s.substring("builtin:".length()));
		}
		return new Image(s, true);
	}

	static double centred(double centre, double w, double gap, int index, int count) {
		double total = count * w + (count - 1) * gap;
		return centre - total / 2 + index * (w + gap);
	}
}
