package org.example.sdubooks.model;

public class CategoryStats {
    // 后端 SQL: SELECT category, COUNT(*) AS count FROM book GROUP BY category
    private String category;
    private Number count; // 数据库可能返回 Long/Integer

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Number getCount() { return count; }
    public void setCount(Number count) { this.count = count; }

    // 兼容前端显示
    public String getCategoryName() { return category != null ? category : "未分类"; }
    public int getBookCount() { return count != null ? count.intValue() : 0; }
}
