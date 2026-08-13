package com.healthfirst.pims.ui;

import com.healthfirst.pims.model.User;

import javax.swing.*;

public class AdminDashboard extends JFrame {

    public AdminDashboard(User user) {
        setTitle("Admin Dashboard - " + user.getFullName());
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel label = new JLabel("Welcome Admin: " + user.getFullName(), SwingConstants.CENTER);
        label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 22));
        add(label);
    }
}