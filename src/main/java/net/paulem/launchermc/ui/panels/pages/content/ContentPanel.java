package net.paulem.launchermc.ui.panels.pages.content;

import net.paulem.launchermc.ui.panels.Panel;
import javafx.scene.layout.GridPane;

/**
 * This class represent all pages that has content, useful to make animations on their open/close
 */
public abstract class ContentPanel extends Panel {
    protected final GridPane contentPane = new GridPane();
}
