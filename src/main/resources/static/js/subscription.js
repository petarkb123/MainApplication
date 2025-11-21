(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        const plans = document.querySelectorAll('.plan');
        plans.forEach((plan, index) => {
            plan.style.opacity = '0';
            plan.style.transform = 'translateY(20px)';
            setTimeout(() => {
                plan.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
                plan.style.opacity = '1';
                plan.style.transform = 'translateY(0)';
            }, 100 + (index * 100));
        });
    });
})();

