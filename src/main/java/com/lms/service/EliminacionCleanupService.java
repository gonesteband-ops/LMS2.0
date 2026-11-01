package com.lms.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EliminacionCleanupService {

    private static final Logger log = LoggerFactory.getLogger(EliminacionCleanupService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Scheduled(cron = "${app.auditoria.cron-limpieza-eliminacion:0 0 5 * * ?}")
    @Transactional
    public void anonimizarUsuariosVencidos() {
        LocalDateTime ahora = LocalDateTime.now();

        int anonimizados = entityManager.createNativeQuery(
            "UPDATE usuarios SET " +
            "  nombre_completo = 'Usuario Anonimizado', " +
            "  email = 'anon-' || id || '@deleted.local', " +
            "  rut = NULL, " +
            "  fecha_nacimiento = NULL, " +
            "  estado_tratamiento = 'ANONIMIZADO', " +
            "  deleted_at = NOW() " +
            "WHERE deleted_at IS NOT NULL " +
            "  AND deleted_at <= :ahora " +
            "  AND estado_tratamiento != 'ANONIMIZADO'"
        ).setParameter("ahora", ahora).executeUpdate();

        if (anonimizados > 0) {
            log.info("anonimizarUsuariosVencidos: anonimizados {} usuarios con periodo de gracia vencido", anonimizados);
        }
    }
}
