"""
Test de verificación de enlaces en templates del dashboard.
Verifica que las tarjetas KPI tengan los href correctos.
"""

import os
import re

BASE_DIR = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'templates')

def normalize_path(p):
    return os.path.normpath(os.path.join(BASE_DIR, p))

def test_kpi_card_has_href():
    """Verifica que la tarjeta Evaluaciones Pendientes tenga href correcto"""
    path = normalize_path('dashboard/index.html')
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Buscar la tarjeta de Evaluaciones Pendientes con href
    assert 'href="/dashboard/evaluaciones-pendientes"' in content, \
        "Falta href='/dashboard/evaluaciones-pendientes' en la tarjeta Evaluaciones Pendientes"
    assert 'href="/cursos"' in content, \
        "Falta href='/cursos' en la tarjeta Cursos Activos"
    assert 'href="/dashboard/documentos"' in content, \
        "Falta href='/dashboard/documentos' en la tarjeta Documentos"
    assert 'href="/dashboard/firmas"' in content, \
        "Falta href='/dashboard/firmas' en la tarjeta Firmas Electrónicas"

    print("[OK] Todas las tarjetas KPI tienen los href correctos")

def test_title_inicio():
    """Verifica que el título sea 'Inicio' en lugar de 'Panel Principal'"""
    path = normalize_path('dashboard/index.html')
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    assert 'Panel Principal' not in content, \
        "El título 'Panel Principal' aún existe en index.html"
    assert '>Inicio</h1>' in content, \
        "Falta el título 'Inicio' en el h1"

    print("[OK] Titulo cambiado a 'Inicio' correctamente")

def test_sidebar_inicio():
    """Verifica que el sidebar diga 'Inicio'"""
    path = normalize_path('fragments/sidebar.html')
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    assert 'Panel Principal' not in content, \
        "'Panel Principal' aún existe en sidebar.html"
    # Buscar Inicio como texto dentro de una etiqueta <a> en el sidebar
    assert '>Inicio</a>' in content or 'Inicio\n' in content, \
        "Falta 'Inicio' en sidebar.html"

    print("[OK] Sidebar actualizado a 'Inicio'")

def test_new_template_exists():
    """Verifica que el template evaluaciones-pendientes.html existe"""
    path = normalize_path('dashboard/evaluaciones-pendientes.html')
    assert os.path.exists(path), \
        "El template dashboard/evaluaciones-pendientes.html no existe"

    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    assert '/api/v1/dashboard/evaluaciones-pendientes' in content, \
        "Falta el endpoint API en el template"
    assert 'Rendir Evaluación' in content, \
        "Falta el botón Rendir Evaluación en el template"

    print("[OK] Template evaluaciones-pendientes.html creado correctamente")

def test_sidebar_toggle_exists():
    """Verifica que el botón sidebar-toggle existe en header.html"""
    path = normalize_path('fragments/header.html')
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    assert 'sidebar-toggle' in content, \
        "Falta el botón sidebar-toggle en header.html"
    assert 'toggleSidebar' in content, \
        "Falta la función toggleSidebar() en header.html"

    print("[OK] Boton sidebar-toggle presente en header.html")

if __name__ == '__main__':
    tests = [
        test_kpi_card_has_href,
        test_title_inicio,
        test_sidebar_inicio,
        test_new_template_exists,
        test_sidebar_toggle_exists,
    ]
    passed = 0
    failed = 0
    for t in tests:
        try:
            t()
            passed += 1
        except AssertionError as e:
            print(f"[FAIL] {t.__name__}: {e}")
            failed += 1
        except Exception as e:
            print(f"[FAIL] {t.__name__}: ERROR - {e}")
            failed += 1

    print(f"\n{'='*40}")
    print(f"Resultados: {passed} OK, {failed} FALLARON")
    if failed > 0:
        exit(1)
