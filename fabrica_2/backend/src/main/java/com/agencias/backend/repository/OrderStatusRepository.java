package com.agencias.backend.repository;

import com.agencias.backend.model.OrderStatusHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;

public class OrderStatusRepository {
    private final EntityManagerFactory emf;

    public OrderStatusRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public OrderStatusHistory save(OrderStatusHistory status) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(status);
            em.flush();
            em.getTransaction().commit();
            return status;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al guardar el estado del pedido: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    public List<OrderStatusHistory> findByOrderId(Long orderId) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                "SELECT osh FROM OrderStatusHistory osh WHERE osh.orderId = :orderId ORDER BY osh.changedAt DESC", 
                OrderStatusHistory.class)
                .setParameter("orderId", orderId)
                .getResultList();
        } finally {
            em.close();
        }
    }

    public OrderStatusHistory findLatestByOrderId(Long orderId) {
        EntityManager em = emf.createEntityManager();
        try {
            List<OrderStatusHistory> results = em.createQuery(
                "SELECT osh FROM OrderStatusHistory osh WHERE osh.orderId = :orderId ORDER BY osh.changedAt DESC", 
                OrderStatusHistory.class)
                .setParameter("orderId", orderId)
                .setMaxResults(1)
                .getResultList();
            return results.isEmpty() ? null : results.get(0);
        } finally {
            em.close();
        }
    }
}
