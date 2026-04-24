-- Permitir estado CANCELLED (y estados legacy) en ORDER_STATUS_HISTORY.
-- Si ves ORA-02290 CHK_OSH_STATUS al cancelar o cambiar estado, ejecuta este script
-- conectado como esquema FABRICA (DBeaver / sqlplus).

ALTER TABLE ORDER_STATUS_HISTORY DROP CONSTRAINT CHK_OSH_STATUS;
ALTER TABLE ORDER_STATUS_HISTORY ADD CONSTRAINT CHK_OSH_STATUS CHECK (
  STATUS IN (
    'INITIATED',
    'CONFIRMED',
    'IN_PREPARATION',
    'PREPARING',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED'
  )
);
COMMIT;
