package net.paulem.launchermc.ui.components;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import net.paulem.launchermc.utils.MathUtils;

/**
 * A stylized gradient button with animated hover effects:
 * smooth shadow growth, color shift, and optional scale transition.
 * <p>
 * Uses a red-to-orange gradient by default, matching the launcher theme.
 */
public class GradientButton extends Button {

    private final DropShadow shadow;
    private final Timeline hoverIn;
    private final Timeline hoverOut;

    // Default theme colors
    private static final Color GRADIENT_START = Color.rgb(139, 0, 0);       // #8B0000
    private static final Color GRADIENT_END = Color.rgb(255, 69, 0);        // #FF4500
    private static final Color GRADIENT_START_HOVER = Color.rgb(160, 0, 0); // #A00000
    private static final Color GRADIENT_END_HOVER = Color.rgb(255, 87, 34); // #FF5722

    private static final Color SHADOW_COLOR = Color.rgb(139, 0, 0, 0.5);
    private static final Color SHADOW_COLOR_HOVER = Color.rgb(255, 69, 0, 0.6);

    /**
     * Create a gradient button with default styling.
     *
     * @param text           Button label
     * @param backgroundRadius CSS background radius value
     * @param scaleOnHover   Scale factor on hover (1.0 = no scale, 1.08 = grow 8%)
     */
    public GradientButton(String text, double backgroundRadius, double scaleOnHover) {
        super(text);
        this.setCache(false);
        this.setCacheHint(CacheHint.QUALITY);

        // ── Initial DropShadow ──
        shadow = new DropShadow();
        shadow.setColor(SHADOW_COLOR);
        shadow.setRadius(14);
        shadow.setSpread(0);
        shadow.setOffsetY(4);
        this.setEffect(shadow);

        // ── Animated hover property for gradient interpolation ──
        DoubleProperty hoverProgress = new SimpleDoubleProperty(0);
        hoverProgress.addListener((obs, oldVal, newVal) -> {
            double t = newVal.doubleValue();
            int r1 = MathUtils.lerp(GRADIENT_START.getRed(), GRADIENT_START_HOVER.getRed(), t);
            int g1 = MathUtils.lerp(GRADIENT_START.getGreen(), GRADIENT_START_HOVER.getGreen(), t);
            int b1 = MathUtils.lerp(GRADIENT_START.getBlue(), GRADIENT_START_HOVER.getBlue(), t);
            int r2 = MathUtils.lerp(GRADIENT_END.getRed(), GRADIENT_END_HOVER.getRed(), t);
            int g2 = MathUtils.lerp(GRADIENT_END.getGreen(), GRADIENT_END_HOVER.getGreen(), t);
            int b2 = MathUtils.lerp(GRADIENT_END.getBlue(), GRADIENT_END_HOVER.getBlue(), t);
            this.setStyle(String.format(
                    "-fx-background-color: linear-gradient(to right, rgb(%d,%d,%d), rgb(%d,%d,%d));" +
                    "-fx-background-radius: %.0f;",
                    r1, g1, b1, r2, g2, b2, backgroundRadius
            ));
        });

        Interpolator smooth = Interpolator.EASE_BOTH;
        Duration duration = Duration.millis(250);

        // ── Hover IN ──
        hoverIn = new Timeline(
                new KeyFrame(duration,
                        new KeyValue(this.scaleXProperty(), scaleOnHover, smooth),
                        new KeyValue(this.scaleYProperty(), scaleOnHover, smooth),
                        new KeyValue(shadow.radiusProperty(), 22, smooth),
                        new KeyValue(shadow.spreadProperty(), 0.12, smooth),
                        new KeyValue(shadow.offsetYProperty(), 6, smooth),
                        new KeyValue(shadow.colorProperty(), SHADOW_COLOR_HOVER, smooth),
                        new KeyValue(hoverProgress, 1.0, smooth)
                )
        );

        // ── Hover OUT ──
        hoverOut = new Timeline(
                new KeyFrame(duration,
                        new KeyValue(this.scaleXProperty(), 1.0, smooth),
                        new KeyValue(this.scaleYProperty(), 1.0, smooth),
                        new KeyValue(shadow.radiusProperty(), 14, smooth),
                        new KeyValue(shadow.spreadProperty(), 0, smooth),
                        new KeyValue(shadow.offsetYProperty(), 4, smooth),
                        new KeyValue(shadow.colorProperty(), SHADOW_COLOR, smooth),
                        new KeyValue(hoverProgress, 0.0, smooth)
                )
        );

        this.setOnMouseEntered(e -> {
            hoverOut.stop();
            hoverIn.playFromStart();
        });
        this.setOnMouseExited(e -> {
            hoverIn.stop();
            hoverOut.playFromStart();
        });
    }

