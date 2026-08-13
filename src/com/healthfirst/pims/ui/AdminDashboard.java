package com.healthfirst.pims.ui;

import com.healthfirst.pims.model.User;

import javax.swing.*;
import java.awt.*;

/**
 * AdminDashboard - Main window for the Administrator.
 * Contains tabs for:
 * 1. Medicines
 * 2. Suppliers
 * 3. Users
 * 4. Reports
 */
public class AdminDashboard extends JFrame {

    private User currentUser;   // The admin who is currently logged in

    public AdminDashboard(User user) {
        this.currentUser = user;

        // ===== WINDOW SETTINGS =====
        setTitle("HealthFirst PIMS - Admin Dashboard | Logged in as: " + user.getFullName());
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);               // Center the window on screen

        // ===== CREATE THE TABBED PANE =====
        // JTabbedPane allows us to have multiple "pages" (tabs) in one window
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // ----- Tab 1: Medicines -----
        JPanel medicinesPanel = createPlaceholderPanel("Medicines Management (Coming next)");
        tabbedPane.addTab("Medicines", medicinesPanel);

        // ----- Tab 2: Suppliers -----
        JPanel suppliersPanel = createPlaceholderPanel("Suppliers Management (Coming soon)");
        tabbedPane.addTab("Suppliers", suppliersPanel);

        // ----- Tab 3: Users -----
        JPanel usersPanel = createPlaceholderPanel("User Management (Coming soon)");
        tabbedPane.addTab("Users", usersPanel);

        // ----- Tab 4: Reports -----
        JPanel reportsPanel = createPlaceholderPanel("Reports (Coming soon)");
        tabbedPane.addTab("Reports", reportsPanel);

        // Add the tabbed pane to the window
        add(tabbedPane);

        // Optional: Add a logout button later at the bottom or top
    }

    /**
     * Helper method to create a simple placeholder panel.
     * We will replace these later with real content.
     */
    private JPanel createPlaceholderPanel(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 247, 250));

        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 22));
        label.setForeground(new Color(80, 80, 80));

        panel.add(label, BorderLayout.CENTER);
        return panel;
    }
}