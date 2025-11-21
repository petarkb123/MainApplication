(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        const muscleGroups = ['chest', 'back', 'legs', 'shoulders', 'biceps', 'triceps', 'forearms', 'hamstrings', 'calves', 'core'];
        muscleGroups.forEach(group => {
            const content = document.getElementById(group + '-content');
            const icon = document.getElementById(group + '-icon');
            if (content && icon) {
                content.style.display = 'none';
                icon.style.transform = 'rotate(0deg)';
            }
        });

        const exerciseCards = document.querySelectorAll('.exercise-card');
        const visibleCards = Array.from(exerciseCards).filter(card => {
            return card.closest('.muscle-group-content') === null;
        });

        visibleCards.forEach((card, index) => {
            card.style.opacity = '0';
            card.style.transform = 'translateY(20px)';
            setTimeout(() => {
                card.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
                card.style.opacity = '1';
                card.style.transform = 'translateY(0)';
            }, 100 + (index * 100));
        });

        const muscleGroupSections = document.querySelectorAll('.muscle-group-section');
        muscleGroupSections.forEach((section, index) => {
            section.style.opacity = '0';
            section.style.transform = 'translateY(20px)';
            setTimeout(() => {
                section.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
                section.style.opacity = '1';
                section.style.transform = 'translateY(0)';
            }, 200 + (index * 80));
        });
    });

    window.toggleMuscleGroup = function(groupName) {
        const content = document.getElementById(groupName + '-content');
        const icon = document.getElementById(groupName + '-icon');

        if (content && icon) {
            const isCollapsed = content.style.display === 'none' || 
                               (!content.style.display && window.getComputedStyle(content).display === 'none');
            
            if (isCollapsed) {
                content.style.display = 'block';
                content.offsetHeight;
                icon.style.transition = 'transform 0.3s ease';
                icon.style.transform = 'rotate(180deg)';
                const cards = content.querySelectorAll('.exercise-card');
                cards.forEach((card, index) => {
                    card.style.opacity = '0';
                    card.style.transform = 'translateY(15px)';
                    card.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
                    setTimeout(() => {
                        card.style.opacity = '1';
                        card.style.transform = 'translateY(0)';
                    }, 50 + (index * 40));
                });
            } else {
                icon.style.transition = 'transform 0.3s ease';
                icon.style.transform = 'rotate(0deg)';
                content.style.display = 'none';
            }
        }
    };

    window.openAddModal = function() {
        const modal = document.getElementById('addModal');
        if (modal) {
            modal.style.display = 'flex';
            modal.offsetHeight;
            requestAnimationFrame(() => {
                modal.classList.add('show');
            });
        }
    };

    window.closeAddModal = function() {
        const modal = document.getElementById('addModal');
        if (modal) {
            modal.classList.remove('show');
            setTimeout(() => {
                modal.style.display = 'none';
            }, 300);
        }
    };

    window.onclick = function(event) {
        const modal = document.getElementById('addModal');
        if (event.target === modal) {
            closeAddModal();
        }
    };
})();