    /**
     * Create a gradient button with default radius (14) and no scale on hover.
     */
    public GradientButton(String text) {
        this(text, 14, 1.0);
    }

    /**
     * Set the graphic (icon) for this button.
     */
    public GradientButton withGraphic(Node graphic) {
        this.setGraphic(graphic);
        return this;
    }

    /**
     * Override the default shadow values for larger buttons (e.g., Play button).
     */
    public GradientButton withLargeShadow() {
        shadow.setRadius(20);
        shadow.setOffsetY(6);

        // Update hover IN keyframes with larger values
        hoverIn.getKeyFrames().clear();
        Interpolator smooth = Interpolator.EASE_BOTH;
        Duration duration = Duration.millis(250);

        DoubleProperty hoverProgress = new SimpleDoubleProperty(0);
        hoverProgress.addListener((obs, oldVal, newVal) -> {
            double t = newVal.doubleValue();
            int r1 = MathUtils.lerp(GRADIENT_START.getRed(), GRADIENT_START_HOVER.getRed(), t);
            int g1 = MathUtils.lerp(GRADIENT_START.getGreen(), GRADIENT_START_HOVER.getGreen(), t);
            int b1 = MathUtils.lerp(GRADIENT_START.getBlue(), GRADIENT_START_HOVER.getBlue(), t);
            int r2 = MathUtils.lerp(GRADIENT_END.getRed(), GRADIENT_END_HOVER.getRed(), t);
            int g2 = MathUtils.lerp(GRADIENT_END.getGreen(), GRADIENT_END_HOVER.getGreen(), t);
            int b2 = MathUtils.lerp(GRADIENT_END.getBlue(), GRADIENT_END_HOVER.getBlue(), t);
            this.setStyle(String.format(
                    "-fx-background-color: linear-gradient(to right, rgb(%d,%d,%d), rgb(%d,%d,%d));" +
                    "-fx-background-radius: 25;",
                    r1, g1, b1, r2, g2, b2
            ));
        });

        hoverIn.getKeyFrames().add(new KeyFrame(duration,
                new KeyValue(this.scaleXProperty(), 1.08, smooth),
                new KeyValue(this.scaleYProperty(), 1.08, smooth),
                new KeyValue(shadow.radiusProperty(), 28, smooth),
                new KeyValue(shadow.spreadProperty(), 0.15, smooth),
                new KeyValue(shadow.offsetYProperty(), 8, smooth),
                new KeyValue(shadow.colorProperty(), SHADOW_COLOR_HOVER, smooth),
                new KeyValue(hoverProgress, 1.0, smooth)
        ));

        hoverOut.getKeyFrames().clear();
        hoverOut.getKeyFrames().add(new KeyFrame(duration,
                new KeyValue(this.scaleXProperty(), 1.0, smooth),
                new KeyValue(this.scaleYProperty(), 1.0, smooth),
                new KeyValue(shadow.radiusProperty(), 20, smooth),
                new KeyValue(shadow.spreadProperty(), 0, smooth),
                new KeyValue(shadow.offsetYProperty(), 6, smooth),
                new KeyValue(shadow.colorProperty(), SHADOW_COLOR, smooth),
                new KeyValue(hoverProgress, 0.0, smooth)
        ));

        return this;
    }

}

