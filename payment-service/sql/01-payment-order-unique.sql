-- Ejecutar UNA VEZ en payment-db antes de arrancar Payment actualizado.
-- No borra ni modifica pagos existentes. Si hay duplicados, aborta toda la transacción.
BEGIN;
LOCK TABLE payments IN SHARE ROW EXCLUSIVE MODE;
DO $$
BEGIN
    IF EXISTS (SELECT order_id FROM payments GROUP BY order_id HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Hay pedidos con pagos duplicados. Revisarlos manualmente antes de instalar.';
    END IF;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_order_id ON payments(order_id);
COMMIT;

-- Consulta de diagnóstico (ejecutar aparte si falla el bloque anterior):
-- SELECT order_id, array_agg(id ORDER BY id) AS payment_ids
-- FROM payments GROUP BY order_id HAVING count(*) > 1;
