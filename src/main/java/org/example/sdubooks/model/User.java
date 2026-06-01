package org.example.sdubooks.model;

import java.time.LocalDateTime;

public class User {
    // 与后端 User 实体字段名一致
    private Integer personId;
    private Integer userTypeId;
    private String userName;
    private String password;
    private String createTime;
    private Integer creatorId;
    private LocalDateTime lastLoginTime;
    private Integer loginCount;
    private Integer status;

    public Integer getPersonId() { return personId; }
    public void setPersonId(Integer personId) { this.personId = personId; }

    public Integer getUserTypeId() { return userTypeId; }
    public void setUserTypeId(Integer userTypeId) { this.userTypeId = userTypeId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public Integer getCreatorId() { return creatorId; }
    public void setCreatorId(Integer creatorId) { this.creatorId = creatorId; }

    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }

    public Integer getLoginCount() { return loginCount; }
    public void setLoginCount(Integer loginCount) { this.loginCount = loginCount; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    // 兼容前端表格显示
    public String getUsername() { return userName; }
    public String getRole() { return userTypeId != null && userTypeId == 2 ? "ADMIN" : "USER"; }
    public Boolean getEnabled() { return status != null && status == 1; }
}
