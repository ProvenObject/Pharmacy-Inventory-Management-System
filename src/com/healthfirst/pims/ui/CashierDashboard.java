package com.healthfirst.pims.ui;

import com.healthfirst.pims.model.User;

import javax.swing.*;

public class CashierDashboard extends JFrame {

    public CashierDashboard(User user) {
        setTitle("Cashier POS - " + user.getFullName());
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel label = new JLabel("Welcome Cashier: " + user.getFullName(), SwingConstants.CENTER);
        label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 22));
        add(label);
    }
}