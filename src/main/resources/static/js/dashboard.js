(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        const cards = document.querySelectorAll('.action-card, .stat-card, .activity-item');
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

