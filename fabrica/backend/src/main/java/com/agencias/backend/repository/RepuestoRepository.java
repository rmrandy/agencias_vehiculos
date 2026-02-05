package com.agencias.backend.repository;

import com.agencias.backend.model.Repuesto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

public class RepuestoRepository {
    private final EntityManagerFactory emf;

    public RepuestoRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public Repuesto save(Repuesto repuesto) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (repuesto.getId() == null) {
                em.persist(repuesto);
            } else {
                repuesto = em.merge(repuesto);
            }
            tx.commit();
            return repuesto;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public Optional<Repuesto> findById(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            Repuesto repuesto = em.find(Repuesto.class, id);
            return Optional.ofNullable(repuesto);
        } finally {
            em.close();
        }
    }

    public List<Repuesto> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Repuesto> query = em.createQuery("SELECT r FROM Repuesto r", Repuesto.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    public void delete(Long id) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Repuesto repuesto = em.find(Repuesto.class, id);
            if (repuesto != null) {
                em.remove(repuesto);
            }
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
}
