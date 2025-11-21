(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        const rows = document.querySelectorAll('tbody tr');
        rows.forEach((row, index) => {
            row.style.opacity = '0';
            row.style.transform = 'translateY(20px)';
            setTimeout(() => {
                row.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
                row.style.opacity = '1';
                row.style.transform = 'translateY(0)';
            }, 100 + (index * 50));
        });
    });
})();

