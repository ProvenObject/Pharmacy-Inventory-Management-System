package com.healthfirst.pims.ui;

import com.healthfirst.pims.db.DatabaseConnection;
import com.healthfirst.pims.model.Supplier;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * SuppliersPanel - Full CRUD for suppliers
 * Very similar structure to MedicinesPanel
 */
public class SuppliersPanel extends JPanel {

    // ===== FORM FIELDS =====
    private JTextField txtName, txtContactPerson, txtPhone, txtEmail;
    private JTextArea txtAddress;

    // ===== TABLE =====
    private JTable table;
    private DefaultTableModel tableModel;

    private int selectedSupplierId = -1;

    public SuppliersPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(new Color(245, 247, 250));

        add(createFormPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);

        loadSuppliers();
    }

    // =========================================================
    // FORM PANEL
    // =========================================================
    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Supplier Details"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 1
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        txtName = new JTextField(18);
        panel.add(txtName, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Contact Person:"), gbc);
        gbc.gridx = 3;
        txtContactPerson = new JTextField(18);
        panel.add(txtContactPerson, gbc);

        // Row 2
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1;
        txtPhone = new JTextField(18);
        panel.add(txtPhone, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 3;
        txtEmail = new JTextField(18);
        panel.add(txtEmail, gbc);

        // Row 3 - Address
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        txtAddress = new JTextArea(3, 20);
        txtAddress.setLineWrap(true);
        txtAddress.setWrapStyleWord(true);
        JScrollPane addressScroll = new JScrollPane(txtAddress);
        panel.add(addressScroll, gbc);
        gbc.gridwidth = 1; // reset

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

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 4;
        panel.add(buttonPanel, gbc);

        // Button actions
        btnAdd.addActionListener(e -> addSupplier());
        btnUpdate.addActionListener(e -> updateSupplier());
        btnDelete.addActionListener(e -> deleteSupplier());
        btnClear.addActionListener(e -> clearForm());
        btnRefresh.addActionListener(e -> loadSuppliers());

        return panel;
    }

    // =========================================================
    // TABLE
    // =========================================================
    private JScrollPane createTablePanel() {
        String[] columns = {"ID", "Name", "Contact Person", "Phone", "Email", "Address"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // Click row → fill form
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                fillFormFromTable();
            }
        });

        return new JScrollPane(table);
    }

    // =========================================================
    // LOAD SUPPLIERS
    // =========================================================
    private void loadSuppliers() {
        tableModel.setRowCount(0);

        String sql = "SELECT * FROM suppliers ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("supplier_id"),
                        rs.getString("name"),
                        rs.getString("contact_person"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("address")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading suppliers: " + e.getMessage());
        }
    }

    // =========================================================
    // FILL FORM
    // =========================================================
    private void fillFormFromTable() {
        int row = table.getSelectedRow();
        selectedSupplierId = (int) tableModel.getValueAt(row, 0);

        txtName.setText(tableModel.getValueAt(row, 1).toString());
        txtContactPerson.setText(tableModel.getValueAt(row, 2) != null ? tableModel.getValueAt(row, 2).toString() : "");
        txtPhone.setText(tableModel.getValueAt(row, 3) != null ? tableModel.getValueAt(row, 3).toString() : "");
        txtEmail.setText(tableModel.getValueAt(row, 4) != null ? tableModel.getValueAt(row, 4).toString() : "");
        txtAddress.setText(tableModel.getValueAt(row, 5) != null ? tableModel.getValueAt(row, 5).toString() : "");
    }

    // =========================================================
    // CLEAR FORM
    // =========================================================
    private void clearForm() {
        selectedSupplierId = -1;
        txtName.setText("");
        txtContactPerson.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        txtAddress.setText("");
        table.clearSelection();
    }

    // =========================================================
    // ADD
    // =========================================================
    private void addSupplier() {
        if (txtName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Supplier name is required");
            return;
        }

        String sql = "INSERT INTO suppliers (name, contact_person, phone, email, address) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, txtName.getText().trim());
            pst.setString(2, txtContactPerson.getText().trim());
            pst.setString(3, txtPhone.getText().trim());
            pst.setString(4, txtEmail.getText().trim());
            pst.setString(5, txtAddress.getText().trim());

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Supplier added successfully!");
            clearForm();
            loadSuppliers();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding supplier: " + e.getMessage());
        }
    }

    // =========================================================
    // UPDATE
    // =========================================================
    private void updateSupplier() {
        if (selectedSupplierId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a supplier first");
            return;
        }
        if (txtName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Supplier name is required");
            return;
        }

        String sql = "UPDATE suppliers SET name=?, contact_person=?, phone=?, email=?, address=? WHERE supplier_id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, txtName.getText().trim());
            pst.setString(2, txtContactPerson.getText().trim());
            pst.setString(3, txtPhone.getText().trim());
            pst.setString(4, txtEmail.getText().trim());
            pst.setString(5, txtAddress.getText().trim());
            pst.setInt(6, selectedSupplierId);

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Supplier updated successfully!");
            clearForm();
            loadSuppliers();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating supplier: " + e.getMessage());
        }
    }

    // =========================================================
    // DELETE
    // =========================================================
    private void deleteSupplier() {
        if (selectedSupplierId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a supplier first");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this supplier?\n(Medicines linked to this supplier will have supplier set to NULL)",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM suppliers WHERE supplier_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, selectedSupplierId);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Supplier deleted successfully!");
            clearForm();
            loadSuppliers();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error deleting supplier: " + e.getMessage());
        }
    }
}