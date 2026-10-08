package com.example.demotest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/message_queue";
    private static final String username = "root";
    private static final String password = "root";

    public Connection getConnection() {
        Connection connection = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    //    Create table users(id INT PRIMARY KEY AUTO_INCREMENT,username varchar(20), password varchar(20));
    // store the new user in db
    public boolean userExist(String username) {
        //check existing user or not
        String sql = "SELECT * FROM users where username = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);) {
            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString(2) != null;
            } else {
                return false;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public boolean registerUser(String username, String password) {
        //check existing user or not
        String sql = "INSERT INTO users(username , password) values(?,?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);) {
            stmt.setString(1, username);
            stmt.setString(2, password);

            int rs = stmt.executeUpdate();
            return rs != 0;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // get the user details is valid or not during login
    public boolean validateUser(String username, String password) {
        //check existing user or not
        String sql = "SELECT * FROM users where username =? and password = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);) {
            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString(1) != null;
            } else {
                return false;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getUserID(String username) {
        String sql = "SELECT id FROM users where username =?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);) {
            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Fixed column index from 0 to 1 (JDBC uses 1-based indexing)
                return rs.getInt(1);
            } else {
                return -1;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Integer> getAllUserIds() {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT id FROM users";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}