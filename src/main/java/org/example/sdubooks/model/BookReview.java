package org.example.sdubooks.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookReview {
    private Integer reviewId;
    private Integer bookId;
    private Integer userId;
    private String content;
    private BigDecimal rating;
    private LocalDateTime createTime;
    private Integer likeCount;
    private String username;

    public Integer getReviewId() { return reviewId; }
    public void setReviewId(Integer reviewId) { this.reviewId = reviewId; }

    public Integer getBookId() { return bookId; }
    public void setBookId(Integer bookId) { this.bookId = bookId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    public String getUsername() { return username != null ? username : "匿名用户"; }
    public void setUsername(String username) { this.username = username; }
}
