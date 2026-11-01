/**
 * SCORM API — Cliente JavaScript para comunicación con el LMS.
 *
 * Implementa la interfaz API_1484_11 (SCORM 2004) y API (SCORM 1.2).
 * Se inyecta en el iframe del paquete SCORM para interceptar
 * las llamadas del contenido y traducirlas a fetch contra el backend.
 *
 * @version 1.0.0
 */
(function(global) {
    'use strict';

    var SCORM_API = {
        // Configuración
        _paqueteId: null,
        _initialized: false,
        _terminated: false,
        _lastError: 0,
        _baseUrl: '/api/v1/scorm',
        _timeout: 10000, // 10 segundos

        // ============================================================
        // Inicialización
        // ============================================================

        /**
         * Inicializa el API SCORM con el ID del paquete.
         * Debe llamarse antes de cualquier otra operación.
         *
         * @param {string} paqueteId - UUID del paquete SCORM
         */
        inicializar: function(paqueteId) {
            this._paqueteId = paqueteId;
            this._initialized = false;
            this._terminated = false;
            this._lastError = 0;
        },

        // ============================================================
        // SCORM 1.2 API
        // ============================================================

        /**
         * LMSInitialize() — Inicializa la comunicación con el LMS.
         * SCORM 1.2: llama a this.LMSInitialize()
         *
         * @param {string} param - Parámetro (generalmente "")
         * @return {string} "true" o "false"
         */
        LMSInitialize: function(param) {
            if (this._terminated) {
                this._lastError = 101;
                return 'false';
            }
            if (this._initialized) {
                this._lastError = 101;
                return 'false';
            }

            this._lastError = 0;

            // Petición SÍNCRONA al backend (el contenido SCORM llama inmediatamente
            // a LMSGetValue/LMSSetValue después de LMSInitialize y necesita
            // que _initialized esté en true antes de continuar)
            var url = this._baseUrl + '/' + this._paqueteId + '/initialize';
            var xhr = new XMLHttpRequest();
            xhr.open('POST', url, false); // Síncrono
            xhr.timeout = this._timeout;
            xhr.setRequestHeader('Content-Type', 'application/json');

            try {
                xhr.send(JSON.stringify({}));
                if (xhr.status >= 200 && xhr.status < 300) {
                    var response = JSON.parse(xhr.responseText);
                    if (response.result === 'true') {
                        this._initialized = true;
                        this._lastError = 0;
                    } else {
                        this._lastError = 101;
                    }
                } else {
                    this._lastError = 101;
                }
            } catch(e) {
                this._lastError = 101;
            }

            return this._lastError === 0 ? 'true' : 'false';
        },

        /**
         * LMSFinish() — Finaliza la comunicación con el LMS.
         * SCORM 1.2: llama a this.LMSFinish()
         *
         * @param {string} param - Parámetro (generalmente "")
         * @return {string} "true" o "false"
         */
        LMSFinish: function(param) {
            if (!this._initialized) {
                this._lastError = 301;
                return 'false';
            }

            this._lastError = 0;

            // Petición síncrona para garantizar consistencia
            var url = this._baseUrl + '/' + this._paqueteId + '/finish';
            var xhr = new XMLHttpRequest();
            xhr.open('POST', url, false); // Síncrono
            xhr.timeout = this._timeout;
            xhr.setRequestHeader('Content-Type', 'application/json');

            try {
                xhr.send(JSON.stringify({}));
                if (xhr.status >= 200 && xhr.status < 300) {
                    var response = JSON.parse(xhr.responseText);
                    if (response.result === 'true') {
                        this._terminated = true;
                        this._initialized = false;
                        this._lastError = 0;
                    } else {
                        this._lastError = 101;
                    }
                } else {
                    this._lastError = 101;
                }
            } catch(e) {
                this._lastError = 101;
            }

            this._terminated = true;
            this._initialized = false;
            return this._lastError === 0 ? 'true' : 'false';
        },

        /**
         * LMSGetValue() — Obtiene un valor del modelo de datos.
         * SCORM 1.2: llama a this.LMSGetValue(name)
         *
         * @param {string} name - Nombre del elemento (ej: "cmi.core.score.raw")
         * @return {string} Valor del elemento o ""
         */
        LMSGetValue: function(name) {
            if (!this._initialized) {
                this._lastError = 301;
                return '';
            }

            if (!name || typeof name !== 'string') {
                this._lastError = 201;
                return '';
            }

            this._lastError = 0;

            var encodedClave = encodeURIComponent(name);
            var url = this._baseUrl + '/' + this._paqueteId + '/getValue?clave=' + encodedClave;

            // Petición síncrona (simulada con XMLHttpRequest sincrónico)
            var result = '';
            var xhr = new XMLHttpRequest();
            xhr.open('GET', url, false); // Síncrono
            xhr.timeout = this._timeout;

            try {
                xhr.send();
                if (xhr.status >= 200 && xhr.status < 300) {
                    var response = JSON.parse(xhr.responseText);
                    result = response.valor || '';
                    this._lastError = 0;
                } else {
                    this._lastError = 101;
                }
            } catch(e) {
                this._lastError = 101;
            }

            return result;
        },

        /**
         * LMSSetValue() — Establece un valor en el modelo de datos.
         * SCORM 1.2: llama a this.LMSSetValue(name, value)
         *
         * @param {string} name - Nombre del elemento
         * @param {string} value - Valor a establecer
         * @return {string} "true" o "false"
         */
        LMSSetValue: function(name, value) {
            if (!this._initialized) {
                this._lastError = 301;
                return 'false';
            }

            if (!name || typeof name !== 'string') {
                this._lastError = 201;
                return 'false';
            }

            this._lastError = 0;

            var url = this._baseUrl + '/' + this._paqueteId + '/setValue';
            var data = JSON.stringify({ clave: name, valor: value || '' });

            var xhr = new XMLHttpRequest();
            xhr.open('POST', url, false); // Síncrono
            xhr.timeout = this._timeout;
            xhr.setRequestHeader('Content-Type', 'application/json');

            try {
                xhr.send(data);
                if (xhr.status >= 200 && xhr.status < 300) {
                    var response = JSON.parse(xhr.responseText);
                    if (response.result === 'true') {
                        this._lastError = 0;

                        // Notificar progreso al padre si es cmi.core.score.related o cmi.core.score.raw
                        if (name === 'cmi.core.score.raw' || name === 'cmi.score.raw' || name === 'cmi.core.lesson_status') {
                            this._notificarProgreso(name, value);
                        }

                        return 'true';
                    }
                }
                this._lastError = 101;
            } catch(e) {
                this._lastError = 101;
            }

            return 'false';
        },

        /**
         * LMSCommit() — Persiste los valores almacenados.
         * SCORM 1.2: llama a this.LMSCommit()
         *
         * @param {string} param - Parámetro (generalmente "")
         * @return {string} "true" o "false"
         */
        LMSCommit: function(param) {
            if (!this._initialized) {
                this._lastError = 301;
                return 'false';
            }

            this._lastError = 0;

            // Petición síncrona para garantizar que el commit se completa
            var url = this._baseUrl + '/' + this._paqueteId + '/commit';
            var xhr = new XMLHttpRequest();
            xhr.open('POST', url, false); // Síncrono
            xhr.timeout = this._timeout;
            xhr.setRequestHeader('Content-Type', 'application/json');

            try {
                xhr.send(JSON.stringify({}));
                if (xhr.status >= 200 && xhr.status < 300) {
                    var response = JSON.parse(xhr.responseText);
                    this._lastError = response.result === 'true' ? 0 : 101;
                } else {
                    this._lastError = 101;
                }
            } catch(e) {
                this._lastError = 101;
            }

            return this._lastError === 0 ? 'true' : 'false';
        },

        /**
         * LMSGetLastError() — Obtiene el código del último error.
         *
         * @return {number} Código de error
         */
        LMSGetLastError: function() {
            return this._lastError;
        },

        /**
         * LMSGetErrorString() — Obtiene el mensaje de error.
         *
         * @param {number} errorCode - Código de error
         * @return {string} Mensaje de error
         */
        LMSGetErrorString: function(errorCode) {
            var errors = {
                0: 'Sin error',
                101: 'Excepción general',
                201: 'Argumento inválido',
                202: 'El elemento no puede tener hijos',
                203: 'Elemento no es un arreglo',
                301: 'No inicializado',
                401: 'No implementado',
                402: 'Valor inválido, elemento es keyword',
                403: 'Elemento es solo lectura',
                404: 'Elemento es solo escritura',
                405: 'Tipo de dato incorrecto'
            };
            return errors[errorCode] || 'Error desconocido (' + errorCode + ')';
        },

        /**
         * LMSGetDiagnostic() — Obtiene diagnóstico de error.
         *
         * @param {number} errorCode - Código de error
         * @return {string} Diagnóstico
         */
        LMSGetDiagnostic: function(errorCode) {
            var diagnostics = {
                0: 'Sin error',
                101: 'Ocurrió una excepción general durante la operación',
                201: 'Se pasó un argumento inválido a la función',
                202: 'El elemento especificado no puede contener hijos',
                203: 'El elemento no es un arreglo, no puede tener hijos',
                301: 'La sesión SCORM no ha sido inicializada',
                401: 'La funcionalidad solicitada no está implementada',
                402: 'No se puede asignar valor a un elemento keyword',
                403: 'El elemento es de solo lectura',
                404: 'El elemento es de solo escritura',
                405: 'El tipo de dato no coincide con el esperado'
            };
            return diagnostics[errorCode] || 'Código de error no reconocido: ' + errorCode;
        },

        // ============================================================
        // SCORM 2004 API (API_1484_11)
        // ============================================================

        /**
         * Inicializar (SCORM 2004).
         */
        Initialize: function(param) {
            return this.LMSInitialize(param);
        },

        /**
         * Terminar (SCORM 2004).
         */
        Terminate: function(param) {
            return this.LMSFinish(param);
        },

        /**
         * Obtener valor (SCORM 2004).
         */
        GetValue: function(name) {
            return this.LMSGetValue(name);
        },

        /**
         * Establecer valor (SCORM 2004).
         */
        SetValue: function(name, value) {
            return this.LMSSetValue(name, value);
        },

        /**
         * Commit (SCORM 2004).
         */
        Commit: function(param) {
            return this.LMSCommit(param);
        },

        /**
         * Obtener último error (SCORM 2004).
         */
        GetLastError: function() {
            return this.LMSGetLastError();
        },

        /**
         * Obtener mensaje de error (SCORM 2004).
         */
        GetErrorString: function(errorCode) {
            return this.LMSGetErrorString(errorCode);
        },

        /**
         * Obtener diagnóstico (SCORM 2004).
         */
        GetDiagnostic: function(errorCode) {
            return this.LMSGetDiagnostic(errorCode);
        },

        // ============================================================
        // Métodos internos
        // ============================================================

        /**
         * Realiza una petición HTTP asíncrona.
         *
         * @param {string} method - Método HTTP
         * @param {string} url - URL del endpoint
         * @param {object} data - Datos a enviar
         * @return {Promise} Promesa con la respuesta JSON
         */
        _peticion: function(method, url, data) {
            return new Promise(function(resolve, reject) {
                var xhr = new XMLHttpRequest();
                xhr.open(method, url, true);
                xhr.timeout = SCORM_API._timeout;
                xhr.setRequestHeader('Content-Type', 'application/json');

                xhr.onload = function() {
                    if (xhr.status >= 200 && xhr.status < 300) {
                        try {
                            resolve(JSON.parse(xhr.responseText));
                        } catch(e) {
                            reject(e);
                        }
                    } else {
                        reject(new Error('HTTP ' + xhr.status));
                    }
                };

                xhr.onerror = function() {
                    reject(new Error('Error de red'));
                };

                xhr.ontimeout = function() {
                    reject(new Error('Timeout'));
                };

                xhr.send(JSON.stringify(data));
            });
        },

        /**
         * Notifica progreso al contenedor padre (scorm-player.html).
         */
        _notificarProgreso: function(clave, valor) {
            try {
                var progreso = 0;

                if (clave === 'cmi.core.lesson_status' || clave === 'cmi.lesson_status') {
                    if (valor === 'completed' || valor === 'passed') {
                        progreso = 100;
                    } else if (valor === 'incomplete') {
                        progreso = 50;
                    } else if (valor === 'browsed') {
                        progreso = 10;
                    }
                } else if (clave === 'cmi.core.score.raw' || clave === 'cmi.score.raw') {
                    progreso = parseInt(valor) || 0;
                }

                if (window.parent && window.parent !== window) {
                    window.parent.postMessage({
                        type: 'scorm-progress',
                        value: progreso.toString()
                    }, '*');
                }
            } catch(e) {
                // Ignorar errores de postMessage
            }
        }
    };

    // ============================================================
    // Exponer API globalmente
    // ============================================================

    // SCORM 1.2 espera encontrar el objeto API en window.API
    global.API = {
        LMSInitialize: function(param) {
            return SCORM_API.LMSInitialize(param);
        },
        LMSFinish: function(param) {
            return SCORM_API.LMSFinish(param);
        },
        LMSGetValue: function(name) {
            return SCORM_API.LMSGetValue(name);
        },
        LMSSetValue: function(name, value) {
            return SCORM_API.LMSSetValue(name, value);
        },
        LMSCommit: function(param) {
            return SCORM_API.LMSCommit(param);
        },
        LMSGetLastError: function() {
            return SCORM_API.LMSGetLastError();
        },
        LMSGetErrorString: function(errorCode) {
            return SCORM_API.LMSGetErrorString(errorCode);
        },
        LMSGetDiagnostic: function(errorCode) {
            return SCORM_API.LMSGetDiagnostic(errorCode);
        }
    };

    // SCORM 2004 espera encontrar el objeto API_1484_11 en window.API_1484_11
    global.API_1484_11 = {
        Initialize: function(param) {
            return SCORM_API.Initialize(param);
        },
        Terminate: function(param) {
            return SCORM_API.Terminate(param);
        },
        GetValue: function(name) {
            return SCORM_API.GetValue(name);
        },
        SetValue: function(name, value) {
            return SCORM_API.SetValue(name, value);
        },
        Commit: function(param) {
            return SCORM_API.Commit(param);
        },
        GetLastError: function() {
            return SCORM_API.GetLastError();
        },
        GetErrorString: function(errorCode) {
            return SCORM_API.GetErrorString(errorCode);
        },
        GetDiagnostic: function(errorCode) {
            return SCORM_API.GetDiagnostic(errorCode);
        }
    };

    // Exponer también el objeto LMS_SCORM_API para control desde el reproductor
    global.LMS_SCORM_API = SCORM_API;

})(window);

// Soporte para FindSCORM/SCORM 1.2 que busca API en window.parent
(function() {
    'use strict';
    if (window.parent && window.parent !== window) {
        try {
            if (window.parent.API) {
                // Ya existe API en el padre
            }
        } catch(e) {
            // Cross-origin, no se puede acceder
        }
    }
})();
