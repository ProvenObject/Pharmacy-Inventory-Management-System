===============================================
  HealthFirst Pharmacy Inventory Management System (PIMS)
  Programming 732 Assignment
===============================================

DEFAULT LOGIN CREDENTIALS
-------------------------
Admin:
  Username: admin
  Password: admin123

Cashier:
  Username: cashier
  Password: cash123


HOW TO RUN
----------
1. Make sure MySQL is running.
2. Run the database.sql script to create the database and sample data.
3. Open the project in IntelliJ (or run the executable).
4. Run the Main class.


FEATURES IMPLEMENTED
--------------------
- Secure Login with role-based access (Admin / Cashier)
- Admin Dashboard with tabs:
  • Medicines (Full CRUD)
  • Suppliers (Full CRUD)
  • Users (Create / Update / Delete)
  • Reports (Sales, Item-Wise, Low Stock, Expiry)
- Cashier Point of Sale:
  • Search medicines
  • Add to cart
  • Checkout (reduces stock automatically)
  • Generate Bill / Receipt
- Database: MySQL with proper relationships


TECHNOLOGIES
------------
- Java Swing (GUI)
- JDBC
- MySQL
