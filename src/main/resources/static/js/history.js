(function () {
    'use strict';

    const data = window.__pageDataHistory || {};
    const exercises = data.exercises || [];
    const workoutSessionId = data.sessionId || null;

    window.openStartWorkoutModal = function() {
        const modal = document.getElementById('startWorkoutModal');
        if (modal) {
            modal.style.display = 'flex';
            setTimeout(() => modal.classList.add('show'), 10);
        }
    };

    window.closeStartWorkoutModal = function() {
        const modal = document.getElementById('startWorkoutModal');
        if (modal) {
            modal.classList.remove('show');
            setTimeout(() => modal.style.display = 'none', 300);
        }
    };

    window.startWorkout = function() {
        const templateId = document.getElementById('templateSelect')?.value;
        let url = '/workouts/session';
        if (templateId) {
            url += '?templateId=' + templateId;
        }
        window.location.href = url;
    };

    document.addEventListener('DOMContentLoaded', function() {
        const modal = document.getElementById('startWorkoutModal');
        if (modal) {
            modal.addEventListener('click', function(e) {
                if (e.target === this) {
                    closeStartWorkoutModal();
                }
            });
        }

        const cards = document.querySelectorAll('.session-card');
        cards.forEach((card, index) => {
            card.style.opacity = '0';
            card.style.transform = 'translateY(20px)';
            setTimeout(() => {
                card.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
                card.style.opacity = '1';
                card.style.transform = 'translateY(0)';
            }, 100 + (index * 100));
        });
    });
})();

