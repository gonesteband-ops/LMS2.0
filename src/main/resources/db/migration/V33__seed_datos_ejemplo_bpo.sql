-- V33__seed_datos_ejemplo_bpo.sql
-- Seed de ejemplo BPO (antes V24/V26)
-- Se ejecuta después de V25 (agrega columna codigo a subservicios_bpo) para evitar
-- el error "column codigo does not exist" cuando la BD inicia desde V23.

INSERT INTO cuentas_bpo (id, nombre, codigo, activa) VALUES
    ('a1000000-0000-0000-0000-000000000001','Gestor de ejemplo','GESTOR_EJEMPLO',TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO subservicios_bpo (id, nombre, codigo, cuenta_id) VALUES
    ('b2000000-0000-0000-0000-000000000001','carpeta de ejemplo','carpeta-de-ejemplo','a1000000-0000-0000-0000-000000000001')
ON CONFLICT (id) DO NOTHING;

-- Los contenidos de ejemplo solo se insertan si existe el usuario admin@lms.cl
-- (creado por DataInitializer al iniciar la app, después de las migraciones Flyway).
-- Así no viola la restricción NOT NULL de creado_por en BD nuevas (ej: producción en V23).
INSERT INTO contenidos_bpo (id, tipo, titulo, descripcion_corta, contenido_html, estado, vigencia_desde, vigencia_hasta, cuenta_id, subservicio_id, creado_por, created_at)
SELECT v.id::uuid, v.tipo, v.titulo, v.descripcion_corta, v.contenido_html, v.estado, v.vigencia_desde::date, v.vigencia_hasta::date, v.cuenta_id::uuid, v.subservicio_id::uuid, adm.id, v.created_at::timestamp
FROM (VALUES
    ('c3000000-0000-0000-0000-000000000001','procedimiento','Ejemplo de publicación','Contenido de ejemplo para el gestor','<p>Este es un ejemplo de contenido para el Gestor de Ejemplo.</p>','publicado',CURRENT_DATE,CURRENT_DATE+90,'a1000000-0000-0000-0000-000000000001','b2000000-0000-0000-0000-000000000001',CURRENT_DATE),
    ('c3000000-0000-0000-0000-000000000002','noticia','Nuevos horarios de atencion por cierre','Horarios modificados','<h3>Nuevos Horarios</h3><p>Lunes a viernes 09:00-17:00. Sabado cerrado.</p>','publicado',CURRENT_DATE,CURRENT_DATE+90,'a1000000-0000-0000-0000-000000000001','b2000000-0000-0000-0000-000000000001',CURRENT_DATE-1),
    ('c3000000-0000-0000-0000-000000000003','oferta','Portabilidad numerica','Pasos para portabilidad','<p>1. Solicitar datos. 2. Verificar deuda.</p>','publicado',CURRENT_DATE,CURRENT_DATE+90,'a1000000-0000-0000-0000-000000000001','b2000000-0000-0000-0000-000000000001',CURRENT_DATE-2)
) AS v(id, tipo, titulo, descripcion_corta, contenido_html, estado, vigencia_desde, vigencia_hasta, cuenta_id, subservicio_id, created_at)
JOIN usuarios adm ON adm.email = 'admin@lms.cl'
ON CONFLICT (id) DO NOTHING;

INSERT INTO usuarios_cuenta_bpo (usuario_id, cuenta_id, created_by)
SELECT u.id,c.id,u.id FROM usuarios u, cuentas_bpo c WHERE u.email='admin@lms.cl' AND c.codigo='GESTOR_EJEMPLO'
AND NOT EXISTS (SELECT 1 FROM usuarios_cuenta_bpo uc WHERE uc.usuario_id=u.id AND uc.cuenta_id=c.id);

INSERT INTO usuarios_cuenta_bpo (usuario_id, cuenta_id, created_by)
SELECT u.id,c.id,(SELECT id FROM usuarios WHERE email='admin@lms.cl') FROM usuarios u, cuentas_bpo c
WHERE u.email='estudiante@lms.cl' AND c.codigo='GESTOR_EJEMPLO'
AND NOT EXISTS (SELECT 1 FROM usuarios_cuenta_bpo uc WHERE uc.usuario_id=u.id AND uc.cuenta_id=c.id);
