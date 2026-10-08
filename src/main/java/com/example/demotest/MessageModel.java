package com.example.demotest;

public class MessageModel {

    private int msg_id;
    private int user_id;
    private String Content;
    private String Type;
    private boolean priority;

    //when it len is <20kb = 1024 * 20
    private String payload;
    //when content lenght getBytes() use it and
    // <100kb = 100 * 1024
    private String path;
    //>100kb then "Size limit reached"
    private String status;

    private String tableName;

    MessageModel(int msg_id, int user_id, String Content, String Type, boolean priority, String payload, String path, String tableName) {
        this.msg_id = msg_id;
        this.user_id = user_id;
        this.Content = Content;
        this.Type = Type;
        this.priority = priority;
        this.payload = payload;
        this.path = path;
        this.status = null;
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public int getMsg_id() {
        return msg_id;
    }

    public String getStatus() {
        return status;
    }

    public String getPath() {
        return path;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isPriority() {
        return priority;
    }

    public String getType() {
        return Type;
    }

    public String getContent() {
        return Content;
    }

    public int getUser_id() {
        return user_id;
    }
}