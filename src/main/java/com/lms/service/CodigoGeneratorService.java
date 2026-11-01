package com.lms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de generación de códigos alfanuméricos correlativos.
 * <p>
 * Formato: {@code PREFIJO + NNN} donde NNN es un número correlativo
 * con padding a 3 dígitos.
 * <p>
 * Ejemplos: {@code EVA001}, {@code SCO042}, {@code DOC153}
 * <p>
 * Utiliza la tabla {@code secuencias_codigos} en BD para garantizar
 * atomicidad y evitar duplicados en entornos multi-hilo.
 */
@Service
public class CodigoGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(CodigoGeneratorService.class);

    private static final int PADDING = 3;

    private final JdbcTemplate jdbcTemplate;

    public CodigoGeneratorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Genera un código alfanumérico correlativo usando la función
     * PostgreSQL {@code generar_codigo_alfanumerico}.
     *
     * @param prefijo Prefijo del código (EVA, SCO, DOC, etc.)
     * @return Código generado, ej: {@code EVA001}
     */
    @Transactional
    public String generarCodigo(String prefijo) {
        String prefijoUpper = prefijo.toUpperCase();

        try {
            String codigo = jdbcTemplate.queryForObject(
                "SELECT generar_codigo_alfanumerico(?)",
                String.class,
                prefijoUpper
            );
            log.debug("Código generado: {} para prefijo: {}", codigo, prefijoUpper);
            return codigo;
        } catch (Exception e) {
            log.error("Error generando código para prefijo: {}", prefijoUpper, e);
            // Fallback: generar código en memoria si la BD falla
            return generarCodigoFallback(prefijoUpper);
        }
    }

    /**
     * Genera código en memoria como fallback si la función de BD no está disponible.
     */
    private synchronized String generarCodigoFallback(String prefijo) {
        Integer ultimoNumero = jdbcTemplate.queryForObject(
            "SELECT ultimo_numero FROM secuencias_codigos WHERE prefijo = ? FOR UPDATE",
            Integer.class,
            prefijo
        );

        if (ultimoNumero == null) {
            jdbcTemplate.update(
                "INSERT INTO secuencias_codigos (prefijo, ultimo_numero) VALUES (?, 1)",
                prefijo
            );
            return prefijo + String.format("%0" + PADDING + "d", 1);
        }

        int nuevoNumero = ultimoNumero + 1;
        jdbcTemplate.update(
            "UPDATE secuencias_codigos SET ultimo_numero = ?, updated_at = CURRENT_TIMESTAMP WHERE prefijo = ?",
            nuevoNumero, prefijo
        );

        return prefijo + String.format("%0" + PADDING + "d", nuevoNumero);
    }

    /**
     * Obtiene el último código generado para un prefijo.
     */
    @Transactional(readOnly = true)
    public String obtenerUltimoCodigo(String prefijo) {
        Integer ultimoNumero = jdbcTemplate.queryForObject(
            "SELECT ultimo_numero FROM secuencias_codigos WHERE prefijo = ?",
            Integer.class,
            prefijo.toUpperCase()
        );
        if (ultimoNumero == null) {
            return null;
        }
        return prefijo.toUpperCase() + String.format("%0" + PADDING + "d", ultimoNumero);
    }
}
