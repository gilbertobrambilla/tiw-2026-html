package it.polimi.tiw.utils;

import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static DatabaseConnection instance;

    private final String url;
    private final String user;
    private final String password;

    private DatabaseConnection(ServletContext context) throws UnavailableException {
        this.url = context.getInitParameter("dbUrl");
        this.user = context.getInitParameter("dbUser");
        this.password = context.getInitParameter("dbPassword");
        String driver = context.getInitParameter("dbDriver");

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new UnavailableException("Driver " + driver + " not found.");
        }
    }

    public static synchronized DatabaseConnection getInstance(ServletContext context) throws UnavailableException {
        if (instance == null) {
            instance = new DatabaseConnection(context);
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(this.url, this.user, this.password);
    }
}
