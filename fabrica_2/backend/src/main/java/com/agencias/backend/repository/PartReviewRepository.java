package com.agencias.backend.repository;

import com.agencias.backend.model.PartReview;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class PartReviewRepository {
    private final EntityManagerFactory emf;

    public PartReviewRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public PartReview save(PartReview review) {
        EntityManager em = emf.createEntityManager();
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (review.getReviewId() == null) {
                em.persist(review);
            } else {
                review = em.merge(review);
            }
            tx.commit();
            return review;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<PartReview> findByPartId(Long partId) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<PartReview> q = em.createQuery(
                "SELECT r FROM PartReview r WHERE r.partId = :partId ORDER BY r.createdAt ASC",
                PartReview.class
            );
            q.setParameter("partId", partId);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public List<PartReview> findRootsByPartId(Long partId) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<PartReview> q = em.createQuery(
                "SELECT r FROM PartReview r WHERE r.partId = :partId AND r.parentId IS NULL ORDER BY r.createdAt ASC",
                PartReview.class
            );
            q.setParameter("partId", partId);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /** Promedio de rating (1-5) de comentarios raíz del producto. Null si no hay valoraciones. */
    public Double averageRatingByPartId(Long partId) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Double> q = em.createQuery(
                "SELECT AVG(r.rating) FROM PartReview r WHERE r.partId = :partId AND r.parentId IS NULL AND r.rating IS NOT NULL",
                Double.class
            );
            q.setParameter("partId", partId);
            List<Double> list = q.getResultList();
            return (list.isEmpty() || list.get(0) == null) ? null : list.get(0);
        } finally {
            em.close();
        }
    }
}
