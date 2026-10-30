INSERT INTO products (id, name, description, category, price_usd) VALUES
('P-101', 'Laptop Gamer XYZ', 'Laptop 16" RTX, 32GB RAM, 1TB SSD', 'Computadoras', 1200.00),
('P-102', 'Mouse Inalámbrico Pro', 'Mouse ergonómico 4000 DPI', 'Accesorios', 45.50),
('P-103', 'Monitor 27" 4K', 'Panel IPS 144Hz', 'Monitores', 380.00)
ON CONFLICT (id) DO NOTHING;

INSERT INTO inventory (product_id, available_units, warehouse_location, estimated_delivery_days) VALUES
('P-101', 15, 'Almacén Chihuahua - Pasillo B3', 2),
('P-102', 0, 'Almacén CDMX - Pasillo A1', 7),
('P-103', 8, 'Almacén Monterrey - Pasillo C2', 3)
ON CONFLICT (product_id) DO NOTHING;