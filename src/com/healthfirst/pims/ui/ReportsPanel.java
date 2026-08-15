package com.healthfirst.pims.ui;

import com.healthfirst.pims.db.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

/**
 * ReportsPanel - Four required reports
 * 1. Sales Report
 * 2. Item-Wise Sales
 * 3. Low Stock
 * 4. Expiry (next 30 days)
 */
public class ReportsPanel extends JPanel {

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblTitle;

    public ReportsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(new Color(245, 247, 250));

        // Top buttons
        add(createButtonPanel(), BorderLayout.NORTH);

        // Title of current report
        lblTitle = new JLabel("Select a report", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(lblTitle, BorderLayout.CENTER);

        // Table
        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.getTableHeader().setReorderingAllowed(false);

        add(new JScrollPane(table), BorderLayout.SOUTH);
        // Adjust layout properly
        remove(lblTitle);
        JPanel center = new JPanel(new BorderLayout());
        center.add(lblTitle, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panel.setBackground(new Color(230, 240, 255));

        JButton btnSales = new JButton("Sales Report");
        JButton btnItemWise = new JButton("Item-Wise Sales");
        JButton btnLowStock = new JButton("Low Stock");
        JButton btnExpiry = new JButton("Expiry (Next 30 Days)");

        btnSales.addActionListener(e -> loadSalesReport());
        btnItemWise.addActionListener(e -> loadItemWiseReport());
        btnLowStock.addActionListener(e -> loadLowStockReport());
        btnExpiry.addActionListener(e -> loadExpiryReport());

        panel.add(btnSales);
        panel.add(btnItemWise);
        panel.add(btnLowStock);
        panel.add(btnExpiry);

        return panel;
    }

    // =========================================================
    // 1. SALES REPORT
    // =========================================================
    private void loadSalesReport() {
        lblTitle.setText("Sales Report - All Transactions");

        String[] columns = {"Sale ID", "Date", "Total Amount", "Cashier"};
        tableModel.setColumnIdentifiers(columns);
        tableModel.setRowCount(0);

        String sql = """
            SELECT s.sale_id, s.sale_date, s.total_amount, u.full_name
            FROM sales s
            LEFT JOIN users u ON s.user_id = u.user_id
            ORDER BY s.sale_date DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("sale_id"),
                        rs.getTimestamp("sale_date"),
                        String.format("R %.2f", rs.getDouble("total_amount")),
                        rs.getString("full_name")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // =========================================================
    // 2. ITEM-WISE SALES
    // =========================================================
    private void loadItemWiseReport() {
        lblTitle.setText("Item-Wise Sales Report");

        String[] columns = {"Medicine", "Total Qty Sold", "Total Revenue"};
        tableModel.setColumnIdentifiers(columns);
        tableModel.setRowCount(0);

        String sql = """
            SELECT m.name,
                   SUM(si.quantity_sold) AS total_qty,
                   SUM(si.quantity_sold * si.price_at_sale) AS revenue
            FROM sale_items si
            JOIN medicines m ON si.medicine_id = m.medicine_id
            GROUP BY m.medicine_id, m.name
            ORDER BY revenue DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("name"),
                        rs.getInt("total_qty"),
                        String.format("R %.2f", rs.getDouble("revenue"))
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // =========================================================
    // 3. LOW STOCK REPORT
    // =========================================================
    private void loadLowStockReport() {
        lblTitle.setText("Low Stock Report (Qty ≤ Reorder Level)");

        String[] columns = {"ID", "Medicine", "Current Qty", "Reorder Level", "Company"};
        tableModel.setColumnIdentifiers(columns);
        tableModel.setRowCount(0);

        String sql = """
            SELECT medicine_id, name, quantity_in_stock, reorder_level, company
            FROM medicines
            WHERE quantity_in_stock <= reorder_level
            ORDER BY quantity_in_stock ASC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("medicine_id"),
                        rs.getString("name"),
                        rs.getInt("quantity_in_stock"),
                        rs.getInt("reorder_level"),
                        rs.getString("company")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // =========================================================
    // 4. EXPIRY REPORT (next 30 days)
    // =========================================================
    private void loadExpiryReport() {
        lblTitle.setText("Medicines Expiring in the Next 30 Days");

        String[] columns = {"ID", "Medicine", "Expiry Date", "Days Left", "Qty in Stock"};
        tableModel.setColumnIdentifiers(columns);
        tableModel.setRowCount(0);

        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(30);

        String sql = """
            SELECT medicine_id, name, expiry_date, quantity_in_stock
            FROM medicines
            WHERE expiry_date BETWEEN ? AND ?
            ORDER BY expiry_date ASC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setDate(1, Date.valueOf(today));
            pst.setDate(2, Date.valueOf(limit));

            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Date expiry = rs.getDate("expiry_date");
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, expiry.toLocalDate());

                tableModel.addRow(new Object[]{
                        rs.getInt("medicine_id"),
                        rs.getString("name"),
                        expiry,
                        daysLeft,
                        rs.getInt("quantity_in_stock")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
}