package com.agencias.backend.repository;

import com.agencias.backend.model.EnterpriseProfile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

public class EnterpriseProfileRepository {
    private final EntityManagerFactory emf;

    public EnterpriseProfileRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public Optional<EnterpriseProfile> findByUserId(Long userId) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<EnterpriseProfile> q = em.createQuery(
                "SELECT e FROM EnterpriseProfile e WHERE e.userId = :uid", EnterpriseProfile.class);
            q.setParameter("uid", userId);
            return q.getResultList().stream().findFirst();
        } finally {
            em.close();
        }
    }

    public List<EnterpriseProfile> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<EnterpriseProfile> q = em.createQuery(
                "SELECT e FROM EnterpriseProfile e ORDER BY e.userId", EnterpriseProfile.class);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public EnterpriseProfile save(EnterpriseProfile profile) {
        EntityManager em = emf.createEntityManager();
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (profile.getEnterpriseId() == null) {
                em.persist(profile);
            } else {
                profile = em.merge(profile);
            }
            tx.commit();
            return profile;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
