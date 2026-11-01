package com.lms.service;

import com.lms.model.IncidenteSeguridad;
import com.lms.repository.IncidenteSeguridadRepository;
import com.lms.util.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BreachDetectionService {

    private static final Logger log = LoggerFactory.getLogger(BreachDetectionService.class);
    private static final long WINDOW_MINUTES = 5;

    private final ConcurrentHashMap<String, LoginAttemptTracker> loginAttempts = new ConcurrentHashMap<>();
    private final IncidenteSeguridadRepository repository;

    public BreachDetectionService(IncidenteSeguridadRepository repository) {
        this.repository = repository;
    }

    public void registrarIntentoFallido(String email, String ip) {
        String key = email + "|" + ip;
        loginAttempts.compute(key, (k, v) -> {
            if (v == null || v.timestamp.isBefore(LocalDateTime.now().minusMinutes(WINDOW_MINUTES))) {
                return new LoginAttemptTracker(1, ip);
            }
            v.count.incrementAndGet();
            return v;
        });
        verificarAtaqueFuerzaBruta(email, ip);
    }

    void verificarAtaqueFuerzaBruta(String email, String ip) {
        String key = email + "|" + ip;
        LoginAttemptTracker tracker = loginAttempts.get(key);
        if (tracker == null || tracker.count.get() < 10) return;

        long ipsDistintas = loginAttempts.entrySet().stream()
                .filter(e -> e.getKey().startsWith(email + "|") && !e.getKey().endsWith("|" + ip))
                .filter(e -> e.getValue().timestamp.isAfter(LocalDateTime.now().minusMinutes(WINDOW_MINUTES)))
                .count();

        String tipo;
        String descripcion;
        if (ipsDistintas >= 3) {
            tipo = "ATAQUE_FUERZA_BRUTA_DISTRIBUIDO";
            descripcion = String.format("10+ intentos fallidos para %s desde %d IPs distintas en %d min", email, ipsDistintas + 1, WINDOW_MINUTES);
        } else if (tracker.count.get() > 50) {
            tipo = "ATAQUE_FUERZA_BRUTA";
            descripcion = String.format("50+ intentos fallidos para %s desde IP %s en %d min", email, ip, WINDOW_MINUTES);
        } else {
            return;
        }

        String nivelRiesgo = ipsDistintas >= 3 ? "ALTO" : "MEDIO";
        String contenido = tipo + "|" + descripcion + "|" + LocalDateTime.now();
        String hash = HashUtils.sha256(contenido);

        IncidenteSeguridad incidente = IncidenteSeguridad.builder()
                .tipo(tipo)
                .nivelRiesgo(nivelRiesgo)
                .descripcion(descripcion)
                .usuariosAfectados(1)
                .ipOrigen(ip)
                .fechaDeteccion(LocalDateTime.now())
                .estado("DETECTADO")
                .hashSha256(hash)
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(incidente);
        log.warn("[BREACH] {} - {}", tipo, descripcion);
    }

    private static class LoginAttemptTracker {
        final AtomicInteger count;
        final LocalDateTime timestamp;
        final String ip;

        LoginAttemptTracker(int initial, String ip) {
            this.count = new AtomicInteger(initial);
            this.timestamp = LocalDateTime.now();
            this.ip = ip;
        }
    }
}
