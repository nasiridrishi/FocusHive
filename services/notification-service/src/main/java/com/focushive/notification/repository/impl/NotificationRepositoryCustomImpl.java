package com.focushive.notification.repository.impl;

import com.focushive.notification.entity.Notification;
import com.focushive.notification.repository.NotificationRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.jpa.QueryHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of custom repository with optimized queries.
 * Uses JPA query hints and batch fetching to prevent N+1 problems.
 */
@Repository
public class NotificationRepositoryCustomImpl implements NotificationRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Notification> findNotificationsWithOptimizedFetch(String userId, Pageable pageable) {
        // Count query
        TypedQuery<Long> countQuery = entityManager.createQuery(
                "SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.deletedAt IS NULL",
                Long.class
        );
        countQuery.setParameter("userId", userId);
        Long totalElements = countQuery.getSingleResult();

        // Data query with optimization hints
        TypedQuery<Notification> query = entityManager.createQuery(
                "SELECT n FROM Notification n WHERE n.userId = :userId AND n.deletedAt IS NULL " +
                "ORDER BY n.createdAt DESC",
                Notification.class
        );
        query.setParameter("userId", userId);

        // Add query hints for optimization
        query.setHint(HibernateHints.HINT_FETCH_SIZE, 50);
        query.setHint(QueryHints.HINT_READONLY, false);
        query.setHint(HibernateHints.HINT_CACHEABLE, true);
        query.setHint("jakarta.persistence.query.timeout", 5000);

        // Apply pagination
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Notification> notifications = query.getResultList();

        return new PageImpl<>(notifications, pageable, totalElements);
    }

    @Override
    public List<Notification> findRecentNotificationsOptimized(String userId, LocalDateTime since) {
        TypedQuery<Notification> query = entityManager.createQuery(
                "SELECT n FROM Notification n " +
                "WHERE n.userId = :userId AND n.createdAt > :since AND n.deletedAt IS NULL " +
                "ORDER BY n.createdAt DESC",
                Notification.class
        );
        query.setParameter("userId", userId);
        query.setParameter("since", since);

        // Optimization hints
        query.setHint(HibernateHints.HINT_FETCH_SIZE, 100);
        query.setHint(QueryHints.HINT_READONLY, true);
        query.setHint("jakarta.persistence.query.timeout", 3000);

        return query.getResultList();
    }

    @Override
    public List<Notification> findByIdsOptimized(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        TypedQuery<Notification> query = entityManager.createQuery(
                "SELECT n FROM Notification n WHERE n.id IN :ids",
                Notification.class
        );
        query.setParameter("ids", ids);

        // Batch fetching hint
        query.setHint(HibernateHints.HINT_FETCH_SIZE, Math.min(ids.size(), 100));
        query.setHint("jakarta.persistence.query.timeout", 5000);

        return query.getResultList();
    }
}