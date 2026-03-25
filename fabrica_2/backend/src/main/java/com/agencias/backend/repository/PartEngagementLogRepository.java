package com.agencias.backend.repository;

import com.agencias.backend.model.PartEngagementLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Date;
import java.util.List;

public class PartEngagementLogRepository {
    private final EntityManagerFactory emf;

    public PartEngagementLogRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public PartEngagementLog save(PartEngagementLog log) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            if (log.getLogId() == null) {
                em.persist(log);
            } else {
                log = em.merge(log);
            }
            em.getTransaction().commit();
            return log;
        } finally {
            em.close();
        }
    }

    public List<PartEngagementLog> findByEventTypeAndDateRange(String eventType, Date from, Date to, Integer limit) {
        EntityManager em = emf.createEntityManager();
        try {
            String jpql = "SELECT e FROM PartEngagementLog e WHERE e.eventType = :eventType AND e.createdAt >= :from AND e.createdAt <= :to ORDER BY e.createdAt DESC";
            var q = em.createQuery(jpql, PartEngagementLog.class)
                .setParameter("eventType", eventType)
                .setParameter("from", from)
                .setParameter("to", to);
            if (limit != null && limit > 0) q.setMaxResults(limit);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /** Cuenta eventos por partId y tipo en un rango de fechas. Útil para reportes agregados. */
    public List<Object[]> countByPartIdAndEventType(Date from, Date to, String eventType) {
        EntityManager em = emf.createEntityManager();
        try {
            String jpql = "SELECT e.partId, COUNT(e) FROM PartEngagementLog e WHERE e.eventType = :eventType AND e.createdAt >= :from AND e.createdAt <= :to GROUP BY e.partId ORDER BY COUNT(e) DESC";
            @SuppressWarnings("unchecked")
            List<Object[]> list = em.createQuery(jpql)
                .setParameter("eventType", eventType)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
            return list;
        } finally {
            em.close();
        }
    }
}
