package com.agencias.backend.repository;

import com.agencias.backend.model.OrderHeader;
import com.agencias.backend.model.OrderStatusHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class OrderRepository {
    private final EntityManagerFactory emf;

    public OrderRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public OrderHeader save(OrderHeader order) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            if (order.getOrderId() == null) {
                em.persist(order);
            } else {
                order = em.merge(order);
            }
            em.getTransaction().commit();
            return order;
        } finally {
            em.close();
        }
    }

    public Optional<OrderHeader> findById(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            OrderHeader order = em.find(OrderHeader.class, id);
            return Optional.ofNullable(order);
        } finally {
            em.close();
        }
    }

    public Optional<OrderHeader> findByOrderNumber(String orderNumber) {
        EntityManager em = emf.createEntityManager();
        try {
            List<OrderHeader> results = em.createQuery(
                "SELECT o FROM OrderHeader o WHERE o.orderNumber = :orderNumber", OrderHeader.class)
                .setParameter("orderNumber", orderNumber)
                .getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }

    public List<OrderHeader> findByUserId(Long userId) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                "SELECT o FROM OrderHeader o WHERE o.userId = :userId ORDER BY o.createdAt DESC", OrderHeader.class)
                .setParameter("userId", userId)
                .getResultList();
        } finally {
            em.close();
        }
    }

    public List<OrderHeader> findAll() {
        return findAllFiltered(null, null, null, null);
    }

    /**
     * Lista pedidos con filtros opcionales. Si status no es null, solo pedidos cuyo último estado sea ese.
     */
    public List<OrderHeader> findAllFiltered(String status, Long userId, Date fromDate, Date toDate) {
        EntityManager em = emf.createEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT o FROM OrderHeader o WHERE 1=1 ");
            if (userId != null) jpql.append("AND o.userId = :userId ");
            if (fromDate != null) jpql.append("AND o.createdAt >= :fromDate ");
            if (toDate != null) jpql.append("AND o.createdAt <= :toDate ");
            jpql.append("ORDER BY o.createdAt DESC");

            var q = em.createQuery(jpql.toString(), OrderHeader.class);
            if (userId != null) q.setParameter("userId", userId);
            if (fromDate != null) q.setParameter("fromDate", fromDate);
            if (toDate != null) q.setParameter("toDate", toDate);
            List<OrderHeader> list = q.getResultList();

            if (status != null && !status.isBlank()) {
                OrderStatusRepository statusRepo = new OrderStatusRepository(emf);
                list = list.stream()
                    .filter(o -> {
                        OrderStatusHistory latest = statusRepo.findLatestByOrderId(o.getOrderId());
                        return latest != null && status.equalsIgnoreCase(latest.getStatus());
                    })
                    .collect(java.util.stream.Collectors.toList());
            }
            return list;
        } finally {
            em.close();
        }
    }
}
