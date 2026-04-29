
package com.example.demo1;  // ← ajouter cette ligne
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class maintest {
    private static final long serialVersionUID = 1L;
    public static void main(String[] args) throws SQLException {
        Connection conn = DriverManager.getConnection(
                "jdbc:sqlserver://localhost:63135;databaseName=banque_db;encrypt=false",
                "sa",
                "1234"
        );
        System.out.println("Connexion OK !");
    }
}
