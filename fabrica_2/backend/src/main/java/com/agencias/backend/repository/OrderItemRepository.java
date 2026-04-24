package com.agencias.backend.repository;

import com.agencias.backend.model.OrderItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class OrderItemRepository {
    private final EntityManagerFactory emf;

    public OrderItemRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public OrderItem save(OrderItem item) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            if (item.getOrderItemId() == null) {
                em.persist(item);
            } else {
                item = em.merge(item);
            }
            em.getTransaction().commit();
            return item;
        } finally {
            em.close();
        }
    }

    public List<OrderItem> findByOrderId(Long orderId) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                "SELECT oi FROM OrderItem oi WHERE oi.orderId = :orderId", OrderItem.class)
                .setParameter("orderId", orderId)
                .getResultList();
        } finally {
            em.close();
        }
    }

    /** Líneas de pedido para un conjunto de pedidos (reportería). */
    public List<OrderItem> findByOrderIds(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                "SELECT oi FROM OrderItem oi WHERE oi.orderId IN :ids", OrderItem.class)
                .setParameter("ids", orderIds)
                .getResultList();
        } finally {
            em.close();
        }
    }
}
