package org.example.sdubooks.model;

public class Book {
    private Long id;
    private String title;
    private String author;
    private Integer total;
    private Integer available;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
    public Integer getAvailable() { return available; }
    public void setAvailable(Integer available) { this.available = available; }
}