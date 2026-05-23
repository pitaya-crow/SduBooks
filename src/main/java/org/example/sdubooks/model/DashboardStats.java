package org.example.sdubooks.model;

public class DashboardStats {
    private int totalBooks;
    private int borrowedBooks;
    private int registeredUsers;
    private int overdueBooks;

    // Getters (必须添加)
    public int getTotalBooks() { return totalBooks; }
    public int getBorrowedBooks() { return borrowedBooks; }
    public int getRegisteredUsers() { return registeredUsers; }
    public int getOverdueBooks() { return overdueBooks; }
}
