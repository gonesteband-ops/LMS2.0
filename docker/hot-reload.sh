#!/bin/sh
# ============================================================
# Script de Hot Reload para LMS LegalTech (Docker)
# ============================================================
# Monitorea cambios en archivos fuente usando polling (find -newer)
# para compatibilidad con bind mounts en Windows/WSL2.
# Compila automáticamente y DevTools reinicia la app.
# ============================================================

SRC_DIR="/app/src"
POLL_INTERVAL=3

echo "============================================"
echo "  LMS LegalTech - Hot Reload Watcher"
echo "============================================"
echo "Monitoring: $SRC_DIR (polling every ${POLL_INTERVAL}s)"
echo ""

# Crear archivo de timestamp para detectar cambios
TIMESTAMP_FILE="/tmp/last-build.timestamp"
touch "$TIMESTAMP_FILE"

# Función para compilar
do_compile() {
    TIMESTAMP=$(date '+%H:%M:%S')
    echo "[$TIMESTAMP] Cambio detectado, recompilando..."
    cd /app
    mvn compile -q 2>&1
    if [ $? -eq 0 ]; then
        echo "[$TIMESTAMP] Compilacion exitosa - Devtools reiniciara la app"
        touch "$TIMESTAMP_FILE"
    else
        echo "[$TIMESTAMP] Error de compilacion - revisa el codigo"
    fi
}

# Iniciar la aplicación Spring Boot con spring-boot-devtools
echo "[$(date '+%H:%M:%S')] Iniciando aplicacion Spring Boot..."
cd /app
mvn spring-boot:run \
    -Dspring-boot.run.profiles=development \
    -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true" &
APP_PID=$!
echo "[$(date '+%H:%M:%S')] App PID: $APP_PID"

# Esperar a que la app termine de iniciar antes de monitorear
echo "[$(date '+%H:%M:%S')] Esperando 45s para el startup inicial..."
sleep 45

echo "[$(date '+%H:%M:%S')] Iniciando monitoreo de cambios en src/..."
echo ""

# Bucle de polling: verifica cambios periódicamente usando find -newer
# Esto es más compatible con bind mounts que inotifywait
while true; do
    # Buscar archivos .java modificados más recientemente que el timestamp
    CHANGED_FILES=$(find "$SRC_DIR/main/java" -name "*.java" -newer "$TIMESTAMP_FILE" 2>/dev/null)
    
    # También verificar resources clave
    CHANGED_RESOURCES=$(find "$SRC_DIR/main/resources" \
        \( -name "*.html" -o -name "*.css" -o -name "*.js" \
           -o -name "*.yml" -o -name "*.yaml" -o -name "*.properties" -o -name "*.xml" \) \
        -newer "$TIMESTAMP_FILE" 2>/dev/null)
    
    REBUILD=false
    
    if [ -n "$CHANGED_FILES" ]; then
        echo "[$(date '+%H:%M:%S')] Archivos Java modificados:"
        echo "$CHANGED_FILES" | while read -r f; do
            echo "  -> $(basename $(dirname $f))/$(basename $f)"
        done
        REBUILD=true
    fi
    
    if [ -n "$CHANGED_RESOURCES" ]; then
        # Verificar si hay cambios en archivos de configuración (requieren rebuild)
        CONFIG_CHANGED=$(echo "$CHANGED_RESOURCES" | grep -E 'application\.(yml|yaml|properties)$')
        TEMPLATE_CHANGED=$(echo "$CHANGED_RESOURCES" | grep -E '\.html$')
        
        if [ -n "$CONFIG_CHANGED" ]; then
            echo "[$(date '+%H:%M:%S')] Configuracion modificada:"
            echo "$CONFIG_CHANGED" | while read -r f; do
                echo "  -> $(basename $f)"
            done
            REBUILD=true
        fi
        
        # Los templates .html no requieren rebuild (thymeleaf.cache=false)
        if [ -n "$TEMPLATE_CHANGED" ]; then
            echo "[$(date '+%H:%M:%S')] Template HTML modificado (recarga automatica con thymeleaf.cache=false)"
        fi
        
        # CSS/JS no requieren rebuild
        STATIC_CHANGED=$(echo "$CHANGED_RESOURCES" | grep -v -E '\.html$' | grep -v -E 'application\.(yml|yaml|properties)$')
        if [ -n "$STATIC_CHANGED" ]; then
            echo "[$(date '+%H:%M:%S')] Recurso estatico modificado (no requiere reinicio)"
        fi
    fi
    
    if [ "$REBUILD" = true ]; then
        # Verificar que la app sigue corriendo
        if ! kill -0 $APP_PID 2>/dev/null; then
            echo "[$(date '+%H:%M:%S')] ADVERTENCIA: La app ya no esta corriendo (PID: $APP_PID)"
            echo "[$(date '+%H:%M:%S')] Saliendo del watcher..."
            exit 1
        fi
        do_compile
    fi
    
    sleep $POLL_INTERVAL
done
