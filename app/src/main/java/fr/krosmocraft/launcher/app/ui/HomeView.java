package fr.krosmocraft.launcher.app.ui;

import fr.krosmocraft.launcher.core.server.ServerEntry;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public final class HomeView extends BorderPane {

	private final ComboBox<ServerEntry> servers = new ComboBox<>();
	private final ProgressBar progress = new ProgressBar(0);
	private final Label status = new Label();
	private final Button play = new Button("Jouer");
	private final Button graphics = new Button("Graphismes");
	private final BooleanProperty busy = new SimpleBooleanProperty();

	public HomeView(String pseudo, String playerCount, List<ServerEntry> serverList, String selectedServer,
					Runnable onPlay, Runnable onGraphics, Runnable onLogout) {
		getStyleClass().add("home");

		Label player = new Label(pseudo);
		player.getStyleClass().add("player");
		Label online = new Label(playerCount + " joueurs en ligne");
		online.getStyleClass().add("muted");
		Button logout = new Button("Déconnexion");
		logout.getStyleClass().add("link-button");
		logout.setOnAction(e -> onLogout.run());
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox top = new HBox(new VBox(player, online), spacer, logout);
		top.getStyleClass().add("home-top");
		setTop(top);

		servers.getItems().setAll(serverList);
		servers.setCellFactory(l -> new ServerCell());
		servers.setButtonCell(new ServerCell());
		servers.getSelectionModel().select(serverList.stream()
				.filter(s -> s.name().equals(selectedServer)).findFirst().orElse(serverList.getFirst()));
		servers.setVisible(serverList.size() > 1);
		servers.setManaged(serverList.size() > 1);

		play.getStyleClass().addAll("primary", "play");
		play.setDefaultButton(true);
		play.setOnAction(e -> onPlay.run());
		graphics.setOnAction(e -> onGraphics.run());
		play.disableProperty().bind(busy);
		graphics.disableProperty().bind(busy);
		servers.disableProperty().bind(busy);

		progress.setMaxWidth(Double.MAX_VALUE);
		progress.visibleProperty().bind(busy);
		status.visibleProperty().bind(busy);
		HBox.setHgrow(progress, Priority.ALWAYS);

		HBox actions = new HBox(servers, graphics, play);
		actions.getStyleClass().add("actions");
		actions.setAlignment(Pos.CENTER_RIGHT);
		VBox bottom = new VBox(status, progress, actions);
		bottom.getStyleClass().add("home-bottom");
		setBottom(bottom);
	}

	public ServerEntry selectedServer() {
		return servers.getValue();
	}

	public BooleanProperty busyProperty() {
		return busy;
	}

	public ProgressBar progressBar() {
		return progress;
	}

	public Label statusLabel() {
		return status;
	}

	private static final class ServerCell extends ListCell<ServerEntry> {
		@Override
		protected void updateItem(ServerEntry item, boolean empty) {
			super.updateItem(item, empty);
			setText(empty || item == null ? null : item.name());
		}
	}
}
