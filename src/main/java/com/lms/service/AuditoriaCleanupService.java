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
public class AuditoriaCleanupService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaCleanupService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Scheduled(cron = "${app.auditoria.cron-limpieza:0 0 4 * * ?}")
    @Transactional
    public void limpiarEventosAntiguos() {
        LocalDateTime corte = LocalDateTime.now().minusYears(2);
        int eliminados = entityManager.createNativeQuery(
                "DELETE FROM eventos_auditoria WHERE created_at < ? AND es_pii = true"
        ).setParameter(1, corte).executeUpdate();
        if (eliminados > 0) {
            log.info("limpiarEventosAntiguos: eliminados {} eventos de auditoria con PII anteriores a {}", eliminados, corte);
        }
    }
}
