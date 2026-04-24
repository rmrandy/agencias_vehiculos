package com.agencias.backend.repository;

import com.agencias.backend.model.InventoryLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class InventoryLogRepository {
    private final EntityManagerFactory emf;

    public InventoryLogRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public List<InventoryLog> findRecent(Integer limit, Long partId, Long userId) {
        EntityManager em = emf.createEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT l FROM InventoryLog l WHERE 1=1");
            if (partId != null) {
                jpql.append(" AND l.partId = :partId");
            }
            if (userId != null) {
                jpql.append(" AND l.userId = :userId");
            }
            jpql.append(" ORDER BY l.createdAt DESC");
            TypedQuery<InventoryLog> q = em.createQuery(jpql.toString(), InventoryLog.class);
            if (partId != null) q.setParameter("partId", partId);
            if (userId != null) q.setParameter("userId", userId);
            if (limit != null && limit > 0) {
                q.setMaxResults(limit);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public InventoryLog save(InventoryLog log) {
        EntityManager em = emf.createEntityManager();
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (log.getLogId() == null) {
                em.persist(log);
            } else {
                log = em.merge(log);
            }
            tx.commit();
            return log;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
