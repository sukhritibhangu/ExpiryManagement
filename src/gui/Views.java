package gui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

final class Views {
    private Views() { }

    static Button button(String text, Runnable action, String... style) {
        Button button = new Button(text);
        button.getStyleClass().addAll(style);
        button.setOnAction(event -> action.run());
        button.setOnMouseEntered(event -> scaleOnHover(button, 1.025));
        button.setOnMouseExited(event -> scaleOnHover(button, 1));
        return button;
    }

    private static void scaleOnHover(Button button, double scale) {
        if (button.getScene() == null
                || Boolean.TRUE.equals(button.getScene().getProperties().get("ledger.reducedMotion"))) {
            button.setScaleX(1);
            button.setScaleY(1);
            return;
        }
        animation.Motion.scale(button, button.getScaleX(), scale);
    }

    static Label label(String text, String... style) {
        Label label = new Label(text);
        label.getStyleClass().addAll(style);
        label.setWrapText(true);
        return label;
    }

    static Label badge(String text, String color) { return label(text, "badge", color); }

    static VBox card(String title, Node... nodes) {
        VBox card = new VBox();
        card.getStyleClass().add("card");
        if (!title.isBlank()) card.getChildren().add(label(title, "section"));
        card.getChildren().addAll(nodes);
        return card;
    }

    static HBox row(Node... nodes) {
        HBox box = new HBox(10, nodes);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    static Region space() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    static void tip(Button button, String description) {
        button.setTooltip(new Tooltip(description));
        button.setAccessibleText(description);
    }

    static SVGPath icon(String path) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.getStyleClass().add("icon");
        return svg;
    }
}
