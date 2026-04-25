-- Migrate the 'is_used' boolean to a 'status' VARCHAR (An toàn hơn với kiểm tra tồn tại)
DO $$ 
BEGIN 
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='user_vouchers' AND column_name='is_used') THEN
        
        -- 1. Đổi kiểu dữ liệu
        ALTER TABLE user_vouchers 
          ALTER COLUMN is_used TYPE VARCHAR(20) 
          USING (CASE WHEN is_used THEN 'USED' ELSE 'AVAILABLE' END);

        -- 2. Đổi tên cột sang 'status'
        ALTER TABLE user_vouchers RENAME COLUMN is_used TO status;

        -- 3. Thiết lập giá trị mặc định
        ALTER TABLE user_vouchers ALTER COLUMN status SET DEFAULT 'AVAILABLE';
        
    END IF;
END $$;
