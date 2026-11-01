/* ============================================================
   app.js - Utilidades Globales LMS LegalTech
   JavaScript vainilla - Sin frameworks
   ============================================================ */

(function() {
    'use strict';

    // ============================================================
    // INICIALIZACIÓN
    // ============================================================
    document.addEventListener('DOMContentLoaded', function() {
        initTheme();
        initDropdowns();
        initModals();
        initToasts();
        initMobileMenu();
        initTooltips();
    });

    // ============================================================
    // TEMA (DARK MODE)
    // ============================================================
    function initTheme() {
        const savedTheme = localStorage.getItem('lms-theme') || 'light';
        document.documentElement.setAttribute('data-theme', savedTheme);
        updateThemeIcon(savedTheme);
    }

    function toggleTheme() {
        const current = document.documentElement.getAttribute('data-theme');
        const next = current === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', next);
        localStorage.setItem('lms-theme', next);
        updateThemeIcon(next);
    }

    function updateThemeIcon(theme) {
        const toggles = document.querySelectorAll('.theme-toggle');
        toggles.forEach(function(btn) {
            if (theme === 'dark') {
                btn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>';
            } else {
                btn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>';
            }
        });
    }

    // Exponer toggleTheme globalmente
    window.toggleTheme = toggleTheme;

    // ============================================================
    // FORMATO DE FECHAS
    // ============================================================
    function formatDate(dateStr, options) {
        if (!dateStr) return '-';
        try {
            var date = new Date(dateStr);
            if (isNaN(date.getTime())) return dateStr;
            var opts = options || {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            };
            return date.toLocaleDateString('es-CL', opts);
        } catch(e) {
            return dateStr;
        }
    }

    function formatRelativeTime(dateStr) {
        if (!dateStr) return '';
        try {
            var date = new Date(dateStr);
            if (isNaN(date.getTime())) return dateStr;
            var now = new Date();
            var diffMs = now - date;
            var diffSec = Math.floor(diffMs / 1000);
            var diffMin = Math.floor(diffSec / 60);
            var diffHour = Math.floor(diffMin / 60);
            var diffDay = Math.floor(diffHour / 24);

            if (diffSec < 60) return 'Ahora';
            if (diffMin < 60) return 'Hace ' + diffMin + ' min';
            if (diffHour < 24) return 'Hace ' + diffHour + ' h';
            if (diffDay < 7) return 'Hace ' + diffDay + ' d';
            return formatDate(dateStr);
        } catch(e) {
            return dateStr;
        }
    }

    window.formatDate = formatDate;
    window.formatRelativeTime = formatRelativeTime;

    // ============================================================
    // TOAST NOTIFICATIONS
    // ============================================================
    function initToasts() {
        // Auto-hide toasts after 5 seconds
        document.querySelectorAll('.toast').forEach(function(toast) {
            setTimeout(function() {
                if (toast.parentNode) {
                    toast.style.opacity = '0';
                    toast.style.transform = 'translateX(100%)';
                    setTimeout(function() {
                        if (toast.parentNode) toast.remove();
                    }, 300);
                }
            }, 5000);
        });
    }

    function showToast(message, type) {
        type = type || 'info';
        var container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.className = 'toast-container';
            document.body.appendChild(container);
        }

        var icons = {
            success: '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
            error: '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>',
            warning: '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>',
            info: '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>'
        };

        var toast = document.createElement('div');
        toast.className = 'toast toast-' + type;
        toast.innerHTML = '<div class="toast-icon">' + (icons[type] || icons.info) + '</div>' +
            '<div class="toast-message">' + message + '</div>' +
            '<button class="toast-close" onclick="this.parentElement.remove()">' +
            '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>' +
            '</button>';

        container.appendChild(toast);

        setTimeout(function() {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(100%)';
            setTimeout(function() {
                if (toast.parentNode) toast.remove();
            }, 300);
        }, 5000);
    }

    window.showToast = showToast;

    // ============================================================
    // DROPDOWNS
    // ============================================================
    function initDropdowns() {
        document.addEventListener('click', function(e) {
            var dropdown = e.target.closest('.dropdown');
            // Close all dropdowns
            document.querySelectorAll('.dropdown-menu.active').forEach(function(menu) {
                if (!dropdown || !dropdown.contains(menu)) {
                    menu.classList.remove('active');
                }
            });
            // Toggle current
            if (dropdown) {
                var menu = dropdown.querySelector('.dropdown-menu');
                if (menu) {
                    menu.classList.toggle('active');
                }
            }
        });
    }

    // ============================================================
    // MODALS
    // ============================================================
    function initModals() {
        document.addEventListener('click', function(e) {
            var modalTrigger = e.target.closest('[data-modal]');
            if (modalTrigger) {
                var modalId = modalTrigger.getAttribute('data-modal');
                var modal = document.getElementById(modalId);
                if (modal) {
                    openModal(modal);
                }
            }

            // Close modal on overlay click
            if (e.target.classList.contains('modal-overlay')) {
                closeModal(e.target);
            }

            // Close modal on close button
            var closeBtn = e.target.closest('.modal-close');
            if (closeBtn) {
                var modal = closeBtn.closest('.modal-overlay');
                if (modal) closeModal(modal);
            }
        });

        // Close on Escape key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                document.querySelectorAll('.modal-overlay.active').forEach(closeModal);
            }
        });
    }

    function openModal(modalOrId) {
        var modal = typeof modalOrId === 'string' ? document.getElementById(modalOrId) : modalOrId;
        if (modal) {
            modal.classList.add('active');
            document.body.style.overflow = 'hidden';
        }
    }

    function closeModal(modalOrId) {
        var modal = typeof modalOrId === 'string' ? document.getElementById(modalOrId) : modalOrId;
        if (modal) {
            modal.classList.remove('active');
            document.body.style.overflow = '';
        }
    }

    window.openModal = openModal;
    window.closeModal = closeModal;

    // ============================================================
    // MOBILE MENU
    // ============================================================
    function initMobileMenu() {
        document.addEventListener('click', function(e) {
            var toggle = e.target.closest('.mobile-menu-toggle');
            if (!toggle) return;

            // Dashboard sidebar
            var sidebar = document.querySelector('.dashboard-sidebar');
            if (sidebar) {
                sidebar.classList.toggle('mobile-open');
                return;
            }

            // Public header mobile menu (homepage)
            var menu = document.getElementById('mobile-menu');
            if (menu) {
                var isVisible = menu.style.display !== 'none';
                menu.style.display = isVisible ? 'none' : 'block';
            }
        });
    }

    // ============================================================
    // TOOLTIPS
    // ============================================================
    function initTooltips() {
        document.querySelectorAll('[data-tooltip]').forEach(function(el) {
            el.addEventListener('mouseenter', function(e) {
                var text = this.getAttribute('data-tooltip');
                var tooltip = document.createElement('div');
                tooltip.className = 'tooltip';
                tooltip.textContent = text;
                tooltip.style.cssText = 'position:fixed;background:#0f172a;color:white;padding:4px 10px;border-radius:6px;font-size:12px;pointer-events:none;z-index:10000;white-space:nowrap;';
                document.body.appendChild(tooltip);

                var rect = this.getBoundingClientRect();
                tooltip.style.left = (rect.left + rect.width / 2 - tooltip.offsetWidth / 2) + 'px';
                tooltip.style.top = (rect.top - tooltip.offsetHeight - 6) + 'px';

                this._tooltip = tooltip;
            });

            el.addEventListener('mouseleave', function() {
                if (this._tooltip) {
                    this._tooltip.remove();
                    this._tooltip = null;
                }
            });
        });
    }

    // ============================================================
    // HTTP HELPERS (para fetch API)
    // ============================================================
    var api = {
        baseUrl: '/api/v1',

        request: function(method, path, data) {
            var options = {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                }
            };

            if (data && method !== 'GET') {
                options.body = JSON.stringify(data);
            }

            return fetch(this.baseUrl + path, options)
                .then(function(response) {
                    if (!response.ok) {
                        return response.json().then(function(err) {
                            throw new Error(err.detail || err.message || 'Error del servidor');
                        });
                    }
                    return response.json();
                });
        },

        get: function(path) {
            return this.request('GET', path);
        },

        post: function(path, data) {
            return this.request('POST', path, data);
        },

        put: function(path, data) {
            return this.request('PUT', path, data);
        },

        del: function(path) {
            return this.request('DELETE', path);
        }
    };

    window.api = api;

    // ============================================================
    // UTILIDADES
    // ============================================================
    function debounce(fn, delay) {
        var timer;
        return function() {
            var context = this;
            var args = arguments;
            clearTimeout(timer);
            timer = setTimeout(function() {
                fn.apply(context, args);
            }, delay || 300);
        };
    }

    function copyToClipboard(text) {
        if (navigator.clipboard) {
            navigator.clipboard.writeText(text).then(function() {
                showToast('Copiado al portapapeles', 'success');
            }).catch(function() {
                fallbackCopy(text);
            });
        } else {
            fallbackCopy(text);
        }
    }

    function fallbackCopy(text) {
        var ta = document.createElement('textarea');
        ta.value = text;
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        ta.remove();
        showToast('Copiado al portapapeles', 'success');
    }

    // Funciones globales para onclick en templates
    window.toggleMobileMenu = function() {
        var menu = document.getElementById('mobile-menu');
        if (menu) {
            menu.style.display = menu.style.display === 'none' ? 'block' : 'none';
        }
    };

    window.toggleSidebar = function() {
        var layout = document.querySelector('.dashboard-layout');
        if (layout) {
            layout.classList.toggle('sidebar-collapsed');
        }
    };

    window.toggleTheme = function() {
        var current = document.documentElement.getAttribute('data-theme');
        var theme = current === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('lms-theme', theme);
        updateThemeIcon(theme);
    };

    window.copyToClipboard = copyToClipboard;
    window.debounce = debounce;
    window.showToast = showToast;

})();
