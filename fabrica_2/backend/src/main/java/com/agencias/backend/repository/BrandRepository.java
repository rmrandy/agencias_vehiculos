package com.agencias.backend.repository;

import com.agencias.backend.model.Brand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

public class BrandRepository {
    private final EntityManagerFactory emf;

    public BrandRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public Brand save(Brand brand) {
        EntityManager em = emf.createEntityManager();
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (brand.getBrandId() == null) {
                em.persist(brand);
            } else {
                brand = em.merge(brand);
            }
            tx.commit();
            return brand;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Optional<Brand> findById(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            Brand b = em.find(Brand.class, id);
            return Optional.ofNullable(b);
        } finally {
            em.close();
        }
    }

    public List<Brand> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Brand> q = em.createQuery("SELECT b FROM Brand b ORDER BY b.name", Brand.class);
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
            Brand b = em.find(Brand.class, id);
            if (b != null) {
                em.remove(b);
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
