package fr.paulem.launcher.utils;

import fr.paulem.launcher.ui.panel.IPanel;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

public class DirectFadeTransition {
    public static FadeTransition getFadeTransition(IPanel panel){
        FadeTransition transition = new FadeTransition(Duration.millis(250), panel.getLayout());
        transition.setFromValue(0);
        transition.setToValue(1);
        transition.setAutoReverse(true);
        return transition;
    }
}
