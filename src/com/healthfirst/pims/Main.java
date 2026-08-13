package com.healthfirst.pims;

import com.healthfirst.pims.ui.LoginFrame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Start the application on the Event Dispatch Thread (correct way for Swing)
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}