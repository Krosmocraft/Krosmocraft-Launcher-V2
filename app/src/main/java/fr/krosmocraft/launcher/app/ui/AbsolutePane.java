package fr.krosmocraft.launcher.app.ui;

import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class AbsolutePane extends Pane {

	public static final double PREF = -1;

	public record Box(double x, double y, double w, double h) {
	}

	private final Map<Node, Function<Geo, Box>> bounds = new HashMap<>();
	private final Map<Node, Function<Geo, Font>> fonts = new HashMap<>();

	public AbsolutePane() {
		setPickOnBounds(false); // empty areas let clicks through to the layers below
	}

	public static Box box(double x, double y, double w, double h) {
		return new Box(x, y, w, h);
	}

	public <N extends Node> N place(N node, Function<Geo, Box> rule) {
		bounds.put(node, rule);
		if (!getChildren().contains(node)) {
			getChildren().add(node);
		}
		requestLayout();
		return node;
	}

	public <N extends Node> N font(N node, Function<Geo, Font> rule) {
		fonts.put(node, rule);
		requestLayout();
		return node;
	}

	public void onLayout(java.util.function.Consumer<Geo> hook) {
		hooks.add(hook);
		requestLayout();
	}

	private final java.util.List<java.util.function.Consumer<Geo>> hooks = new java.util.ArrayList<>();

	public void remove(Node node) {
		bounds.remove(node);
		fonts.remove(node);
		getChildren().remove(node);
	}

	public Geo geo() {
		return new Geo((int) Math.round(getWidth()), (int) Math.round(getHeight()));
	}

	@Override
	protected void layoutChildren() {
		Geo g = geo();
		if (g.w() <= 0 || g.h() <= 0) {
			return;
		}
		hooks.forEach(h -> h.accept(g));
		fonts.forEach((node, rule) -> {
			Font f = rule.apply(g);
			if (node instanceof Labeled l) {
				l.setFont(f);
			} else if (node instanceof TextInputControl t) {
				t.setFont(f);
			}
		});
		for (Node node : getChildren()) {
			Function<Geo, Box> rule = bounds.get(node);
			if (rule == null) {
				continue;
			}
			Box b = rule.apply(g);
			switch (node) {
				case ImageView iv -> {
					iv.setFitWidth(b.w());
					iv.setFitHeight(b.h());
					iv.relocate(b.x(), b.y());
				}
				case Rectangle r -> {
					r.setWidth(b.w());
					r.setHeight(b.h());
					r.relocate(b.x(), b.y());
				}
				case Region r -> r.resizeRelocate(b.x(), b.y(),
						b.w() == PREF ? r.prefWidth(-1) : Math.max(r.minWidth(-1), b.w()),
						b.h() == PREF ? r.prefHeight(-1) : Math.max(r.minHeight(-1), b.h()));
				default -> node.relocate(b.x(), b.y());
			}
		}
	}
}
