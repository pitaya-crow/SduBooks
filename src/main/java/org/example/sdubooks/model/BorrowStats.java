package org.example.sdubooks.model;

import java.util.List;

public class BorrowStats {
    private Long totalBorrowCount;
    private Long totalReturnCount;
    private Double monthlyGrowthRate;

    public Long getTotalBorrowCount() {
        return totalBorrowCount;
    }

    public void setTotalBorrowCount(Long totalBorrowCount) {
        this.totalBorrowCount = totalBorrowCount;
    }

    public Long getTotalReturnCount() {
        return totalReturnCount;
    }

    public void setTotalReturnCount(Long totalReturnCount) {
        this.totalReturnCount = totalReturnCount;
    }

    public Double getMonthlyGrowthRate() {
        return monthlyGrowthRate;
    }

    public void setMonthlyGrowthRate(Double monthlyGrowthRate) {
        this.monthlyGrowthRate = monthlyGrowthRate;
    }
}
