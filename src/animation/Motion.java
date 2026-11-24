package animation;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

public final class Motion {
    private Motion() { }

    public static void enter(Node node, boolean reduced, double offset, int delay) {
        if (reduced) return;
        node.setOpacity(0);
        node.setTranslateX(offset);
        FadeTransition fade = new FadeTransition(Duration.millis(200), node);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(220), node);
        slide.setToX(0);
        ParallelTransition transition = new ParallelTransition(fade, slide);
        transition.setDelay(Duration.millis(delay));
        transition.play();
    }

    public static void width(javafx.scene.layout.Region node, double width, boolean reduced) {
        if (reduced) {
            node.setPrefWidth(width);
            return;
        }
        new Timeline(new KeyFrame(Duration.millis(180),
                new KeyValue(node.prefWidthProperty(), width, Interpolator.EASE_BOTH))).play();
    }

    public static void count(javafx.scene.control.Label label, int target, boolean reduced) {
        if (reduced) {
            label.setText(Integer.toString(target));
            return;
        }
        var count = new javafx.beans.property.SimpleIntegerProperty();
        label.textProperty().bind(count.asString());
        new Timeline(new KeyFrame(Duration.millis(440), new KeyValue(count, target, Interpolator.EASE_OUT))).play();
    }

    public static void scale(Node node, double from, double to) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(150), node);
        transition.setFromX(from);
        transition.setFromY(from);
        transition.setToX(to);
        transition.setToY(to);
        transition.play();
    }
}
