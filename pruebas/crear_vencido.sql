-- Prueba WF3: crea un préstamo con fecha de devolución vencida
-- Descuenta el stock para mantener la consistencia del inventario
UPDATE equipos
SET stock_disponible = stock_disponible - 1
WHERE codigo = 'LAP-001';

INSERT INTO prestamos (carne, nombre_estudiante, id_equipo, cantidad, fecha_prestamo, fecha_devolucion_esperada)
SELECT '4090-23-5678', 'Carlos Caal Tiul', id_equipo, 1, '2026-10-10', '2026-10-15'
FROM equipos
WHERE codigo = 'LAP-001';