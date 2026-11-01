package com.lms.util;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class AnonymizationService {

    private static final Set<String> PII_KEYS = Set.of("email", "nombre", "nombrecompleto", "rut", "ip", "iporigen", "useragent");

    public String anonymizarEmail(String email) {
        if (email == null) return null;
        int arroba = email.indexOf('@');
        if (arroba <= 0) return "***@***";
        String local = email.substring(0, arroba);
        String dominio = email.substring(arroba + 1);
        int punto = dominio.indexOf('.');
        if (punto <= 0) return local.charAt(0) + "***@" + dominio.charAt(0) + "***";
        String tld = dominio.substring(punto);
        String dominioName = dominio.substring(0, punto);
        return local.charAt(0) + "***@" + dominioName.charAt(0) + "***" + tld;
    }

    public String anonymizarNombre(String nombre) {
        if (nombre == null) return null;
        String[] partes = nombre.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.length; i++) {
            if (i > 0) sb.append(" ");
            String p = partes[i];
            sb.append(p.charAt(0)).append("***");
        }
        return sb.toString();
    }

    public String anonymizarIP(String ip) {
        if (ip == null) return null;
        String[] octetos = ip.split("\\.");
        if (octetos.length < 4) return "***.***.***.***";
        return octetos[0] + "." + octetos[1] + ".***.***";
    }

    public String anonymizarRUT(String rut) {
        if (rut == null) return null;
        int guion = rut.indexOf('-');
        if (guion <= 0) return "***.***.***-*";
        String numero = rut.substring(0, guion);
        int ultimoPunto = numero.lastIndexOf('.');
        if (ultimoPunto <= 0) return "***.***.***-*";
        return numero.substring(0, ultimoPunto + 1) + "***-*";
    }

    public Map<String, Object> anonymizarMetadata(Map<String, Object> metadata) {
        Map<String, Object> result = new HashMap<>(metadata);
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey().toLowerCase();
            Object value = entry.getValue();
            if (!(value instanceof String s)) continue;
            if (PII_KEYS.contains(key)) {
                result.put(entry.getKey(), switch (key) {
                    case "email" -> anonymizarEmail(s);
                    case "nombre", "nombreCompleto" -> anonymizarNombre(s);
                    case "rut" -> anonymizarRUT(s);
                    case "ip", "ipOrigen" -> anonymizarIP(s);
                    default -> s;
                });
            }
        }
        return result;
    }

    public boolean containsPII(Map<String, Object> metadata) {
        if (metadata == null) return false;
        return metadata.keySet().stream()
                .map(String::toLowerCase)
                .anyMatch(PII_KEYS::contains);
    }
}
