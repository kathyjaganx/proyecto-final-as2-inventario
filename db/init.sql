-- =====================================================
-- Proyecto Final - Análisis de Sistemas II
-- Control de inventario y préstamos de laboratorio
-- Autora: Irma Yaritza Katiuxca Jagan Xol - 4090-23-9748
-- =====================================================

-- Inventario de equipos del laboratorio
CREATE TABLE equipos (
    id_equipo        SERIAL PRIMARY KEY,
    codigo           VARCHAR(20)  NOT NULL UNIQUE,
    nombre           VARCHAR(100) NOT NULL,
    categoria        VARCHAR(50)  NOT NULL,
    stock_total      INT NOT NULL CHECK (stock_total >= 0),
    stock_disponible INT NOT NULL CHECK (stock_disponible >= 0),
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_stock_coherente CHECK (stock_disponible <= stock_total)
);

-- Préstamos registrados
CREATE TABLE prestamos (
    id_prestamo               SERIAL PRIMARY KEY,
    carne                     VARCHAR(15)  NOT NULL,
    nombre_estudiante         VARCHAR(100) NOT NULL,
    id_equipo                 INT NOT NULL REFERENCES equipos(id_equipo),
    cantidad                  INT NOT NULL CHECK (cantidad > 0),
    fecha_prestamo            DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_devolucion_esperada DATE NOT NULL,
    fecha_devolucion_real     DATE,
    estado                    VARCHAR(15) NOT NULL DEFAULT 'ACTIVO'
                              CHECK (estado IN ('ACTIVO', 'DEVUELTO', 'VENCIDO')),
    creado_en                 TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_fechas CHECK (fecha_devolucion_esperada >= fecha_prestamo)
);

-- Bitácora de auditoría: toda operación queda registrada
CREATE TABLE auditoria (
    id_auditoria SERIAL PRIMARY KEY,
    fecha        TIMESTAMP NOT NULL DEFAULT NOW(),
    workflow     VARCHAR(100) NOT NULL,
    accion       VARCHAR(50)  NOT NULL,
    resultado    VARCHAR(15)  NOT NULL CHECK (resultado IN ('EXITO', 'RECHAZADO', 'ERROR')),
    detalle      JSONB
);

-- Registro de errores capturados por el Error Trigger de n8n
CREATE TABLE errores (
    id_error     SERIAL PRIMARY KEY,
    fecha        TIMESTAMP NOT NULL DEFAULT NOW(),
    workflow     VARCHAR(100),
    nodo         VARCHAR(100),
    mensaje      TEXT NOT NULL,
    id_ejecucion VARCHAR(50),
    datos        JSONB
);

-- Índices para las consultas más frecuentes
CREATE INDEX idx_prestamos_estado ON prestamos(estado);
CREATE INDEX idx_prestamos_vencimiento ON prestamos(fecha_devolucion_esperada);

-- Datos iniciales de prueba
INSERT INTO equipos (codigo, nombre, categoria, stock_total, stock_disponible) VALUES
('LAP-001', 'Laptop Dell Latitude', 'Computo',      5,  5),
('PRO-001', 'Proyector Epson',      'Audiovisual',  2,  2),
('MUL-001', 'Multímetro digital',   'Electrónica',  8,  8),
('ARD-001', 'Kit Arduino UNO',      'Electrónica', 10, 10),
('HDM-001', 'Cable HDMI 3m',        'Accesorios',  15, 15);