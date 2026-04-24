-- Opcional (Oracle FABRICA): vistas de solo lectura para BI.
-- NO es obligatorio: el API /api/reporteria/* ya calcula los reportes en aplicación.

CREATE OR REPLACE VIEW vw_ultimo_estado_pedido AS
SELECT order_id,
       STATUS AS ultimo_estado,
       changed_at AS ultimo_cambio
FROM (
  SELECT h.*,
         ROW_NUMBER() OVER (PARTITION BY order_id ORDER BY changed_at DESC NULLS LAST, status_id DESC) AS rn
  FROM order_status_history h
)
WHERE rn = 1;

CREATE OR REPLACE VIEW vw_mas_vendidos AS
SELECT oi.part_id,
       SUM(oi.qty) AS total_qty,
       SUM(oi.line_total) AS total_importe
FROM order_item oi
JOIN order_header oh ON oh.order_id = oi.order_id
LEFT JOIN vw_ultimo_estado_pedido u ON u.order_id = oh.order_id
WHERE NVL(u.ultimo_estado, 'INITIATED') <> 'CANCELLED'
GROUP BY oi.part_id;
