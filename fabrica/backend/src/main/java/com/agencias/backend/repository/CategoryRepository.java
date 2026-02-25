package com.agencias.backend.repository;

import com.agencias.backend.model.Category;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

public class CategoryRepository {
    private final EntityManagerFactory emf;

    public CategoryRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public Category save(Category category) {
        EntityManager em = emf.createEntityManager();
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (category.getCategoryId() == null) {
                em.persist(category);
            } else {
                category = em.merge(category);
            }
            tx.commit();
            return category;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Optional<Category> findById(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            Category c = em.find(Category.class, id);
            return Optional.ofNullable(c);
        } finally {
            em.close();
        }
    }

    public List<Category> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Category> q = em.createQuery("SELECT c FROM Category c ORDER BY c.name", Category.class);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public void delete(Long id) {
        EntityManager em = emf.createEntityManager();
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Category c = em.find(Category.class, id);
            if (c != null) {
                em.remove(c);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
