package com.agencias.backend.repository;

import com.agencias.backend.model.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

public class RoleRepository {
    private final EntityManagerFactory emf;

    public RoleRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public List<Role> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Role> q = em.createQuery("SELECT r FROM Role r ORDER BY r.name", Role.class);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public Optional<Role> findById(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            Role r = em.find(Role.class, id);
            return Optional.ofNullable(r);
        } finally {
            em.close();
        }
    }

    public Optional<Role> findByName(String name) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Role> q = em.createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class);
            q.setParameter("name", name);
            return q.getResultList().stream().findFirst();
        } finally {
            em.close();
        }
    }

    public Role save(Role role) {
        EntityManager em = emf.createEntityManager();
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (role.getRoleId() == null) {
                em.persist(role);
            } else {
                role = em.merge(role);
            }
            tx.commit();
            return role;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
