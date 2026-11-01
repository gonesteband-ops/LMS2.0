/**
 * ============================================================
 * CONFIGURACIÓN DE QUILL.js — LMS LegalTech
 * ============================================================
 * Editor WYSIWYG liviano y modular.
 * Toolbar personalizado: negrita, itálica, listas, enlace,
 * imagen, video, bloque de código.
 * ============================================================
 * Dependencias:
 *   - /js/quill/quill.min.js
 *   - /js/quill/quill.snow.css
 *   - /css/quill-custom.css
 * ============================================================
 */

(function () {
    'use strict';

    /**
     * Inicializa el editor Quill en el contenedor indicado.
     *
     * @param {string|Element} container  — Selector CSS o elemento DOM del contenedor
     * @param {Object}         opciones   — Opciones adicionales (readOnly, placeholder, initialValue)
     * @returns {Quill|null}  Instancia de Quill o null si falla
     */
    window.LMS_Quill = function (container, opciones) {
        if (typeof Quill === 'undefined') {
            console.error('[LMS_Quill] Quill.js no está cargado.');
            return null;
        }

        // Resolver contenedor
        var el = (typeof container === 'string')
            ? document.querySelector(container)
            : container;

        if (!el) {
            console.error('[LMS_Quill] Contenedor no encontrado:', container);
            return null;
        }

        // Fusionar opciones
        var config = Object.assign({
            placeholder: 'Escribe aquí el contenido…',
            theme: 'snow',
            readOnly: false,
            modules: {
                toolbar: {
                    container: [
                        [{ 'header': [1, 2, 3, 4, 5, 6, false] }],
                        ['bold', 'italic', 'underline', 'strike'],
                        [{ 'list': 'ordered' }, { 'list': 'bullet' }, { 'indent': '-1' }, { 'indent': '+1' }],
                        [{ 'align': [] }],
                        ['link', 'image', 'video'],
                        ['blockquote', 'code-block'],
                        ['clean']
                    ],
                    handlers: {
                        image: imageHandler,
                        video: videoHandler
                    }
                },
                keyboard: {
                    bindings: {
                        tab: false  // Permitir tabulación nativa
                    }
                }
            }
        }, opciones);

        // Crear instancia
        var quill = new Quill(el, config);

        // Si hay valor inicial, establecerlo
        if (config.initialValue) {
            quill.root.innerHTML = config.initialValue;
        }

        // Si es solo lectura, deshabilitar toolbar
        if (config.readOnly) {
            quill.enable(false);
        }

        // === Tooltips en español para los botones del toolbar ===
        var tooltips = {
            'bold': 'Negrita',
            'italic': 'Itálica',
            'underline': 'Subrayado',
            'strike': 'Tachado',
            'header': 'Encabezado',
            'list': 'Lista',
            'ordered': 'Lista numerada',
            'bullet': 'Lista con viñetas',
            'indent': 'Sangría',
            'link': 'Insertar enlace',
            'image': 'Insertar imagen',
            'video': 'Insertar video',
            'blockquote': 'Cita',
            'code-block': 'Bloque de código',
            'clean': 'Limpiar formato',
            'align': 'Alinear'
        };

        // Aplicar tooltips a los botones del toolbar
        var toolbarEl = quill.getModule('toolbar').container;
        if (toolbarEl) {
            var botones = toolbarEl.querySelectorAll('button, select');
            botones.forEach(function (btn) {
                var cls = btn.className || '';
                var val = btn.value || '';
                var key = cls.replace('ql-', '') || val;

                if (tooltips[key]) {
                    btn.setAttribute('title', tooltips[key]);
                } else if (key === '1' || key === '2' || key === '3') {
                    btn.setAttribute('title', 'Encabezado ' + key);
                }
            });
        }

        return quill;
    };

    // ============================================================
    // HANDLER PERSONALIZADO PARA IMÁGENES
    // ============================================================
    // Convierte imágenes a base64 para almacenamiento inline.
    // En producción, reemplazar con subida a servidor.
    // ============================================================
    function imageHandler() {
        var input = document.createElement('input');
        input.setAttribute('type', 'file');
        input.setAttribute('accept', 'image/*');
        input.style.display = 'none';

        input.addEventListener('change', function () {
            var file = input.files[0];
            if (!file) return;

            var reader = new FileReader();
            reader.onload = function (e) {
                var range = this.quill.getSelection(true);
                this.quill.insertEmbed(range.index, 'image', e.target.result, 'user');
                this.quill.setSelection(range.index + 1);
            }.bind(this);

            reader.readAsDataURL(file);
        }.bind(this));

        document.body.appendChild(input);
        input.click();
        document.body.removeChild(input);
    }

    // ============================================================
    // HANDLER PERSONALIZADO PARA VIDEOS
    // ============================================================
    // Inserta un video a partir de una URL (YouTube/Vimeo/MP4).
    // ============================================================
    function videoHandler() {
        var url = prompt('Ingresa la URL del video (YouTube, Vimeo o MP4):');
        if (!url) return;

        var range = this.quill.getSelection(true);

        // Detectar URLs de YouTube para embed
        var youtubeMatch = url.match(
            /(?:youtube\.com\/(?:watch\?v=|embed\/)|youtu\.be\/)([a-zA-Z0-9_-]+)/
        );
        if (youtubeMatch) {
            url = 'https://www.youtube.com/embed/' + youtubeMatch[1];
        }

        // Detectar URLs de Vimeo para embed
        var vimeoMatch = url.match(/vimeo\.com\/(\d+)/);
        if (vimeoMatch) {
            url = 'https://player.vimeo.com/video/' + vimeoMatch[1];
        }

        this.quill.insertEmbed(range.index, 'video', url, 'user');
        this.quill.setSelection(range.index + 1);
    }

})();
