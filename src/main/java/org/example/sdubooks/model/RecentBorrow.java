package org.example.sdubooks.model;
import java.time.LocalDateTime;
public class RecentBorrow {
    private String bookName;
    private String userName;
    private LocalDateTime timestamp;

    // Getters
    public String getBookName() { return bookName; }
    public String getUserName() { return userName; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
