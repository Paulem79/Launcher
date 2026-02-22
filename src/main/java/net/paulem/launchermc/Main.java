package net.paulem.launchermc;

import javafx.application.Application;

import javax.swing.*;

public final class Main {
    public static void main(String[] args) {
        try {
            Class.forName("javafx.application.Application");
        } catch (ClassNotFoundException e) {
            // Mauvaise version de java
            JOptionPane.showMessageDialog(
                    null,
                    "Votre version de Java ne semble pas être la bonne !",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE
            );
        }
        Application.launch(Launcher.class, args);
    }
}
