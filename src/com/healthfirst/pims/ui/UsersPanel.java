package com.healthfirst.pims.ui;

import com.healthfirst.pims.db.DatabaseConnection;
import com.healthfirst.pims.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * UsersPanel - Manage Cashier accounts (and view Admins)
 * Admins can create, update and delete Cashier users.
 */
public class UsersPanel extends JPanel {

    private JTextField txtUsername, txtFullName;
    private JPasswordField txtPassword;
    private JComboBox<String> cmbRole;

    private JTable table;
    private DefaultTableModel tableModel;

    private int selectedUserId = -1;

    public UsersPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(new Color(245, 247, 250));

        add(createFormPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);

        loadUsers();
    }

    // =========================================================
    // FORM
    // =========================================================
    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("User Details"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 1
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        txtUsername = new JTextField(15);
        panel.add(txtUsername, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 3;
        txtFullName = new JTextField(15);
        panel.add(txtFullName, gbc);

        // Row 2
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        txtPassword = new JPasswordField(15);
        panel.add(txtPassword, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 3;
        cmbRole = new JComboBox<>(new String[]{"Cashier", "Admin"});
        panel.add(cmbRole, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setBackground(Color.WHITE);

        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear = new JButton("Clear");
        JButton btnRefresh = new JButton("Refresh");

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnClear);
        buttonPanel.add(btnRefresh);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 4;
        panel.add(buttonPanel, gbc);

        // Actions
        btnAdd.addActionListener(e -> addUser());
        btnUpdate.addActionListener(e -> updateUser());
        btnDelete.addActionListener(e -> deleteUser());
        btnClear.addActionListener(e -> clearForm());
        btnRefresh.addActionListener(e -> loadUsers());

        return panel;
    }

    // =========================================================
    // TABLE
    // =========================================================
    private JScrollPane createTablePanel() {
        String[] columns = {"ID", "Username", "Full Name", "Role"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                fillFormFromTable();
            }
        });

        return new JScrollPane(table);
    }

    // =========================================================
    // LOAD USERS
    // =========================================================
    private void loadUsers() {
        tableModel.setRowCount(0);

        String sql = "SELECT user_id, username, full_name, role FROM users ORDER BY role, username";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading users: " + e.getMessage());
        }
    }

    // =========================================================
    // FILL FORM
    // =========================================================
    private void fillFormFromTable() {
        int row = table.getSelectedRow();
        selectedUserId = (int) tableModel.getValueAt(row, 0);

        txtUsername.setText(tableModel.getValueAt(row, 1).toString());
        txtFullName.setText(tableModel.getValueAt(row, 2).toString());
        cmbRole.setSelectedItem(tableModel.getValueAt(row, 3).toString());

        // Password is not shown for security – leave empty
        txtPassword.setText("");
    }

    // =========================================================
    // CLEAR
    // =========================================================
    private void clearForm() {
        selectedUserId = -1;
        txtUsername.setText("");
        txtFullName.setText("");
        txtPassword.setText("");
        cmbRole.setSelectedIndex(0);
        table.clearSelection();
    }

    // =========================================================
    // ADD USER
    // =========================================================
    private void addUser() {
        String username = txtUsername.getText().trim();
        String fullName = txtFullName.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();
        String role = cmbRole.getSelectedItem().toString();

        if (username.isEmpty() || fullName.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username, Full Name and Password are required");
            return;
        }

        String sql = "INSERT INTO users (username, password, role, full_name) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, username);
            pst.setString(2, password);
            pst.setString(3, role);
            pst.setString(4, fullName);

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "User added successfully!");
            clearForm();
            loadUsers();

        } catch (SQLException e) {
            if (e.getMessage().toLowerCase().contains("duplicate")) {
                JOptionPane.showMessageDialog(this, "Username already exists");
            } else {
                JOptionPane.showMessageDialog(this, "Error adding user: " + e.getMessage());
            }
        }
    }

    // =========================================================
    // UPDATE USER
    // =========================================================
    private void updateUser() {
        if (selectedUserId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user first");
            return;
        }

        String username = txtUsername.getText().trim();
        String fullName = txtFullName.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();
        String role = cmbRole.getSelectedItem().toString();

        if (username.isEmpty() || fullName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and Full Name are required");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {

            if (password.isEmpty()) {
                // Update without changing password
                String sql = "UPDATE users SET username=?, role=?, full_name=? WHERE user_id=?";
                try (PreparedStatement pst = conn.prepareStatement(sql)) {
                    pst.setString(1, username);
                    pst.setString(2, role);
                    pst.setString(3, fullName);
                    pst.setInt(4, selectedUserId);
                    pst.executeUpdate();
                }
            } else {
                // Update including password
                String sql = "UPDATE users SET username=?, password=?, role=?, full_name=? WHERE user_id=?";
                try (PreparedStatement pst = conn.prepareStatement(sql)) {
                    pst.setString(1, username);
                    pst.setString(2, password);
                    pst.setString(3, role);
                    pst.setString(4, fullName);
                    pst.setInt(5, selectedUserId);
                    pst.executeUpdate();
                }
            }

            JOptionPane.showMessageDialog(this, "User updated successfully!");
            clearForm();
            loadUsers();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating user: " + e.getMessage());
        }
    }

    // =========================================================
    // DELETE USER
    // =========================================================
    private void deleteUser() {
        if (selectedUserId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user first");
            return;
        }

        // Prevent deleting yourself (basic safety)
        String selectedUsername = tableModel.getValueAt(table.getSelectedRow(), 1).toString();
        if (selectedUsername.equalsIgnoreCase("admin")) {
            JOptionPane.showMessageDialog(this, "You cannot delete the main admin account");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this user?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, selectedUserId);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "User deleted successfully!");
            clearForm();
            loadUsers();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error deleting user: " + e.getMessage());
        }
    }
}