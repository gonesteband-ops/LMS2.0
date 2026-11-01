package com.lms.util;

import java.util.List;
import java.util.Map;

/**
 * Categorías de eventos de auditoría para el filtro jerárquico.
 * 
 * Cada categoría agrupa uno o más tipo_evento de la tabla eventos_auditoria.
 * El frontend usa este enum para construir los 2 combos:
 *   1er combo: categoría (TODOS, USUARIOS, CURSOS, etc.)
 *   2do combo: tipos de evento específicos dentro de la categoría seleccionada
 */
public enum AuditCategoria {

    TODOS("Todos los eventos", null),

    SEGURIDAD("Seguridad", List.of(
            "LOGIN", "AUTH_LOGIN_EXITOSO", "AUTH_BLOQUEADO",
            "AUTH_CUENTA_BLOQUEADA", "INTENTO_BLOQUEADO"
    )),

    USUARIOS("Usuarios", List.of(
            "USUARIO_REGISTRADO", "USUARIO_CREADO_ADMIN",
            "USUARIO_ACTUALIZADO", "USUARIO_ELIMINADO",
            "USUARIO_SEMILLA_CREADO"
    )),

    ROLES("Roles", List.of(
            "ROL_CREADO"
    )),

    CONSENTIMIENTO("Consentimiento", List.of(
            "CONSENTIMIENTO_OTORGADO"
    )),

    FIRMAS("Firmas", List.of(
            "FIRMA_REGISTRADA"
    )),

    CURSOS("Cursos", List.of(
            "CURSO_CREADO", "CURSO_ACTUALIZADO", "CURSO_ELIMINADO"
    )),

    EVALUACIONES("Evaluaciones", List.of(
            "EVALUACION_CREADA", "EVALUACION_ACTUALIZADA", "EVALUACION_ELIMINADA"
    )),

    DOCUMENTOS("Documentos", List.of(
            "DOCUMENTO_SUBIDO", "DOCUMENTO_ELIMINADO"
    )),

    SCORM("SCORM", List.of(
            "SCORM_SUBIDO", "SCORM_ELIMINADO"
    )),

    INSCRIPCIONES("Inscripciones", List.of(
            "INSCRIPCION_CREADA", "INSCRIPCION_MASIVA",
            "INSCRIPCION_ELIMINADA", "INSCRIPCION_COMPLETADA_FORZADA",
            "CURSO_COMPLETADO"
    )),

    EXPORTACIONES("Exportaciones", List.of(
            "EXPORTACION_SOLICITADA", "EXPORTACIONES_HISTORIAL_LIMPIADO"
    )),

    NOTIFICACIONES("Notificaciones", List.of(
            "NOTIFICACION_ENVIADA"
    ));

    private final String nombre;
    private final List<String> tiposEvento;

    AuditCategoria(String nombre, List<String> tiposEvento) {
        this.nombre = nombre;
        this.tiposEvento = tiposEvento;
    }

    public String getNombre() {
        return nombre;
    }

    public List<String> getTiposEvento() {
        return tiposEvento;
    }

    /**
     * Retorna el listado de categorías para el frontend (excluyendo TODOS).
     */
    public static List<Map<String, Object>> listarCategorias() {
        return List.of(values()).stream()
                .filter(c -> c != TODOS)
                .map(c -> Map.<String, Object>of(
                        "id", c.name(),
                        "nombre", c.getNombre()
                ))
                .toList();
    }

    /**
     * Retorna los tipos de evento para una categoría por su ID.
     */
    public static List<String> listarTiposEvento(String categoriaId) {
        if (categoriaId == null || categoriaId.isBlank()) {
            return null;
        }
        try {
            AuditCategoria cat = AuditCategoria.valueOf(categoriaId.toUpperCase());
            return cat.getTiposEvento();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Retorna los tipos de evento para una categoría dada (mapeado para API).
     */
    public static List<Map<String, Object>> listarTiposPorCategoria(String categoriaId) {
        if (categoriaId == null || categoriaId.isBlank()) {
            return List.of();
        }
        try {
            AuditCategoria cat = AuditCategoria.valueOf(categoriaId.toUpperCase());
            if (cat.getTiposEvento() == null) {
                return List.of();
            }
            return cat.getTiposEvento().stream()
                    .map(t -> Map.<String, Object>of(
                            "id", t,
                            "nombre", t
                    ))
                    .toList();
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }
}
