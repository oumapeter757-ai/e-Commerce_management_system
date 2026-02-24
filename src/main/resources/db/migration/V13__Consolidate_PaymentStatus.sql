-- V13: Consolidate PaymentStatus COMPLETED -> SUCCESSFUL
-- COMPLETED was redundant alongside SUCCESSFUL. All code now uses SUCCESSFUL only.
UPDATE payments SET status = 'SUCCESSFUL' WHERE status = 'COMPLETED';

