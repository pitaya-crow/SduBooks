package org.example.sdubooks.model;

public class LoginResponse {
    private int code;
    private String message;
    private UserData data;

    // 内部类用于接收 data 字段
    public static class UserData {
        public String token;
        public String nickname;

    }

    // Getters and Setters
    public int getCode() { return code; }
    public String getMessage() { return message; }
    public UserData getData() { return data; }
}