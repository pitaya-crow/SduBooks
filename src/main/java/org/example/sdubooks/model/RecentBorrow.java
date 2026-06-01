package org.example.sdubooks.model;

import java.time.LocalDateTime;

public class RecentBorrow {
    private Integer id;
    private String userName;
    private String bookTitle;
    private LocalDateTime borrowedAt;
    private LocalDateTime returnedAt;
    private Integer status;

    public Integer getId() { return id; }
    public String getUserName() { return userName; }
    public String getBookTitle() { return bookTitle; }
    public LocalDateTime getBorrowedAt() { return borrowedAt; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public Integer getStatus() { return status; }
}
