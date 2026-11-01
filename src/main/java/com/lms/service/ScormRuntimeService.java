package com.lms.service;

import com.lms.model.ScormDatoAlumno;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio que implementa la API Runtime de SCORM 1.2 / 2004.
 * <p>
 * Maneja la sesión de comunicación entre el contenido SCORM (iframes)
 * y el backend LMS, incluyendo Initialize, Finish, GetValue, SetValue, Commit.
 * <p>
 * Implementa el patrón de error handling definido en el estándar SCORM.
 */
@Service
public class ScormRuntimeService {

    private final ScormService scormService;

    // Mapa de errores SCORM estándar
    private static final Map<Integer, String[]> ERROR_MAP = new LinkedHashMap<>();

    // Último error por hilo (simulación de sesión single-thread)
    private final Map<String, Integer> lastErrors = new ConcurrentHashMap<>();

    // Sesiones activas: sessionKey -> datos de sesión
    private final Map<String, ScormSession> activeSessions = new ConcurrentHashMap<>();

    static {
        // Errores SCORM 1.2 / 2004
        ERROR_MAP.put(0, new String[]{"No error", "Sin error"});
        ERROR_MAP.put(101, new String[]{"General exception", "Excepción general"});
        ERROR_MAP.put(201, new String[]{"Invalid argument error", "Argumento inválido"});
        ERROR_MAP.put(202, new String[]{"Element cannot have children", "El elemento no puede tener hijos"});
        ERROR_MAP.put(203, new String[]{"Element not an array. Cannot have children", "Elemento no es un arreglo"});
        ERROR_MAP.put(301, new String[]{"Not initialized", "No inicializado"});
        ERROR_MAP.put(401, new String[]{"Not implemented error", "No implementado"});
        ERROR_MAP.put(402, new String[]{"Invalid set value, element is a keyword", "Valor inválido, elemento es keyword"});
        ERROR_MAP.put(403, new String[]{"Element is read only", "Elemento es solo lectura"});
        ERROR_MAP.put(404, new String[]{"Element is write only", "Elemento es solo escritura"});
        ERROR_MAP.put(405, new String[]{"Incorrect data type", "Tipo de dato incorrecto"});
    }

    public ScormRuntimeService(ScormService scormService) {
        this.scormService = scormService;
    }

    /**
     * Inicializa una sesión de comunicación SCORM.
     *
     * @param paqueteId ID del paquete SCORM
     * @param usuarioId ID del usuario
     * @return "true" si se inicializó correctamente
     */
    public String lmsInitialize(UUID paqueteId, UUID usuarioId) {
        try {
            String sessionKey = generarSessionKey(paqueteId, usuarioId);
            ScormSession session = new ScormSession(paqueteId, usuarioId, UUID.randomUUID());
            activeSessions.put(sessionKey, session);

            setLastError(sessionKey, 0);
            return "true";
        } catch (Exception e) {
            setLastError(generarSessionKey(paqueteId, usuarioId), 101);
            return "false";
        }
    }

    /**
     * Finaliza una sesión de comunicación SCORM.
     *
     * @param paqueteId ID del paquete SCORM
     * @param usuarioId ID del usuario
     * @return "true" si se finalizó correctamente
     */
    public String lmsFinish(UUID paqueteId, UUID usuarioId) {
        try {
            String sessionKey = generarSessionKey(paqueteId, usuarioId);
            ScormSession session = activeSessions.remove(sessionKey);

            if (session == null) {
                setLastError(sessionKey, 301);
                return "false";
            }

            setLastError(sessionKey, 0);
            return "true";
        } catch (Exception e) {
            setLastError(generarSessionKey(paqueteId, usuarioId), 101);
            return "false";
        }
    }

