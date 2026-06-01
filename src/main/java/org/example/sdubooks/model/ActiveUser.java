package org.example.sdubooks.model;

public class ActiveUser {
    // 与后端 User 实体字段名一致
    private Integer personId;
    private String userName;
    private Integer userTypeId;
    private String createTime;

    public Integer getPersonId() { return personId; }
    public String getUserName() { return userName; }
    public Integer getUserTypeId() { return userTypeId; }
    public String getCreateTime() { return createTime; }

    // 兼容前端显示
    public String getName() { return userName != null ? userName : "用户"; }
    public String getEmail() { return ""; }
    public int getBorrowCount() { return 0; }
}
