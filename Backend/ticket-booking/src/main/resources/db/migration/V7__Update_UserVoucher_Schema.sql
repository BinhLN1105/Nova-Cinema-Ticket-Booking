-- Migrate the 'is_used' boolean to a 'status' VARCHAR
ALTER TABLE user_vouchers 
  ALTER COLUMN is_used TYPE VARCHAR(20) 
  USING (CASE WHEN is_used THEN 'USED' ELSE 'AVAILABLE' END);

-- Rename column to 'status'
ALTER TABLE user_vouchers RENAME COLUMN is_used TO status;

-- Add a default value to the new column
ALTER TABLE user_vouchers ALTER COLUMN status SET DEFAULT 'AVAILABLE';
