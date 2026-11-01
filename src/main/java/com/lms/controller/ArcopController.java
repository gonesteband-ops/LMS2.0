package com.lms.controller;

import com.lms.service.ArcopService;
import com.lms.util.SecurityUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/arcop")
public class ArcopController {

    private final ArcopService arcopService;
    private final SecurityUtils securityUtils;

    public ArcopController(ArcopService arcopService, SecurityUtils securityUtils) {
        this.arcopService = arcopService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/acceso")
    @PreAuthorize("isAuthenticated()")
    public Object acceso(Authentication authentication,
                          @RequestHeader(name = "HX-Request", required = false) Boolean hxRequest) {
        UUID userId = securityUtils.getCurrentUserId(authentication);
        Map<String, Object> data = arcopService.obtenerAcceso(userId);
        if (hxRequest != null && hxRequest) {
            StringBuilder html = new StringBuilder();
            html.append("<div style=\"background:var(--bg-surface);border-radius:var(--border-radius-lg);padding:var(--spacing-md);\">");
            Map<String, Object> dp = (Map<String, Object>) data.get("datosPersonales");
            html.append("<table style=\"width:100%;border-collapse:collapse;\">");
            html.append("<tr style=\"border-bottom:1px solid var(--border-default);\"><td style=\"padding:0.5rem;font-weight:600;color:var(--text-secondary);\">Nombre</td><td style=\"padding:0.5rem;\">").append(dp.get("nombre")).append("</td></tr>");
            html.append("<tr style=\"border-bottom:1px solid var(--border-default);\"><td style=\"padding:0.5rem;font-weight:600;color:var(--text-secondary);\">Email</td><td style=\"padding:0.5rem;\">").append(dp.get("email")).append("</td></tr>");
            html.append("<tr style=\"border-bottom:1px solid var(--border-default);\"><td style=\"padding:0.5rem;font-weight:600;color:var(--text-secondary);\">RUT</td><td style=\"padding:0.5rem;\">").append(dp.get("rut")).append("</td></tr>");
            html.append("<tr style=\"border-bottom:1px solid var(--border-default);\"><td style=\"padding:0.5rem;font-weight:600;color:var(--text-secondary);\">Fecha Nacimiento</td><td style=\"padding:0.5rem;\">").append(dp.get("fechaNacimiento")).append("</td></tr>");
            html.append("<tr style=\"border-bottom:1px solid var(--border-default);\"><td style=\"padding:0.5rem;font-weight:600;color:var(--text-secondary);\">Fecha Registro</td><td style=\"padding:0.5rem;\">").append(dp.get("fechaRegistro")).append("</td></tr>");
            html.append("<tr style=\"border-bottom:1px solid var(--border-default);\"><td style=\"padding:0.5rem;font-weight:600;color:var(--text-secondary);\">Cursos Inscritos</td><td style=\"padding:0.5rem;\">").append(data.get("cursosInscritos")).append("</td></tr>");
            html.append("<tr style=\"border-bottom:1px solid var(--border-default);\"><td style=\"padding:0.5rem;font-weight:600;color:var(--text-secondary);\">Cursos Completados</td><td style=\"padding:0.5rem;\">").append(data.get("cursosCompletados")).append("</td></tr>");
            html.append("</table></div>");
            return ResponseEntity.ok().header("Content-Type", "text/html;charset=UTF-8").body(html.toString());
        }
        return ResponseEntity.ok(data);
    }

    @PutMapping("/rectificar")
    @PreAuthorize("isAuthenticated()")
    public Object rectificar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String rut,
            @RequestParam(required = false) LocalDate fechaNacimiento,
            Authentication authentication,
            @RequestHeader(name = "HX-Request", required = false) Boolean hxRequest) {
        try {
            UUID userId = securityUtils.getCurrentUserId(authentication);
            arcopService.rectificar(userId, nombre, email, rut, fechaNacimiento);
            if (hxRequest != null && hxRequest) {
                String ok = "<div style=\u0027padding:var(--spacing-md);background:rgba(16,185,129,0.1);border:1px solid #16a34a;border-radius:var(--border-radius-lg);font-size:var(--font-size-sm);color:#16a34a;\u0027 role=\u0027alert\u0027>Tus datos han sido actualizados correctamente.</div>";
                return ResponseEntity.ok().header("Content-Type", "text/html;charset=UTF-8").body(ok);
            }
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage().contains("email")
                    ? "Lo sentimos, este correo electronico ya esta siendo utilizado por otro usuario."
                    : e.getMessage();
            if (hxRequest != null && hxRequest) {
                String err = "<div style=\u0027padding:var(--spacing-md);background:#fef2f2;border:1px solid #fecaca;border-radius:var(--border-radius-lg);font-size:var(--font-size-sm);color:#991b1b;\u0027 role=\u0027alert\u0027>" + msg + "</div>";
                return ResponseEntity.badRequest().header("Content-Type", "text/html;charset=UTF-8").body(err);
            }
            return ResponseEntity.badRequest().body("{\"message\":\"" + msg + "\"}");
        }
    }

