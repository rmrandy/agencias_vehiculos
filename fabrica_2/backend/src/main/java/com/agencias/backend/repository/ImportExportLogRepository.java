package com.agencias.backend.repository;

import com.agencias.backend.model.ImportExportLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class ImportExportLogRepository {
    private final EntityManagerFactory emf;

    public ImportExportLogRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public List<ImportExportLog> findRecent(Integer limit, String operation) {
        EntityManager em = emf.createEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT l FROM ImportExportLog l WHERE 1=1");
            if (operation != null && !operation.isBlank()) {
                jpql.append(" AND l.operation = :op");
            }
            jpql.append(" ORDER BY l.createdAt DESC");
            TypedQuery<ImportExportLog> q = em.createQuery(jpql.toString(), ImportExportLog.class);
            if (operation != null && !operation.isBlank()) {
                q.setParameter("op", operation.trim());
            }
            if (limit != null && limit > 0) {
                q.setMaxResults(limit);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public ImportExportLog save(ImportExportLog log) {
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
