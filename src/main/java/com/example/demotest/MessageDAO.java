package com.example.demotest;

import java.sql.*;
import java.util.logging.Logger;

public class MessageDAO {

    private static Logger logger = Logger.getLogger(MessageDAO.class.getName());
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

    // Helper method to keep track of MCStats metrics
    public void recordStat(String metricType) {
        String sql = "INSERT INTO MCStats (metric_name, count_value) VALUES (?, 1) " +
                "ON DUPLICATE KEY UPDATE count_value = count_value + 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, metricType);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to log MCStats: " + e.getMessage());
        }
    }

    public void storeUserMsg(String tableName, int userId, String msg, String type, String priority) {
//         Create table INFO1(id INT UNIQUE KEY AUTO_INCREMENT,user_id INT, Content TEXT , Type varchar(20));
        // Fixed space after INSERT INTO
        String sql = "INSERT INTO " + tableName + " (user_id, Content, Type, priority) values(?,?,?,?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);) {

            stmt.setInt(1, userId);
            stmt.setString(2, msg);
            stmt.setString(3, type);
            stmt.setBoolean(4, "true".equalsIgnoreCase(priority) || "yes".equalsIgnoreCase(priority));

            // Fixed executeUpdate for INSERT statement
            stmt.executeUpdate();
            recordStat("PRODUCED_MESSAGES");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public MessageModel getTableFirstRow(String tableName) {
        // Fixed LIMIT syntax (removed '=' sign) and implemented priority sequence ordering
        String sql = "SELECT * FROM " + tableName + " ORDER BY " +
                "  user_id IN (" +
                "    SELECT user_id FROM (" +
                "      SELECT DISTINCT user_id FROM " + tableName + " WHERE priority = TRUE" +
                "    ) AS pending_priority" +
                "  ) DESC, " +
                "  post_time ASC, " +
                "  id ASC LIMIT 1";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);) {

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int msg_id = rs.getInt(1);
                int user_id = rs.getInt(2);
                String Content = rs.getString(3);
                String Type = rs.getString(4);
                Boolean priority = rs.getBoolean(6);
                MessageModel messageModel = new MessageModel(msg_id, user_id, Content, Type, priority, null, null, tableName);
                return messageModel;
            }
            return null;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean insertMessage(MessageModel msgModel) {
        if (msgModel == null) return false;

        int msg_id = msgModel.getMsg_id();
        int user_id = msgModel.getUser_id();
        String Content = msgModel.getContent();
        String Type = msgModel.getType();
        boolean priority = msgModel.isPriority();
        String tableName = msgModel.getTableName();

        String sql = "INSERT INTO messageProcess(msg_id, user_id, Content, Type, priority) values(?,?,?,?,?)";

        // as of now assume priority is false

        //get the connection
        try (Connection connection = getConnection(); PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, msg_id);
            pstmt.setInt(2, user_id);
            pstmt.setString(3, Content);
            pstmt.setString(4, Type);
            pstmt.setBoolean(5, priority);

            int rs = pstmt.executeUpdate();
            if (rs != 0) {
                recordStat("PROCESSED_MESSAGES");
                return deleteRowFromTable(msg_id, tableName);
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // Atomic transaction method for fetch, insert, and delete safely
    public boolean processNextSubtableMessage(String tableName) {
        String selectSql = "SELECT id, user_id, Content, Type, priority FROM " + tableName + " " +
                "ORDER BY " +
                "  user_id IN (" +
                "    SELECT user_id FROM (" +
                "      SELECT DISTINCT user_id FROM " + tableName + " WHERE priority = TRUE" +
                "    ) AS pending_priority" +
                "  ) DESC, " +
                "  post_time ASC, id ASC " +
                "LIMIT 1 FOR UPDATE";

        String insertSql = "INSERT INTO messageProcess (msg_id, user_id, Content, Type, priority) VALUES (?, ?, ?, ?, ?)";
        String deleteSql = "DELETE FROM " + tableName + " WHERE id = ?";

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            int msgId = -1;
            int userId = -1;
            String content = null;
            String type = null;
            boolean priority = false;
            boolean found = false;

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql);
                 ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    msgId = rs.getInt("id");
                    userId = rs.getInt("user_id");
                    content = rs.getString("Content");
                    type = rs.getString("Type");
                    priority = rs.getBoolean("priority");
                    found = true;
                }
            }

            if (!found) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setInt(1, msgId);
                insertStmt.setInt(2, userId);
                insertStmt.setString(3, content);
                insertStmt.setString(4, type);
                insertStmt.setBoolean(5, priority);
                insertStmt.executeUpdate();
            }

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, msgId);
                deleteStmt.executeUpdate();
            }

            conn.commit();
            recordStat("PROCESSED_MESSAGES");
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
            }
            logger.severe("Transaction failure for table " + tableName + ": " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { /* ignore */ }
            }
        }
    }

    public boolean deleteRowFromTable(int msg_id, String tableName) {


        String sql = "DELETE FROM " + tableName + " where id = ?";

        //get the connection
        try (Connection connection = getConnection(); PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, msg_id);

            int rs = pstmt.executeUpdate();
            return rs != 0;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}