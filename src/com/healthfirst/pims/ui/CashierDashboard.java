package com.healthfirst.pims.ui;

import com.healthfirst.pims.db.DatabaseConnection;
import com.healthfirst.pims.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Vector;

/**
 * CashierDashboard - Full Point of Sale (POS) system
 * Features:
 * - Search medicine
 * - Add to cart
 * - Update quantity / remove from cart
 * - Checkout (saves sale + reduces stock)
 * - Generate bill
 * - Clear cart
 */
public class CashierDashboard extends JFrame {

    private User currentUser;

    // Search components
    private JTextField txtSearch;
    private JTextField txtQuantity;

    // Cart table
    private JTable cartTable;
    private DefaultTableModel cartModel;

    // Total
    private JLabel lblTotal;

    // We keep cart data in memory
    // Each row: medicineId, name, price, quantity, subtotal
    public CashierDashboard(User user) {
        this.currentUser = user;

        setTitle("HealthFirst PIMS - Cashier POS | " + user.getFullName());
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main layout
        setLayout(new BorderLayout(10, 10));

        // ===== TOP: Search Panel =====
        add(createSearchPanel(), BorderLayout.NORTH);

        // ===== CENTER: Cart Table =====
        add(createCartPanel(), BorderLayout.CENTER);

        // ===== BOTTOM: Total + Buttons =====
        add(createBottomPanel(), BorderLayout.SOUTH);
    }

