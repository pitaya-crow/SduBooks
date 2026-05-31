package org.example.sdubooks.model;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    private int code;

    @SerializedName("msg")
    private String msg;

    private UserData data;

    public static class UserData {
        public int personId;
        public String userName;
        public String token;
        public String nickname;
        public String role;

        public int getPersonId() { return personId; }
        public String getUserName() { return userName; }
        public String getToken() { return token; }
        public String getNickname() { return nickname; }
        public String getRole() { return role; }
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public UserData getData() {
        return data;
    }
}