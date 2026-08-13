package com.healthfirst.pims.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/pims";
    private static final String USER = "root";
    private static final String PASSWORD = "TK@Dev.local";        // password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Test connection
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("SUCCESS: Connected to PIMS database!");
        } catch (SQLException e) {
            System.out.println("Connection failed:");
            e.printStackTrace();
        }
    }
}
