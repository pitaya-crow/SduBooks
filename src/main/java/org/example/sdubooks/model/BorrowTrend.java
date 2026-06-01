package org.example.sdubooks.model;

public class BorrowTrend {
    // 后端返回 date 为 "YYYY-MM" 格式字符串
    private String date;
    private Integer borrowCount;
    private Integer returnCount;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Integer getBorrowCount() { return borrowCount; }
    public void setBorrowCount(Integer borrowCount) { this.borrowCount = borrowCount; }

    public Integer getReturnCount() { return returnCount; }
    public void setReturnCount(Integer returnCount) { this.returnCount = returnCount; }
}
