package com.agencias.backend.repository;

import com.agencias.backend.model.Vehicle;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

public class VehicleRepository {
    private final EntityManagerFactory emf;

    public VehicleRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public Vehicle save(Vehicle vehicle) {
        EntityManager em = emf.createEntityManager();
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (vehicle.getVehicleId() == null) {
                em.persist(vehicle);
            } else {
                vehicle = em.merge(vehicle);
            }
            tx.commit();
            return vehicle;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Optional<Vehicle> findById(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            Vehicle v = em.find(Vehicle.class, id);
            return Optional.ofNullable(v);
        } finally {
            em.close();
        }
    }

    public List<Vehicle> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Vehicle> q = em.createQuery("SELECT v FROM Vehicle v ORDER BY v.make, v.line, v.yearNumber", Vehicle.class);
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
            Vehicle v = em.find(Vehicle.class, id);
            if (v != null) {
                em.remove(v);
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
