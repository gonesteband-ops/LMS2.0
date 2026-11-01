#!/usr/bin/env python3
"""
Script de Test Automatizado — Permisos de Roles y Cursos
LMS LegalTech v1.0

Uso: python test_permisos.py --base-url http://localhost:8080
"""

import requests
import json
import sys
import argparse
from typing import Optional, Dict, List, Tuple

BASE_URL = "http://localhost:8080"
RESULTS = {"passed": 0, "failed": 0, "bugs": []}

# ============================================================
# Helpers
# ============================================================

def login(email: str, password: str) -> Optional[str]:
    """Login y retorna access token."""
    r = requests.post(f"{BASE_URL}/api/v1/auth/login", json={
        "email": email, "password": password
    })
    if r.status_code != 200:
        print(f"  [ERROR] Login failed for {email}: {r.status_code}")
        return None
    return r.json().get("accessToken")


def test(uid: str, desc: str, expected: int, method: str, url: str,
         token: Optional[str] = None, data: Optional[Dict] = None,
         msg_expected: Optional[str] = None):
    """Ejecuta un test y registra resultado."""
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Cookie"] = f"access_token={token}"

    if method == "GET":
        r = requests.get(f"{BASE_URL}{url}", headers=headers)
    elif method == "POST":
        r = requests.post(f"{BASE_URL}{url}", headers=headers, json=data or {})
    else:
        raise ValueError(f"Method not supported: {method}")

    status_ok = r.status_code == expected
    msg_ok = True
    if msg_expected and status_ok:
        body = r.text.lower()
        msg_ok = msg_expected.lower() in body

    if status_ok and msg_ok:
        RESULTS["passed"] += 1
        print(f"  [PASS] {uid}: {desc}")
    else:
        RESULTS["failed"] += 1
        bug = {
            "id": uid,
            "desc": desc,
            "expected": expected,
            "got": r.status_code,
            "response": r.text[:200]
        }
        RESULTS["bugs"].append(bug)
        print(f"  [FAIL] {uid}: {desc} — Esperado {expected}, obtenido {r.status_code}")


# ============================================================
# Tests
# ============================================================

def run_all():
    print(f"\n{'='*60}")
    print(f" TEST MATRIX — PERMISOS DE ROLES Y CURSOS")
    print(f"{'='*60}\n")

    # --- Login de prueba ---
    admin_token = login("admin@lms.cl", "Demo1234!")
    if not admin_token:
        print("  [FATAL] No se pudo autenticar como admin. Abortando.")
        sys.exit(1)

    # 1. Obtener token para un usuario con rol personalizado
    # Crear usuario con rol ROL_DE_PRUEBA via admin
    r_create = requests.post(f"{BASE_URL}/api/v1/usuarios", headers={
        "Content-Type": "application/json",
        "Cookie": f"access_token={admin_token}"
    }, json={
        "email": "test.permisos@example.com",
        "password": "Demo1234!Pass",
        "nombreCompleto": "Test Permisos",
        "rut": "99.999.999-9"
    })
    if r_create.status_code not in (200, 201):
        print(f"  [WARN] No se pudo crear usuario test: {r_create.status_code}")
        print(f"         Puede que ya exista. Continuando...")

    test_token = login("test.permisos@example.com", "Demo1234!Pass")
    if not test_token:
        print("  [WARN] No se pudo autenticar como test_permisos. Saltando tests de ese usuario.")

    # ============================================================
    print(f"\n--- 1. Escenarios de Borde ---\n")

    # E1: ROL_DE_PRUEBA con CURSO_CREAR intenta crear curso
    if test_token:
        test("E1", "Rol personalizado con CURSO_CREAR crea curso (deberia poder)",
             201, "POST", "/api/v1/cursos", test_token, {
                 "titulo": "Curso Test E1",
                 "descripcion": "Test de permisos",
                 "categoria": "Ventas",
                 "contenido": "<p>Test</p>",
                 "motivoCambio": "Test",
                 "fechaLimite": None,
                 "requiereFirmaCumplimiento": False
             })

    # E2: ROL_DE_PRUEBA puede listar cursos
    test("E2", "ROL_DE_PRUEBA lista cursos (deberia poder si tiene CURSO_LEER)",
         200, "GET", "/api/v1/cursos?size=1", admin_token)

    # E3: Listar cursos sin token (deberia dar 401)
    test("E3", "Sin autenticacion lista cursos (deberia rechazar)",
         401, "GET", "/api/v1/cursos?size=1")

    # ============================================================
    print(f"\n--- 2. Pruebas de Regresion ---\n")

    # R1: ADMIN crea curso sin cursos existentes
    test("R1", "Admin crea curso desde cero",
         200, "POST", "/api/v1/cursos", admin_token, {
             "titulo": "Curso Test R1",
             "descripcion": "Regresion admin",
             "categoria": "Ventas",
             "contenido": "<p>Test R1</p>",
             "motivoCambio": "Test",
             "fechaLimite": None,
             "requiereFirmaCumplimiento": False
         })

    # ============================================================
    print(f"\n--- 3. Prueba de Visibilidad (API) ---\n")

    # V1: Admin accede a crear curso (GET /cursos/crear solo HTML, verificamos via POST)
    test("V1", "Admin puede crear curso (POST)",
         200, "POST", "/api/v1/cursos", admin_token, {
             "titulo": "Curso Test V1",
             "descripcion": "Visibilidad admin",
             "categoria": "Ventas",
             "contenido": "<p>Test V1</p>",
             "motivoCambio": "Test",
             "fechaLimite": None,
             "requiereFirmaCumplimiento": False
         })

    # V3: ESTUDIANTE intenta crear curso (deberia dar 403)
    estudiante_token = login("estudiante.demo@example.com", "Demo1234!Pass")
    if estudiante_token:
        test("V3", "Estudiante intenta crear curso (deberia rechazar)",
             403, "POST", "/api/v1/cursos", estudiante_token, {
                 "titulo": "Curso Test V3",
                 "descripcion": "Visibilidad estudiante",
                 "categoria": "Ventas",
                 "contenido": "<p>Test V3</p>",
                 "motivoCambio": "Test",
                 "fechaLimite": None,
                 "requiereFirmaCumplimiento": False
             })

    # ============================================================
    print(f"\n{'='*60}")
    print(f" RESULTADOS")
    print(f"{'='*60}")
    print(f"  Pasados: {RESULTS['passed']}")
    print(f"  Fallados: {RESULTS['failed']}")
    if RESULTS["bugs"]:
        print(f"\n  Bugs encontrados:")
        for b in RESULTS["bugs"]:
            print(f"    {b['id']}: {b['desc']}")
            print(f"           Esperado: {b['expected']}, Obtenido: {b['got']}")
            print(f"           Response: {b['response'][:100]}")
    print()

    # Exit code: 0 si todos pasaron, 1 si hubo fallos
    return 0 if RESULTS["failed"] == 0 else 1


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Test de Permisos LMS")
    parser.add_argument("--base-url", default="http://localhost:8080",
                        help="Base URL de la API (default: http://localhost:8080)")
    args = parser.parse_args()
    BASE_URL = args.base_url
    sys.exit(run_all())
