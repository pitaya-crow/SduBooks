package org.example.sdubooks.model;

public class HotBook {
    private Long id;
    private String title;
    private String author;
    private int borrowCount;

    // Getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getBorrowCount() { return borrowCount; }
}
