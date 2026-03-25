package com.agencias.backend.repository;

import com.agencias.backend.model.AppUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

public class AppUserRepository {
    private final EntityManagerFactory emf;

    public AppUserRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public AppUser save(AppUser user) {
        EntityManager em = emf.createEntityManager();
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (user.getUserId() == null) {
                em.persist(user);
            } else {
                user = em.merge(user);
            }
            tx.commit();
            return user;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Optional<AppUser> findById(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            AppUser u = em.find(AppUser.class, id);
            return Optional.ofNullable(u);
        } finally {
            em.close();
        }
    }

    public Optional<AppUser> findByEmail(String email) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<AppUser> q = em.createQuery("SELECT u FROM AppUser u WHERE u.email = :email", AppUser.class);
            q.setParameter("email", email);
            return q.getResultList().stream().findFirst();
        } finally {
            em.close();
        }
    }

    public long count() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Long> q = em.createQuery("SELECT COUNT(u) FROM AppUser u", Long.class);
            return q.getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<AppUser> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<AppUser> q = em.createQuery(
                "SELECT DISTINCT u FROM AppUser u LEFT JOIN FETCH u.roles ORDER BY u.createdAt DESC",
                AppUser.class
            );
            return q.getResultList();
        } finally {
            em.close();
        }
    }
}
