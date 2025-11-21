(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        const toggleSection = (sectionId, editing) => {
            const container = document.querySelector(`[data-section="${sectionId}"]`);
            if (!container) {
                return;
            }

            const view = container.querySelector(`[data-view="${sectionId}"]`);
            if (view) {
                view.classList.toggle('is-hidden', editing);
            }

            const form = container.querySelector(`[data-form="${sectionId}"]`);
            if (form) {
                form.classList.toggle('is-hidden', !editing);
                if (!editing) {
                    form.reset();
                } else {
                    const firstInput = form.querySelector('input:not([type="hidden"])');
                    if (firstInput) {
                        requestAnimationFrame(() => firstInput.focus({ preventScroll: true }));
                    }
                }
            }

            const removeBlock = container.querySelector(`[data-remove="${sectionId}"]`);
            if (removeBlock) {
                removeBlock.classList.toggle('is-hidden', !editing);
            }
        };

        document.querySelectorAll('[data-action="edit"]').forEach(button => {
            button.addEventListener('click', () => {
                const target = button.getAttribute('data-target');
                if (target) toggleSection(target, true);
            });
        });

        document.querySelectorAll('[data-action="cancel"]').forEach(button => {
            button.addEventListener('click', () => {
                const target = button.getAttribute('data-target');
                if (target) toggleSection(target, false);
            });
        });
    });
})();