    /**
     * Obtiene el valor de una clave del modelo de datos SCORM.
     *
     * @param paqueteId ID del paquete SCORM
     * @param usuarioId ID del usuario
     * @param clave     Clave del modelo de datos (ej: "cmi.core.score.raw")
     * @return Valor almacenado o cadena vacía si no existe
     */
    public String lmsGetValue(UUID paqueteId, UUID usuarioId, String clave) {
        try {
            String sessionKey = generarSessionKey(paqueteId, usuarioId);
            ScormSession session = activeSessions.get(sessionKey);

            if (session == null) {
                setLastError(sessionKey, 301);
                return "";
            }

            Optional<ScormDatoAlumno> dato = scormService.obtenerDatoAlumno(
                    paqueteId, usuarioId, session.getIntentoId(), clave
            );

            setLastError(sessionKey, 0);

            if (dato.isPresent()) {
                return dato.get().getValor();
            }

            return "";
        } catch (Exception e) {
            setLastError(generarSessionKey(paqueteId, usuarioId), 101);
            return "";
        }
    }

    /**
     * Establece el valor de una clave del modelo de datos SCORM.
     *
     * @param paqueteId ID del paquete SCORM
     * @param usuarioId ID del usuario
     * @param clave     Clave del modelo de datos
     * @param valor     Valor a almacenar
     * @return "true" si se almacenó correctamente
     */
    public String lmsSetValue(UUID paqueteId, UUID usuarioId, String clave, String valor) {
        try {
            String sessionKey = generarSessionKey(paqueteId, usuarioId);
            ScormSession session = activeSessions.get(sessionKey);

            if (session == null) {
                setLastError(sessionKey, 301);
                return "false";
            }

            scormService.registrarDatoAlumno(paqueteId, usuarioId, session.getIntentoId(), clave, valor);
            setLastError(sessionKey, 0);
            return "true";
        } catch (Exception e) {
            setLastError(generarSessionKey(paqueteId, usuarioId), 101);
            return "false";
        }
    }

    /**
     * Hace commit de los valores almacenados (persistencia inmediata).
     *
     * @param paqueteId ID del paquete SCORM
     * @param usuarioId ID del usuario
     * @return "true" si se realizó el commit correctamente
     */
    public String lmsCommit(UUID paqueteId, UUID usuarioId) {
        try {
            String sessionKey = generarSessionKey(paqueteId, usuarioId);
            ScormSession session = activeSessions.get(sessionKey);

            if (session == null) {
                setLastError(sessionKey, 301);
                return "false";
            }

            // Los datos ya se persisten en LMSSetValue, este método confirma
            setLastError(sessionKey, 0);
            return "true";
        } catch (Exception e) {
            setLastError(generarSessionKey(paqueteId, usuarioId), 101);
            return "false";
        }
    }

    /**
     * Obtiene el código del último error ocurrido.
     *
     * @return Código de error (0 = sin error)
     */
    public int lmsGetLastError() {
        return lastErrors.getOrDefault("default", 0);
    }

    /**
     * Obtiene el mensaje descriptivo de un código de error.
     *
     * @param errorCode Código de error
     * @return Mensaje de error en español
     */
    public String lmsGetErrorString(int errorCode) {
        String[] errorInfo = ERROR_MAP.get(errorCode);
        if (errorInfo != null) {
            return errorInfo[1]; // Español
        }
        return "Error desconocido (" + errorCode + ")";
    }

    /**
     * Obtiene diagnóstico adicional de un código de error.
     *
     * @param errorCode Código de error
     * @return Diagnóstico del error
     */
    public String lmsGetDiagnostic(int errorCode) {
        String[] errorInfo = ERROR_MAP.get(errorCode);
        if (errorInfo != null) {
            return errorInfo[0] + " - " + errorInfo[1];
        }
        return "Código de error no reconocido: " + errorCode;
    }

    // --- Métodos internos ---

    private String generarSessionKey(UUID paqueteId, UUID usuarioId) {
        return paqueteId.toString() + ":" + usuarioId.toString();
    }

    private void setLastError(String sessionKey, int errorCode) {
        lastErrors.put(sessionKey, errorCode);
        lastErrors.put("default", errorCode);
    }

    /**
     * Clase interna que representa una sesión SCORM activa.
     */
    private static class ScormSession {
        private final UUID paqueteId;
        private final UUID usuarioId;
        private final UUID intentoId;

        public ScormSession(UUID paqueteId, UUID usuarioId, UUID intentoId) {
            this.paqueteId = paqueteId;
            this.usuarioId = usuarioId;
            this.intentoId = intentoId;
        }

        public UUID getIntentoId() {
            return intentoId;
        }
    }
}
