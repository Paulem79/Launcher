package fr.paulem.launcher.ui.panels.pages.content;

import fr.paulem.launcher.ui.panel.Panel;
import fr.paulem.launcher.utils.DirectFadeTransition;
import javafx.animation.FadeTransition;

public abstract class ContentPanel extends Panel {
    @Override
    public void onShow() {
        FadeTransition transition = DirectFadeTransition.getFadeTransition(this);
        transition.setAutoReverse(false);
        transition.play();
    }
}
