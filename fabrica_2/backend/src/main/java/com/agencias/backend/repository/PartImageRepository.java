package com.agencias.backend.repository;

import com.agencias.backend.model.PartImage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class PartImageRepository {
    private final EntityManagerFactory emf;

    public PartImageRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public PartImage save(PartImage row) {
        EntityManager em = emf.createEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();
            if (row.getImageId() == null) {
                em.persist(row);
            } else {
                row = em.merge(row);
            }
            tx.commit();
            return row;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public void deleteByPartId(Long partId) {
        EntityManager em = emf.createEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("DELETE FROM PartImage pi WHERE pi.partId = :pid")
                    .setParameter("pid", partId)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public List<PartImage> findByPartIdOrderBySort(Long partId) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<PartImage> q = em.createQuery(
                    "SELECT pi FROM PartImage pi WHERE pi.partId = :pid ORDER BY pi.sortOrder ASC", PartImage.class);
            q.setParameter("pid", partId);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public long countByPartId(Long partId) {
        EntityManager em = emf.createEntityManager();
        try {
            Long n = em.createQuery("SELECT COUNT(pi) FROM PartImage pi WHERE pi.partId = :pid", Long.class)
                    .setParameter("pid", partId)
                    .getSingleResult();
            return n != null ? n : 0L;
        } finally {
            em.close();
        }
    }

    public PartImage findByPartIdAndSort(Long partId, int sortOrder) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<PartImage> q = em.createQuery(
                    "SELECT pi FROM PartImage pi WHERE pi.partId = :pid AND pi.sortOrder = :so", PartImage.class);
            q.setParameter("pid", partId);
            q.setParameter("so", sortOrder);
            List<PartImage> list = q.getResultList();
            return list.isEmpty() ? null : list.get(0);
        } finally {
            em.close();
        }
    }
}
