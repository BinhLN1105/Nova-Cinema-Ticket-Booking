-- Drop the old check constraint to allow WALLET payment method
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_method_check;
