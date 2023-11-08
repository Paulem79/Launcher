package fr.paulem.launcher.ui.panels.pages.content;

import fr.paulem.launcher.ui.panel.Panel;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

public abstract class ContentPanel extends Panel {
    @Override
    public void onShow() {
        FadeTransition transition = new FadeTransition(Duration.millis(250), this.layout);
        transition.setFromValue(0);
        transition.setToValue(1);
        transition.setAutoReverse(false);
        transition.play();
    }
}
