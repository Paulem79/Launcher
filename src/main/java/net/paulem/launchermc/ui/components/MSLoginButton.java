package net.paulem.launchermc.ui.components;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.util.Duration;

import java.util.Locale;

public class MSLoginButton extends Button {
    private final Timeline hoverIn;
    private final Timeline hoverOut;

    // Propriétés pour stocker et animer les couleurs en temps réel
    private final ObjectProperty<Color> bgColor = new SimpleObjectProperty<>(Color.rgb(255, 255, 255, 0.07));
    private final ObjectProperty<Color> borderColor = new SimpleObjectProperty<>(Color.rgb(255, 255, 255, 0.08));

    public MSLoginButton() {
        // --- 1. Définition des couleurs ---
        Color bgNormal = Color.rgb(255, 255, 255, 0.07);
        Color bgHover = Color.rgb(255, 255, 255, 0.12);

        Color borderNormal = Color.rgb(255, 255, 255, 0.08);
        Color borderHover = Color.color(1.0, 0.27, 0.0, 0.3); // rgba(255, 69, 0, 0.3)
        Color shadowHover = Color.color(1.0, 0.27, 0.0, 0.15); // rgba(255, 69, 0, 0.15)

        // --- 2. Configuration de l'ombre (DropShadow) ---
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(0); // 0 par défaut (invisible)
        dropShadow.setColor(Color.TRANSPARENT);
        this.setEffect(dropShadow);

        // --- 3. Lier les propriétés au CSS inline ---
        Runnable updateStyle = () -> {
            this.setStyle(String.format(Locale.US,
                    "-fx-background-color: %s; -fx-border-color: %s;",
                    toCssColor(bgColor.get()),
                    toCssColor(borderColor.get())
            ));
        };

        bgColor.addListener(obs -> updateStyle.run());
        borderColor.addListener(obs -> updateStyle.run());
        updateStyle.run(); // Appliquer le style initial

        // --- 4. Création des animations ---
        Interpolator smooth = Interpolator.EASE_BOTH;
        Duration duration = Duration.millis(250);

        // ── Hover IN ──
        hoverIn = new Timeline(
                new KeyFrame(duration,
                        new KeyValue(bgColor, bgHover, smooth),
                        new KeyValue(borderColor, borderHover, smooth),
                        new KeyValue(dropShadow.radiusProperty(), 8.0, smooth),
                        new KeyValue(dropShadow.colorProperty(), shadowHover, smooth)
                )
        );

        // ── Hover OUT ──
        hoverOut = new Timeline(
                new KeyFrame(duration,
                        new KeyValue(bgColor, bgNormal, smooth),
                        new KeyValue(borderColor, borderNormal, smooth),
                        new KeyValue(dropShadow.radiusProperty(), 0.0, smooth),
                        new KeyValue(dropShadow.colorProperty(), Color.TRANSPARENT, smooth)
                )
        );

        // --- 5. Gestion des événements souris ---
        this.setOnMouseEntered(e -> {
            hoverOut.stop();
            hoverIn.playFromStart();
        });

        this.setOnMouseExited(e -> {
            hoverIn.stop();
            hoverOut.playFromStart();
        });
    }

    // Utilitaire pour convertir un objet Color JavaFX en string "rgba(r, g, b, a)" valide pour le CSS
    private String toCssColor(Color color) {
        return String.format(Locale.US, "rgba(%d, %d, %d, %f)",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255),
                color.getOpacity()
        );
    }

    public void setGraphic() {
        // --- 1. Création du Logo (Carrés) ---
        GridPane msLogoNode = new GridPane();
        msLogoNode.setHgap(2);
        msLogoNode.setVgap(2);

        double size = 10.0;
        Rectangle r1 = new Rectangle(size, size, Color.web("#F25022"));
        Rectangle r2 = new Rectangle(size, size, Color.web("#7FBA00"));
        Rectangle r3 = new Rectangle(size, size, Color.web("#00A4EF"));
        Rectangle r4 = new Rectangle(size, size, Color.web("#FFB900"));

        msLogoNode.add(r1, 0, 0);
        msLogoNode.add(r2, 1, 0);
        msLogoNode.add(r3, 0, 1);
        msLogoNode.add(r4, 1, 1);

        // --- 2. Création du Texte ---
        Text label = new Text("Microsoft");
        label.setFill(Color.web("#737373"));
        label.setStyle("-fx-font-family: 'Mundo-Sans-Std-Medium', 'Segoe UI', 'Roboto', sans-serif; -fx-font-size: 18px; -fx-font-weight: 800;");

        // CORRECTION : On change l'origine pour ignorer la ligne de base invisible
        label.setBoundsType(TextBoundsType.VISUAL);

        // --- 3. Assemblage dans un HBox ---
        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(msLogoNode, label);

        // OPTIONNEL : Si c'est encore décalé d'un pixel (selon la police), 
        // on remonte manuellement le texte :
        HBox.setMargin(label, new Insets(-2, 0, 0, 0)); 

        this.setGraphic(content);
    }
}