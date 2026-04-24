package com.agencias.backend.repository;

import com.agencias.backend.model.OrderStatusHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;

public class OrderStatusRepository {
    private final EntityManagerFactory emf;

    private static boolean isOrderStatusCheckConstraintViolation(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String m = t.getMessage();
            if (m == null) {
                continue;
            }
            if (m.contains("CHK_OSH_STATUS") || m.contains("ORA-02290")) {
                return true;
            }
        }
        return false;
    }

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
            if (isOrderStatusCheckConstraintViolation(e)) {
                throw new IllegalArgumentException(
                    "Oracle rechaza el estado: el CHECK CHK_OSH_STATUS de ORDER_STATUS_HISTORY no incluye ese valor "
                        + "(p. ej. CANCELLED). Ejecuta el script SQL del repositorio: fabrica/database/08_order_status_allow_cancelled.sql "
                        + "con el usuario FABRICA y reinicia el backend si hace falta.");
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
