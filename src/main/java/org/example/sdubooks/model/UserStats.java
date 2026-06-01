package org.example.sdubooks.model;

public class UserStats {
    // 与后端 User 实体字段名一致
    private String userName;
    private String createTime;
    private Integer totalBorrows;
    private Integer currentBorrows;

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public Integer getTotalBorrows() { return totalBorrows; }
    public void setTotalBorrows(Integer totalBorrows) { this.totalBorrows = totalBorrows; }

    public Integer getCurrentBorrows() { return currentBorrows; }
    public void setCurrentBorrows(Integer currentBorrows) { this.currentBorrows = currentBorrows; }
}
