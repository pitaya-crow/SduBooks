package org.example.sdubooks.model;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    private int code;

    // 使用 SerializedName 确保 Gson 能正确将后端的 "msg" 映射到此字段
    @SerializedName("msg")
    private String msg;

    private UserData data;

    // 内部类用于接收 data 字段
    public static class UserData {
        public String token;
        public String nickname;

        // 【新增】角色字段，用于 Controller 判断跳转目标页面
        public String role;

        // 建议为内部类也提供 Getter，保持封装性
        public String getToken() { return token; }
        public String getNickname() { return nickname; }
        public String getRole() { return role; }
    }

    // Getters
    public int getCode() {
        return code;
    }

    // 【修改】与 Controller 中的 result.getMsg() 保持一致
    public String getMsg() {
        return msg;
    }

    public UserData getData() {
        return data;
    }
}