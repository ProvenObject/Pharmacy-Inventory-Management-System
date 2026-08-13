package com.healthfirst.pims.ui;

import com.healthfirst.pims.db.DatabaseConnection;
import com.healthfirst.pims.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblMessage;

    public LoginFrame() {
        setTitle("HealthFirst Pharmacy - Login");
        setSize(400, 280);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);          // center on screen
        setResizable(false);

        // Main panel
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(new Color(240, 248, 255)); // light blue background

        // Title
        JLabel lblTitle = new JLabel("HealthFirst Pharmacy", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setBounds(50, 20, 300, 30);
        panel.add(lblTitle);

        JLabel lblSubtitle = new JLabel("Inventory Management System", SwingConstants.CENTER);
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSubtitle.setBounds(50, 45, 300, 20);
        panel.add(lblSubtitle);

        // Username
        JLabel lblUser = new JLabel("Username:");
        lblUser.setBounds(70, 90, 80, 25);
        panel.add(lblUser);

        txtUsername = new JTextField();
        txtUsername.setBounds(150, 90, 180, 25);
        panel.add(txtUsername);

        // Password
        JLabel lblPass = new JLabel("Password:");
        lblPass.setBounds(70, 130, 80, 25);
        panel.add(lblPass);

        txtPassword = new JPasswordField();
        txtPassword.setBounds(150, 130, 180, 25);
        panel.add(txtPassword);

        // Login button
        JButton btnLogin = new JButton("Login");
        btnLogin.setBounds(150, 175, 100, 30);
        btnLogin.setBackground(new Color(0, 120, 215));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        panel.add(btnLogin);

        // Message label (for errors)
        lblMessage = new JLabel("", SwingConstants.CENTER);
        lblMessage.setForeground(Color.RED);
        lblMessage.setBounds(50, 215, 300, 25);
        panel.add(lblMessage);

        add(panel);

        // Login button action
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                login();
            }
        });

        // Also allow pressing Enter
        getRootPane().setDefaultButton(btnLogin);
    }

    private void login() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            lblMessage.setText("Please enter username and password");
            return;
        }

        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, username);
            pst.setString(2, password);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                // Login successful
                User user = new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("full_name")
                );

                this.dispose(); // close login window

                if (user.getRole().equalsIgnoreCase("Admin")) {
                    new AdminDashboard(user).setVisible(true);
                } else {
                    new CashierDashboard(user).setVisible(true);
                }

            } else {
                lblMessage.setText("Invalid username or password");
                txtPassword.setText("");
            }

        } catch (Exception ex) {
            lblMessage.setText("Database error");
            ex.printStackTrace();
        }
    }
}