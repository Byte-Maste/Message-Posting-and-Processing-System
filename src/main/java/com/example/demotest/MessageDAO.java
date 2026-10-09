package com.example.demotest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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


    public void recordStat(int msg_id, String sub_table, int user_id , String action_type, long wait_time_ms , long processing_time) {
//        Create table MCStats( id INT PRIMARY KEY AUTO_INCREMENT , msg_id INT NOT NULL , sub_table varchar(20) NOT NULL ,
//    -> user_id INT NOT NULL , action_type ENUM('PRODUCED','CONSUMED') NOT NULL , wait_time_ms BIGINT DEFAULT 0,
//    -> processing_time_ms BIGINT DEFAULT 0 , created_at DATETIME DEFAULT CURRENT_TIMESTAMP);
//        Query OK, 0 rows affected (0.165 sec);

        String sql = "INSERT INTO MCStats(msg_id, sub_table , user_id ,action_type, wait_time_ms , processing_time_ms ) VALUES (?,?,?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, msg_id);
            stmt.setString(2,sub_table);
            stmt.setInt(3,user_id);
            stmt.setString(4,action_type);
            stmt.setLong(5, wait_time_ms);
            stmt.setLong(6,processing_time);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to log MCStats: " + e.getMessage());
        }
    }

    public void storeUserMsg(String tableName, int userId, String msg, String type, String priority, boolean ispayload , String filePath) {
           long startTime = System.currentTimeMillis();
//         Create table INFO1(id INT UNIQUE KEY AUTO_INCREMENT,user_id INT, Content TEXT , Type varchar(20));
        // Fixed space after INSERT INTO
        String sql = "INSERT INTO " + tableName + "(user_id, Content, Type, priority , isPayload , path) values(?,?,?,?,?,?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql , Statement.RETURN_GENERATED_KEYS);) {

            stmt.setInt(1, userId);
            stmt.setString(2, msg);
            stmt.setString(3, type);
            stmt.setBoolean(4, "true".equalsIgnoreCase(priority) || "yes".equalsIgnoreCase(priority));
            stmt.setBoolean(5,ispayload);
            stmt.setString(6,filePath);

            // Fixed executeUpdate for INSERT statement
            if(stmt.executeUpdate() != 0)
            {
                ResultSet rs = stmt.getGeneratedKeys();
                if(rs.next())
                {
                    int msg_id = rs.getInt(1);
                    long diff = System.currentTimeMillis() - startTime;
                    recordStat(msg_id, tableName,userId , "PRODUCED",0L, diff);
                }
            }



        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }




    public boolean processSubtableMessage(String tableName) {

        long startTime = System.currentTimeMillis();
        String selectSql = "SELECT id, user_id, Content, Type, priority, isPayload , path , post_time FROM " + tableName + " WHERE priority = true ORDER BY post_time ASC, id ASC LIMIT 1 FOR UPDATE";

        String insertSql = "INSERT INTO messageprocess(msg_id, user_id, Content, Type, priority , isPayload , path) VALUES (?, ?, ?, ?, ?, ? ,?)";
        String deleteSql = "DELETE FROM " + tableName + " WHERE id = ?";

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);


            // these variable store the data of priority message when selectSql return any message that has priority true or yes
            Integer priorityMsgId = null;
            Integer priorityUserId = null;
            String prioritycontent = null;
            String priorittype = null;
            Timestamp prioritypost_time = null;
            boolean priority = false;
            boolean isPayload = false;
            String path = null;


            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql);
                 ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    priorityMsgId = rs.getInt(1);
                    priorityUserId = rs.getInt(2);
                    prioritycontent = rs.getString(3);
                    priorittype = rs.getString(4);
                    priority = rs.getBoolean(5);
                    isPayload = rs.getBoolean(6);
                    path = rs.getString(7);
                    prioritypost_time = rs.getTimestamp(8);

                }

                if(priorityUserId != null)
                {
                    logger.info("Priority Message Detected Id" + priorityMsgId + "for user "+ priorityUserId);

                    String fetchUserOldProcessSql =  "SELECT id, user_id, Content, Type, priority, isPayload , path ,post_time FROM " + tableName + " WHERE user_id = ? AND id < ? " +
                            "ORDER BY post_time ASC, id ASC ";

                    try (PreparedStatement pstmt = conn.prepareStatement(fetchUserOldProcessSql)) {
                        pstmt.setInt(1, priorityUserId);
                        pstmt.setInt(2, priorityMsgId);

                        List<MessageModel> listofbacklog = new ArrayList<>();

                        // list of oldprocess process -> insert into messagequeue and delete from subtable
                        ResultSet result = pstmt.executeQuery();
                        while(result.next())
                        {
                            listofbacklog.add(new MessageModel(
                                    result.getInt(1),
                                    result.getInt(2),
                                    result.getString(3),
                                    result.getString(4),
                                    result.getBoolean(5),
                                    result.getBoolean(6),
                                    result.getString(7),
                                    result.getTimestamp(8)
                            ));
                        }

                        for(MessageModel backlog : listofbacklog)
                        {
                            //insert every old process in messageprocess in fifo order
                            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                                insertStmt.setInt(1, backlog.getMsg_id());
                                insertStmt.setInt(2, backlog.getUser_id());
                                insertStmt.setString(3, backlog.getContent());
                                insertStmt.setString(4, backlog.getType());
                                insertStmt.setBoolean(5, backlog.isPriority());
                                insertStmt.setBoolean(6, backlog.getPayload());
                                insertStmt.setString(7, backlog.getPath());

                                if(insertStmt.executeUpdate()!= 0) {

                                    long diff = System.currentTimeMillis() - startTime;
                                    long wait_time_ms = System.currentTimeMillis() - backlog.getPostTime().getTime();
                                    recordStat(backlog.getMsg_id(), tableName,backlog.getUser_id() , "CONSUMED",wait_time_ms, diff);

                                    logger.info("inserting the Old Message of user id" + backlog.getUser_id() + "message id " + backlog.getMsg_id() + " inside the messageprocess in fifo order before the inserting priority messageId" + priorityMsgId + "successfully");
                                }else {
                                    logger.info(" failed while inserting the Old Message of user id" + backlog.getUser_id() + "message id " + backlog.getMsg_id() + " inside the messageprocess in fifo order before the inserting priority messageId " + priorityMsgId);

                                }

                            }

                            // delete the old process from the subtable in fifo
                            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                                deleteStmt.setInt(1, backlog.getMsg_id());

                                if(deleteStmt.executeUpdate() != 0) {

                                    logger.info("Deleting the Old Message of user id " + backlog.getUser_id() + " message id which is deleted" +backlog.getMsg_id() + "before deleting the priority messageId "+ priorityMsgId+" successfully");
                                }
                                else {
                                    logger.info("failed Deleting the Old Message of user id " + backlog.getUser_id() + " message id which is deleted" +backlog.getMsg_id() + " failed before deleting the priority messageId "+ priorityMsgId);

                                }

                            }
                        }


                        // also insert the priority info message into message
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setInt(1,priorityMsgId);
                            insertStmt.setInt(2, priorityUserId);
                            insertStmt.setString(3, prioritycontent);
                            insertStmt.setString(4, priorittype);
                            insertStmt.setBoolean(5, priority);
                            insertStmt.setBoolean(6, isPayload);
                            insertStmt.setString(7, path);

                            if(insertStmt.executeUpdate()!= 0) {

                                long diff = System.currentTimeMillis() - startTime;
                                long wait_time_ms = System.currentTimeMillis() - prioritypost_time.getTime();
                                recordStat(priorityMsgId, tableName,priorityUserId , "CONSUMED",wait_time_ms, diff);
                                logger.info("inserting the Old Message of user id" + priorityUserId  + "message id " + priorityMsgId + " inside the messageprocess in fifo order before the inserting priority message successfully");
                            }else {
                                logger.info(" failed while inserting the Old Message of user id" + priorityUserId + "message id " + priorityMsgId + " inside the messageprocess in fifo order before the inserting priority message successfully");

                            }

                        }

                        //deleting the priority info message from subtable
                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                            deleteStmt.setInt(1, priorityMsgId);

                            if(deleteStmt.executeUpdate() != 0) {

                                logger.info("Deleting the Old Message of user id " + priorityUserId + "message id which is deleted " +priorityMsgId + " before deleting the priority message successfully");
                                conn.commit();
                                return true;
                            }
                            else {
                                logger.info("failed Deleting the Old Message of user id " + priorityUserId + "message id which is deleted " + priorityUserId + " failed before deleting the priority message");
                            }

                        }

                    }


                }
                else {
                    // process the individual fifo message process since no priority true in this table exists as of now
                    String selectNoPrioritySql = "SELECT id, user_id, Content, Type, priority, isPayload , path , post_time FROM " + tableName  +
                            " ORDER BY post_time ASC, id ASC " +
                            "LIMIT 1 FOR UPDATE";
                    //store the data of nonpriority message
                    Integer MsgId = null;
                    Integer UserId = null;
                    String content = null;
                    String type = null;
//            post_time
                    boolean Nopriority = false;
                    boolean isPayloadNonPriority = false;
                    String pathNonPriority = null;
                    Timestamp postTime ;



                    try (PreparedStatement selectStmtNonPriority = conn.prepareStatement(selectNoPrioritySql);
                         ) {
                        ResultSet res = selectStmtNonPriority.executeQuery();

                                while(res.next()) {
                                    MsgId = res.getInt(1);
                                    UserId = res.getInt(2);
                                    content = res.getString(3);
                                    type = res.getString(4);
                                    Nopriority = res.getBoolean(5);
                                    isPayloadNonPriority = res.getBoolean(6);
                                    pathNonPriority = res.getString(7);
                                    postTime = res.getTimestamp(8);


                                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                                        insertStmt.setInt(1, MsgId);
                                        insertStmt.setInt(2, UserId);
                                        insertStmt.setString(3, content);
                                        insertStmt.setString(4, type);
                                        insertStmt.setBoolean(5, Nopriority);
                                        insertStmt.setBoolean(6,isPayloadNonPriority);
                                        insertStmt.setString(7, pathNonPriority);

                                        if(insertStmt.executeUpdate()!= 0)
                                        {
                                            long diff = System.currentTimeMillis() - startTime;
                                            long wait_time_ms = System.currentTimeMillis() - postTime.getTime();
                                            recordStat(MsgId, tableName,UserId , "CONSUMED",wait_time_ms, diff);
                                            logger.info("inserting the  Message of user id" + UserId  + "message id " + MsgId + " inside the messageprocess in fifo order in nonpriority message successfully");
                                        }else {
                                            logger.info(" failed while inserting the  Message of user id" + UserId + "message id " + MsgId + " inside the messageprocess in fifo order nonpriority message successfully");

                                        }
                                    }


                                    try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                                        deleteStmt.setInt(1, MsgId);
                                        if(deleteStmt.executeUpdate()!= 0){
                                            logger.info("Deleting the  Message of user id " + UserId + " message id which is deleted " +MsgId + " is the nonpriority message successfully");
                                            conn.commit();
                                            return true;
                                        }
                                        else {
                                            logger.info("failed Deleting the  Message of user id " + UserId + " message id which is deleted " +MsgId + " failed deleting the nonpriority message");

                                        }
                                    }
                                }

                        }



                }
            }
            conn.rollback();
            return false;
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

    public  String getOrAssingTable(int user_id, String type)
     {
         type = type.toLowerCase();
         String sql = "SELECT sub_table_name FROM user_routes WHERE user_id=? AND  msg_type = ?";

         try (Connection connection = getConnection(); PreparedStatement pstmt = connection.prepareStatement(sql) )
         {
             pstmt.setInt(1,user_id);
             pstmt.setString(2,type);
             ResultSet resultSet = pstmt.executeQuery();
             while(resultSet.next())
             {
                 return resultSet.getString("sub_table_name");
             }
         } catch (SQLException e) {
             throw new RuntimeException(e);
         }

         //from maintable
         int totalSubTables = getSubTableCount(type);
         if(totalSubTables == -1)
         {
             logger.info("Create the subtables then try to send the data");
         }
         //1 based index
         int tableIndex = (Math.abs(Integer.hashCode(user_id)) % totalSubTables) + 1;
         String assingedTable = type + tableIndex;
         String insertsql = "INSERT INTO user_routes(user_id , msg_type ,  sub_table_name) values(?,?,?)";
         try(Connection connection = getConnection(); PreparedStatement pstmt = connection.prepareStatement(insertsql))
         {
             pstmt.setInt(1,user_id);
             pstmt.setString(2,type);
             pstmt.setString(3,assingedTable);

             pstmt.executeUpdate();
         } catch (SQLException e) {
             throw new RuntimeException(e);
         }

         return assingedTable;
     }

     public int getSubTableCount(String type)
     {
         String insertsql = "SELECT COUNT(*) FROM maintable  where type = ?";
         try(Connection connection = getConnection(); PreparedStatement pstmt = connection.prepareStatement(insertsql))
         {
             pstmt.setString(1,type);
             ResultSet rs = pstmt.executeQuery();

             if(rs.next())
             {
                 int count = rs.getInt(1);
                 return count;
             }

         } catch (SQLException e) {
             throw new RuntimeException(e);
         }
         return -1;
     }
}

