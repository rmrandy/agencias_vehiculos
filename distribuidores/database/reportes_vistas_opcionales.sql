-- Opcional (SQL Server): vistas de solo lectura para BI / Excel.
-- NO es obligatorio para la aplicación: los reportes usan las tablas ORDER_HEADER, ORDER_ITEM, ORDER_STATUS_HISTORY.
-- Ejecutar en la base AgenciasDistribuidores si quieres consultas fijas desde DBeaver.

IF OBJECT_ID('dbo.vw_ultimo_estado_pedido', 'V') IS NOT NULL DROP VIEW dbo.vw_ultimo_estado_pedido;
GO
CREATE VIEW dbo.vw_ultimo_estado_pedido AS
SELECT h.OrderId, s.Status AS UltimoEstado, s.ChangedAt AS UltimoCambio
FROM ORDER_HEADER h
OUTER APPLY (
  SELECT TOP 1 Status, ChangedAt
  FROM ORDER_STATUS_HISTORY x
  WHERE x.OrderId = h.OrderId
  ORDER BY x.ChangedAt DESC, x.StatusId DESC
) s;
GO

IF OBJECT_ID('dbo.vw_mas_vendidos_local', 'V') IS NOT NULL DROP VIEW dbo.vw_mas_vendidos_local;
GO
CREATE VIEW dbo.vw_mas_vendidos_local AS
SELECT
  i.PartId,
  SUM(i.Qty) AS TotalQty,
  SUM(i.LineTotal) AS TotalImporte
FROM ORDER_ITEM i
INNER JOIN vw_ultimo_estado_pedido u ON u.OrderId = i.OrderId
INNER JOIN ORDER_HEADER h ON h.OrderId = i.OrderId
WHERE i.LineSource = 'LOCAL'
  AND i.PartId IS NOT NULL
  AND ISNULL(u.UltimoEstado, 'INITIATED') <> 'CANCELLED'
GROUP BY i.PartId;
GO