    // =========================================================
    // SEARCH PANEL
    // =========================================================
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12));
        panel.setBackground(new Color(230, 240, 255));
        panel.setBorder(BorderFactory.createTitledBorder("Add Medicine to Cart"));

        panel.add(new JLabel("Search (Name or ID):"));
        txtSearch = new JTextField(20);
        panel.add(txtSearch);

        panel.add(new JLabel("Qty:"));
        txtQuantity = new JTextField("1", 5);
        panel.add(txtQuantity);

        JButton btnAdd = new JButton("Add to Cart");
        btnAdd.setBackground(new Color(0, 120, 215));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);
        btnAdd.addActionListener(e -> addToCart());
        panel.add(btnAdd);

        JButton btnClearSearch = new JButton("Clear");
        btnClearSearch.addActionListener(e -> {
            txtSearch.setText("");
            txtQuantity.setText("1");
        });
        panel.add(btnClearSearch);

        return panel;
    }

    // =========================================================
    // CART TABLE
    // =========================================================
    private JScrollPane createCartPanel() {
        String[] columns = {"ID", "Medicine", "Unit Price", "Qty", "Subtotal"};
        cartModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        cartTable = new JTable(cartModel);
        cartTable.setRowHeight(28);
        cartTable.getTableHeader().setReorderingAllowed(false);

        return new JScrollPane(cartTable);
    }

    // =========================================================
    // BOTTOM PANEL (Total + Buttons)
    // =========================================================
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        panel.setBackground(new Color(245, 247, 250));

        // Total label
        lblTotal = new JLabel("Total: R 0.00");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTotal.setForeground(new Color(0, 100, 0));
        panel.add(lblTotal, BorderLayout.WEST);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 5));
        buttonPanel.setBackground(new Color(245, 247, 250));

        JButton btnRemove = new JButton("Remove Selected");
        JButton btnClearCart = new JButton("Clear Cart");
        JButton btnCheckout = new JButton("Checkout & Generate Bill");

        btnCheckout.setBackground(new Color(0, 150, 0));
        btnCheckout.setForeground(Color.WHITE);
        btnCheckout.setFocusPainted(false);
        btnCheckout.setFont(new Font("Segoe UI", Font.BOLD, 14));

        btnRemove.addActionListener(e -> removeSelectedItem());
        btnClearCart.addActionListener(e -> clearCart());
        btnCheckout.addActionListener(e -> checkout());

        buttonPanel.add(btnRemove);
        buttonPanel.add(btnClearCart);
        buttonPanel.add(btnCheckout);

        panel.add(buttonPanel, BorderLayout.EAST);
        return panel;
    }

    // =========================================================
    // ADD TO CART
    // =========================================================
    private void addToCart() {
        String search = txtSearch.getText().trim();
        if (search.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter medicine name or ID");
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(txtQuantity.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quantity must be a positive number");
            return;
        }

        // Search in database
        String sql = "SELECT * FROM medicines WHERE medicine_id = ? OR name LIKE ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            // Try as ID first
            try {
                pst.setInt(1, Integer.parseInt(search));
            } catch (NumberFormatException e) {
                pst.setInt(1, -1); // impossible ID
            }
            pst.setString(2, "%" + search + "%");

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("medicine_id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                int stock = rs.getInt("quantity_in_stock");

                if (qty > stock) {
                    JOptionPane.showMessageDialog(this,
                            "Not enough stock!\nAvailable: " + stock);
                    return;
                }

                // Check if already in cart → increase quantity
                for (int i = 0; i < cartModel.getRowCount(); i++) {
                    int existingId = (int) cartModel.getValueAt(i, 0);
                    if (existingId == id) {
                        int oldQty = (int) cartModel.getValueAt(i, 3);
                        int newQty = oldQty + qty;

                        if (newQty > stock) {
                            JOptionPane.showMessageDialog(this, "Not enough stock for this quantity");
                            return;
                        }

                        cartModel.setValueAt(newQty, i, 3);
                        cartModel.setValueAt(price * newQty, i, 4);
                        updateTotal();
                        txtSearch.setText("");
                        txtQuantity.setText("1");
                        return;
                    }
                }

                // Add new row to cart
                double subtotal = price * qty;
                cartModel.addRow(new Object[]{id, name, price, qty, subtotal});
                updateTotal();

                txtSearch.setText("");
                txtQuantity.setText("1");

            } else {
                JOptionPane.showMessageDialog(this, "Medicine not found");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
    }

    // =========================================================
    // UPDATE TOTAL
    // =========================================================
    private void updateTotal() {
        double total = 0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            total += (double) cartModel.getValueAt(i, 4);
        }
        lblTotal.setText(String.format("Total: R %.2f", total));
    }

    // =========================================================
    // REMOVE SELECTED
    // =========================================================
    private void removeSelectedItem() {
        int row = cartTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an item to remove");
            return;
        }
        cartModel.removeRow(row);
        updateTotal();
    }

    // =========================================================
    // CLEAR CART
    // =========================================================
    private void clearCart() {
        if (cartModel.getRowCount() == 0) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Clear the entire cart?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            cartModel.setRowCount(0);
            updateTotal();
        }
    }

    // =========================================================
    // CHECKOUT + SAVE SALE + REDUCE STOCK + SHOW BILL
    // =========================================================
    private void checkout() {
        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Cart is empty");
            return;
        }

        double total = 0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            total += (double) cartModel.getValueAt(i, 4);
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Confirm sale of R %.2f ?", total),
                "Confirm Checkout", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Insert into sales table
            String saleSql = "INSERT INTO sales (total_amount, user_id) VALUES (?, ?)";
            PreparedStatement salePst = conn.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS);
            salePst.setDouble(1, total);
            salePst.setInt(2, currentUser.getUserId());
            salePst.executeUpdate();

            ResultSet keys = salePst.getGeneratedKeys();
            keys.next();
            int saleId = keys.getInt(1);

            // 2. Insert each item into sale_items + reduce stock
            String itemSql = "INSERT INTO sale_items (sale_id, medicine_id, quantity_sold, price_at_sale) VALUES (?, ?, ?, ?)";
            String stockSql = "UPDATE medicines SET quantity_in_stock = quantity_in_stock - ? WHERE medicine_id = ?";

            PreparedStatement itemPst = conn.prepareStatement(itemSql);
            PreparedStatement stockPst = conn.prepareStatement(stockSql);

            for (int i = 0; i < cartModel.getRowCount(); i++) {
                int medId = (int) cartModel.getValueAt(i, 0);
                double price = (double) cartModel.getValueAt(i, 2);
                int qty = (int) cartModel.getValueAt(i, 3);

                // sale_items
                itemPst.setInt(1, saleId);
                itemPst.setInt(2, medId);
                itemPst.setInt(3, qty);
                itemPst.setDouble(4, price);
                itemPst.executeUpdate();

                // reduce stock
                stockPst.setInt(1, qty);
                stockPst.setInt(2, medId);
                stockPst.executeUpdate();
            }

            conn.commit(); // Everything succeeded

            // Show bill
            showBill(saleId, total);

            // Clear cart
            cartModel.setRowCount(0);
            updateTotal();

            JOptionPane.showMessageDialog(this, "Sale completed successfully!");

        } catch (Exception e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            JOptionPane.showMessageDialog(this, "Checkout failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // =========================================================
    // SHOW BILL WINDOW
    // =========================================================
    private void showBill(int saleId, double total) {
        JDialog billDialog = new JDialog(this, "Bill / Receipt", true);
        billDialog.setSize(420, 500);
        billDialog.setLocationRelativeTo(this);

        JTextArea billArea = new JTextArea();
        billArea.setEditable(false);
        billArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        billArea.setMargin(new Insets(15, 15, 15, 15));

        StringBuilder sb = new StringBuilder();
        sb.append("====================================\n");
        sb.append("       HEALTHFIRST PHARMACY\n");
        sb.append("     Inventory Management System\n");
        sb.append("====================================\n\n");
        sb.append("Sale ID   : ").append(saleId).append("\n");
        sb.append("Date      : ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        sb.append("Cashier   : ").append(currentUser.getFullName()).append("\n");
        sb.append("------------------------------------\n");
        sb.append(String.format("%-20s %5s %8s\n", "Item", "Qty", "Amount"));
        sb.append("------------------------------------\n");

        for (int i = 0; i < cartModel.getRowCount(); i++) {
            String name = cartModel.getValueAt(i, 1).toString();
            if (name.length() > 18) name = name.substring(0, 18);
            int qty = (int) cartModel.getValueAt(i, 3);
            double sub = (double) cartModel.getValueAt(i, 4);
            sb.append(String.format("%-20s %5d %8.2f\n", name, qty, sub));
        }

        sb.append("------------------------------------\n");
        sb.append(String.format("%-26s %8.2f\n", "TOTAL:", total));
        sb.append("====================================\n");
        sb.append("     Thank you for your purchase!\n");
        sb.append("====================================\n");

        billArea.setText(sb.toString());
        billDialog.add(new JScrollPane(billArea));

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> billDialog.dispose());
        JPanel bottom = new JPanel();
        bottom.add(btnClose);
        billDialog.add(bottom, BorderLayout.SOUTH);

        billDialog.setVisible(true);
    }
}