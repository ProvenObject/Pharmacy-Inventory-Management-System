package com.healthfirst.pims.ui;

import com.healthfirst.pims.db.DatabaseConnection;
import com.healthfirst.pims.model.Medicine;
import com.healthfirst.pims.model.Supplier;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * MedicinesPanel - Full CRUD for medicines
 * "Medicines" tab of AdminDashboard.
 */
public class MedicinesPanel extends JPanel {

    // ===== FORM FIELDS =====
    private JTextField txtName, txtCompany, txtPrice, txtQuantity, txtReorderLevel, txtExpiry;
    private JComboBox<String> cmbType;
    private JComboBox<Supplier> cmbSupplier;

    // ===== TABLE =====
    private JTable table;
    private DefaultTableModel tableModel;

    // Currently selected medicine ID (for Update / Delete)
    private int selectedMedicineId = -1;

    public MedicinesPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(new Color(245, 247, 250));

        // 1. Create the top form panel
        add(createFormPanel(), BorderLayout.NORTH);

        // 2. Create the table in the center
        add(createTablePanel(), BorderLayout.CENTER);

        // 3. Load data when the panel opens
        loadSuppliers();
        loadMedicines();
    }

    // =========================================================
    // FORM PANEL (top part)
    // =========================================================
    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Medicine Details"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ----- Row 1 -----
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        txtName = new JTextField(15);
        panel.add(txtName, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Company:"), gbc);
        gbc.gridx = 3;
        txtCompany = new JTextField(15);
        panel.add(txtCompany, gbc);

        // ----- Row 2 -----
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        cmbType = new JComboBox<>(new String[]{"Tablet", "Capsule", "Syrup", "Injection", "Cream", "Other"});
        panel.add(cmbType, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Price (R):"), gbc);
        gbc.gridx = 3;
        txtPrice = new JTextField(10);
        panel.add(txtPrice, gbc);

        // ----- Row 3 -----
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        txtQuantity = new JTextField(10);
        panel.add(txtQuantity, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Reorder Level:"), gbc);
        gbc.gridx = 3;
        txtReorderLevel = new JTextField(10);
        panel.add(txtReorderLevel, gbc);

        // ----- Row 4 -----
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Expiry (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        txtExpiry = new JTextField(10);
        panel.add(txtExpiry, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Supplier:"), gbc);
        gbc.gridx = 3;
        cmbSupplier = new JComboBox<>();
        panel.add(cmbSupplier, gbc);

        // ----- Buttons Row -----
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

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 4;
        panel.add(buttonPanel, gbc);

        // ===== BUTTON ACTIONS =====
        btnAdd.addActionListener(e -> addMedicine());
        btnUpdate.addActionListener(e -> updateMedicine());
        btnDelete.addActionListener(e -> deleteMedicine());
        btnClear.addActionListener(e -> clearForm());
        btnRefresh.addActionListener(e -> loadMedicines());

        return panel;
    }

    // =========================================================
    // TABLE PANEL (center)
    // =========================================================
    private JScrollPane createTablePanel() {
        String[] columns = {"ID", "Name", "Company", "Type", "Price", "Qty", "Reorder", "Expiry", "Supplier ID"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // When user clicks a row → fill the form
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                fillFormFromTable();
            }
        });

        return new JScrollPane(table);
    }

    // =========================================================
    // LOAD SUPPLIERS INTO COMBOBOX
    // =========================================================
    private void loadSuppliers() {
        cmbSupplier.removeAllItems();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM suppliers ORDER BY name")) {

            while (rs.next()) {
                Supplier s = new Supplier(
                        rs.getInt("supplier_id"),
                        rs.getString("name"),
                        rs.getString("contact_person"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("address")
                );
                cmbSupplier.addItem(s);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading suppliers: " + e.getMessage());
        }
    }

    // =========================================================
    // LOAD ALL MEDICINES INTO TABLE
    // =========================================================
    private void loadMedicines() {
        tableModel.setRowCount(0); // Clear existing rows

        String sql = "SELECT * FROM medicines ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("medicine_id"),
                        rs.getString("name"),
                        rs.getString("company"),
                        rs.getString("medicine_type"),
                        rs.getDouble("price"),
                        rs.getInt("quantity_in_stock"),
                        rs.getInt("reorder_level"),
                        rs.getDate("expiry_date"),
                        rs.getInt("supplier_id")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading medicines: " + e.getMessage());
        }
    }

    // =========================================================
    // FILL FORM WHEN USER CLICKS A ROW
    // =========================================================
    private void fillFormFromTable() {
        int row = table.getSelectedRow();
        selectedMedicineId = (int) tableModel.getValueAt(row, 0);

        txtName.setText(tableModel.getValueAt(row, 1).toString());
        txtCompany.setText(tableModel.getValueAt(row, 2).toString());
        cmbType.setSelectedItem(tableModel.getValueAt(row, 3).toString());
        txtPrice.setText(tableModel.getValueAt(row, 4).toString());
        txtQuantity.setText(tableModel.getValueAt(row, 5).toString());
        txtReorderLevel.setText(tableModel.getValueAt(row, 6).toString());
        txtExpiry.setText(tableModel.getValueAt(row, 7).toString());

        // Select the correct supplier in the combo box
        int supplierId = (int) tableModel.getValueAt(row, 8);
        for (int i = 0; i < cmbSupplier.getItemCount(); i++) {
            if (cmbSupplier.getItemAt(i).getSupplierId() == supplierId) {
                cmbSupplier.setSelectedIndex(i);
                break;
            }
        }
    }

    // =========================================================
    // CLEAR FORM
    // =========================================================
    private void clearForm() {
        selectedMedicineId = -1;
        txtName.setText("");
        txtCompany.setText("");
        cmbType.setSelectedIndex(0);
        txtPrice.setText("");
        txtQuantity.setText("");
        txtReorderLevel.setText("");
        txtExpiry.setText("");
        if (cmbSupplier.getItemCount() > 0) cmbSupplier.setSelectedIndex(0);
        table.clearSelection();
    }

    // =========================================================
    // ADD NEW MEDICINE
    // =========================================================
    private void addMedicine() {
        if (!validateForm()) return;

        String sql = "INSERT INTO medicines (name, company, medicine_type, price, quantity_in_stock, reorder_level, expiry_date, supplier_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, txtName.getText().trim());
            pst.setString(2, txtCompany.getText().trim());
            pst.setString(3, cmbType.getSelectedItem().toString());
            pst.setDouble(4, Double.parseDouble(txtPrice.getText().trim()));
            pst.setInt(5, Integer.parseInt(txtQuantity.getText().trim()));
            pst.setInt(6, Integer.parseInt(txtReorderLevel.getText().trim()));
            pst.setDate(7, Date.valueOf(txtExpiry.getText().trim()));
            pst.setInt(8, ((Supplier) cmbSupplier.getSelectedItem()).getSupplierId());

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Medicine added successfully!");
            clearForm();
            loadMedicines();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding medicine: " + e.getMessage());
        }
    }

    // =========================================================
    // UPDATE MEDICINE
    // =========================================================
    private void updateMedicine() {
        if (selectedMedicineId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a medicine first");
            return;
        }
        if (!validateForm()) return;

        String sql = "UPDATE medicines SET name=?, company=?, medicine_type=?, price=?, quantity_in_stock=?, reorder_level=?, expiry_date=?, supplier_id=? WHERE medicine_id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, txtName.getText().trim());
            pst.setString(2, txtCompany.getText().trim());
            pst.setString(3, cmbType.getSelectedItem().toString());
            pst.setDouble(4, Double.parseDouble(txtPrice.getText().trim()));
            pst.setInt(5, Integer.parseInt(txtQuantity.getText().trim()));
            pst.setInt(6, Integer.parseInt(txtReorderLevel.getText().trim()));
            pst.setDate(7, Date.valueOf(txtExpiry.getText().trim()));
            pst.setInt(8, ((Supplier) cmbSupplier.getSelectedItem()).getSupplierId());
            pst.setInt(9, selectedMedicineId);

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Medicine updated successfully!");
            clearForm();
            loadMedicines();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating medicine: " + e.getMessage());
        }
    }

    // =========================================================
    // DELETE MEDICINE
    // =========================================================
    private void deleteMedicine() {
        if (selectedMedicineId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a medicine first");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this medicine?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM medicines WHERE medicine_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, selectedMedicineId);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Medicine deleted successfully!");
            clearForm();
            loadMedicines();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error deleting medicine: " + e.getMessage());
        }
    }

    // =========================================================
    // SIMPLE VALIDATION
    // =========================================================
    private boolean validateForm() {
        if (txtName.getText().trim().isEmpty() ||
                txtPrice.getText().trim().isEmpty() ||
                txtQuantity.getText().trim().isEmpty() ||
                txtExpiry.getText().trim().isEmpty()) {

            JOptionPane.showMessageDialog(this, "Please fill in all required fields");
            return false;
        }
        return true;
    }
}