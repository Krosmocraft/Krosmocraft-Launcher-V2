package fr.krosmocraft.launcher.app.ui;

import fr.krosmocraft.launcher.app.Texts;
import fr.krosmocraft.launcher.core.preset.Preset;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

public final class PresetPickerView extends VBox {

	public PresetPickerView(List<Preset> presets, String currentId, boolean firstLaunch,
							BiConsumer<Preset, Boolean> onPick, Runnable onCancel) {
		getStyleClass().add("presets");
		setAlignment(Pos.CENTER);

		Label title = new Label(firstLaunch ? "Choisissez vos graphismes" : "Graphismes");
		title.getStyleClass().add("heading");
		Label subtitle = new Label("Modifiable à tout moment depuis le bouton Graphismes.");
		subtitle.getStyleClass().add("muted");

		HBox cards = new HBox();
		cards.getStyleClass().add("cards");
		cards.setAlignment(Pos.CENTER);
		for (Preset p : presets) {
			cards.getChildren().add(card(p, p.id().equals(currentId), onPick));
		}

		getChildren().addAll(title, subtitle, cards);
		if (!firstLaunch) {
			Button cancel = new Button("Retour");
			cancel.setOnAction(e -> onCancel.run());
			getChildren().add(cancel);
		}
	}

	private VBox card(Preset p, boolean current, BiConsumer<Preset, Boolean> onPick) {
		ImageView shot = new ImageView(new Image(p.screenshot(), true));
		shot.setPreserveRatio(true);
		shot.setFitWidth(260);
		Label name = new Label(p.name());
		name.getStyleClass().add("card-title");
		Label impact = new Label(Texts.impact(p.fpsImpact()));
		impact.getStyleClass().addAll("badge", "impact-" + p.fpsImpact().name().toLowerCase(Locale.ROOT));
		Label description = new Label(p.description());
		description.setWrapText(true);
		VBox.setVgrow(description, Priority.ALWAYS);
		Button pick = new Button(current ? "Réinitialiser mes réglages" : "Choisir");
		pick.getStyleClass().add(current ? "secondary" : "primary");
		pick.setMaxWidth(Double.MAX_VALUE);
		pick.setOnAction(e -> onPick.accept(p, current));

		VBox card = new VBox(shot, name, impact, description, pick);
		card.getStyleClass().add("card");
		if (current) {
			card.getStyleClass().add("current");
		}
		return card;
	}
}