    @DeleteMapping("/suprimir")
    @PreAuthorize("isAuthenticated()")
    public Object suprimir(
            @RequestParam(defaultValue = "Solicitud del titular") String motivo,
            Authentication authentication,
            @RequestHeader(name = "HX-Request", required = false) Boolean hxRequest) {
        try {
            UUID userId = securityUtils.getCurrentUserId(authentication);
            arcopService.solicitarSupresion(userId, motivo);
            if (hxRequest != null && hxRequest) {
                java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
                java.time.LocalDateTime gracia = ahora.plusDays(30);
                String fecha = String.format("%02d/%02d/%04d", gracia.getDayOfMonth(), gracia.getMonthValue(), gracia.getYear());
                String html = "<div style='padding:12px;background:#f0fdf4;border:1px solid #16a34a;border-radius:8px;'>"
                    + "<p style='font-size:14px;color:#16a34a;font-weight:600;margin:0 0 8px 0;'>Solicitud recibida</p>"
                    + "<p style='font-size:14px;color:#6b7280;margin:0 0 4px 0;'>Tu cuenta sera anonimizada el <strong>" + fecha + "</strong> (30 dias de gracia).</p>"
                    + "<p style='font-size:14px;color:#6b7280;margin:0 0 12px 0;'>Puedes cancelar dentro del periodo de gracia.</p>"
                    + "<button class='btn btn-primary' style='width:100%;justify-content:center;background:#16a34a;border-color:#16a34a;' hx-post='/api/v1/arcop/cancelar-supresion' hx-target='#eliminar-msg' hx-swap='innerHTML'>Solicitar Activacion de la cuenta</button></div>";
                return ResponseEntity.ok().header("Content-Type", "text/html;charset=UTF-8").body(html);
            }
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Solicitud recibida. 30 dias de gracia."));
        } catch (Exception e) {
            if (hxRequest != null && hxRequest) {
                return ResponseEntity.badRequest().header("Content-Type", "text/html;charset=UTF-8")
                    .body("<div style='padding:12px;background:#fef2f2;border:1px solid #fecaca;border-radius:8px;color:#991b1b;font-size:14px;'>Error al procesar la solicitud.</div>");
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Error al procesar la solicitud."));
        }
    }

    @PostMapping("/cancelar-supresion")
    @PreAuthorize("isAuthenticated()")
    public Object cancelarSupresion(Authentication authentication,
                                     @RequestHeader(name = "HX-Request", required = false) Boolean hxRequest) {
        try {
            UUID userId = securityUtils.getCurrentUserId(authentication);
            arcopService.cancelarSupresion(userId);
            if (hxRequest != null && hxRequest) {
                String html = "<div style='padding:12px;background:#f0fdf4;border:1px solid #16a34a;border-radius:8px;'>"
                    + "<p style='font-size:14px;color:#16a34a;font-weight:600;margin:0 0 12px 0;'>Cuenta reactivada correctamente</p>"
                    + "<button class='btn btn-danger' style='width:100%;justify-content:center;' hx-delete='/api/v1/arcop/suprimir' hx-confirm='Confirma la eliminacion de tu cuenta? Se anonimizaran tus datos. Tienes 30 dias para cancelar.' hx-target='#eliminar-msg' hx-swap='innerHTML'>Solicitar eliminacion de cuenta</button></div>";
                return ResponseEntity.ok().header("Content-Type", "text/html;charset=UTF-8").body(html);
            }
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            if (hxRequest != null && hxRequest) {
                return ResponseEntity.badRequest().header("Content-Type", "text/html;charset=UTF-8")
                    .body("<div style='padding:12px;background:#fef2f2;border:1px solid #fecaca;border-radius:8px;color:#991b1b;font-size:14px;'>Error al reactivar la cuenta.</div>");
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Error al reactivar la cuenta."));
        }
    }

    @PostMapping("/bloquear")
    @PreAuthorize("isAuthenticated()")
    public Object bloquear(Authentication authentication,
                            @RequestHeader(name = "HX-Request", required = false) Boolean hxRequest) {
        try {
            UUID userId = securityUtils.getCurrentUserId(authentication);
            arcopService.bloquear(userId);
            if (hxRequest != null && hxRequest) {
                return ResponseEntity.ok()
                        .header("Content-Type", "text/html;charset=UTF-8")
                        .body("Cuenta bloqueada temporalmente por 90 dias. Inicia sesion para reactivarla.");
            }
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            String msg = "Lo sentimos, ocurrio un error al bloquear tu cuenta. Intenta nuevamente.";
            if (hxRequest != null && hxRequest) {
                return ResponseEntity.badRequest()
                        .header("Content-Type", "text/html;charset=UTF-8")
                        .body(msg);
            }
            return ResponseEntity.badRequest().body("{\"message\":\"" + msg + "\"}");
        }
    }

    @GetMapping("/portabilidad")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> portabilidad(Authentication authentication) {
        UUID userId = securityUtils.getCurrentUserId(authentication);
        String json = arcopService.portabilidadJson(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("mis-datos.json").build().toString())
                .body(json);
    }
}
