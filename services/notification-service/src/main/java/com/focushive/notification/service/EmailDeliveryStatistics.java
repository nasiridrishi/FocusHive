package com.focushive.notification.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Email delivery statistics and metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDeliveryStatistics {
    
    /**
     * Total number of emails sent
     */
    private long totalSent;
    
    /**
     * Number of emails delivered
     */
    private long deliveredCount;
    
    /**
     * Number of emails that bounced
     */
    private long bounceCount;
    
    /**
     * Number of complaints received
     */
    private long complaintCount;
    
    /**
     * Number of failed emails
     */
    private long failedCount;
    
    /**
     * Delivery rate (delivered/total sent)
     */
    private double deliveryRate;
    
    /**
     * Bounce rate (bounced/total sent)
     */
    private double bounceRate;
    
    /**
     * Complaint rate (complaints/total sent)
     */
    private double complaintRate;
    
    /**
     * Statistics breakdown by email type/category
     */
    private Map<String, TypeStatistics> statisticsByType;
    
    /**
     * When these statistics were calculated
     */
    private LocalDateTime calculatedAt;
    
    /**
     * Time period these statistics cover (start)
     */
    private LocalDateTime periodStart;
    
    /**
     * Time period these statistics cover (end)
     */
    private LocalDateTime periodEnd;
    
    /**
     * Statistics for a specific email type or category.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeStatistics {
        private long sent;
        private long delivered;
        private long bounced;
        private long complained;
        private double deliveryRate;
        private double bounceRate;
        private double complaintRate;
    }
}