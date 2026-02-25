package com.agencias.backend.repository;

import com.agencias.backend.model.Part;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PartRepository {
    private final EntityManagerFactory emf;

    public PartRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public Part save(Part part) {
        EntityManager em = emf.createEntityManager();
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (part.getPartId() == null) {
                em.persist(part);
            } else {
                part = em.merge(part);
            }
            tx.commit();
            return part;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Optional<Part> findById(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            Part p = em.find(Part.class, id);
            return Optional.ofNullable(p);
        } finally {
            em.close();
        }
    }

    public Optional<Part> findByPartNumber(String partNumber) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Part> q = em.createQuery("SELECT p FROM Part p WHERE p.partNumber = :pn", Part.class);
            q.setParameter("pn", partNumber);
            return q.getResultList().stream().findFirst();
        } finally {
            em.close();
        }
    }

    public List<Part> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Part> q = em.createQuery("SELECT p FROM Part p WHERE p.active = 1 ORDER BY p.createdAt DESC", Part.class);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public List<Part> findByCategory(Long categoryId) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Part> q = em.createQuery("SELECT p FROM Part p WHERE p.categoryId = :cid AND p.active = 1 ORDER BY p.title", Part.class);
            q.setParameter("cid", categoryId);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public List<Part> findByBrand(Long brandId) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Part> q = em.createQuery("SELECT p FROM Part p WHERE p.brandId = :bid AND p.active = 1 ORDER BY p.title", Part.class);
            q.setParameter("bid", brandId);
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
            Part p = em.find(Part.class, id);
            if (p != null) {
                em.remove(p);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Búsqueda por nombre (título), descripción y especificaciones (descripción).
     * Cualquier término null se omite. LIKE %valor% en cada campo.
     */
    public List<Part> search(String nombre, String descripcion, String especificaciones) {
        EntityManager em = emf.createEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT p FROM Part p WHERE p.active = 1");
            List<String> conditions = new ArrayList<>();
            if (nombre != null && !nombre.isBlank()) {
                conditions.add("LOWER(p.title) LIKE :nombre");
            }
            if (descripcion != null && !descripcion.isBlank()) {
                conditions.add("LOWER(p.description) LIKE :descripcion");
            }
            if (especificaciones != null && !especificaciones.isBlank()) {
                conditions.add("LOWER(p.description) LIKE :espec");
            }
            if (!conditions.isEmpty()) {
                jpql.append(" AND ").append(String.join(" AND ", conditions));
            }
            jpql.append(" ORDER BY p.title");
            TypedQuery<Part> q = em.createQuery(jpql.toString(), Part.class);
            if (nombre != null && !nombre.isBlank()) {
                q.setParameter("nombre", "%" + nombre.toLowerCase().trim() + "%");
            }
            if (descripcion != null && !descripcion.isBlank()) {
                q.setParameter("descripcion", "%" + descripcion.toLowerCase().trim() + "%");
            }
            if (especificaciones != null && !especificaciones.isBlank()) {
                q.setParameter("espec", "%" + especificaciones.toLowerCase().trim() + "%");
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /** Lista todos los repuestos (incluye inactivos) para exportación. */
    public List<Part> findAllForExport() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Part> q = em.createQuery("SELECT p FROM Part p ORDER BY p.partId", Part.class);
            return q.getResultList();
        } finally {
            em.close();
        }
    }
}
