package org.example.sdubooks.model;

public class UserStats {
    private String username;
    private String email;
    private String joinDate;
    private Integer totalBorrows;
    private Integer currentBorrows;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getJoinDate() { return joinDate; }
    public void setJoinDate(String joinDate) { this.joinDate = joinDate; }

    public Integer getTotalBorrows() { return totalBorrows; }
    public void setTotalBorrows(Integer totalBorrows) { this.totalBorrows = totalBorrows; }

    public Integer getCurrentBorrows() { return currentBorrows; }
    public void setCurrentBorrows(Integer currentBorrows) { this.currentBorrows = currentBorrows; }
}
