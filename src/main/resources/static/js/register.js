(function () {
    'use strict';

    function saveFormData(event, url) {
        event.preventDefault();

        const formData = {
            firstName: document.getElementById('firstName').value,
            lastName: document.getElementById('lastName').value,
            username: document.getElementById('username').value,
            email: document.getElementById('email').value,
            password: document.getElementById('password').value,
            confirmPassword: document.getElementById('confirmPassword').value,
            terms: document.getElementById('terms').checked
        };

        sessionStorage.setItem('registerFormData', JSON.stringify(formData));
        window.location.href = url;
    }

    function restoreFormData() {
        const savedData = sessionStorage.getItem('registerFormData');
        if (!savedData) {
            return;
        }
        try {
            const formData = JSON.parse(savedData);

            if (formData.firstName) document.getElementById('firstName').value = formData.firstName;
            if (formData.lastName) document.getElementById('lastName').value = formData.lastName;
            if (formData.username) document.getElementById('username').value = formData.username;
            if (formData.email) document.getElementById('email').value = formData.email;
            if (formData.password) document.getElementById('password').value = formData.password;
            if (formData.confirmPassword) document.getElementById('confirmPassword').value = formData.confirmPassword;
            if (formData.terms) document.getElementById('terms').checked = formData.terms;

            if (formData.password) {
                const event = new Event('input');
                document.getElementById('password').dispatchEvent(event);
            }
        } catch (e) {
            // Keep this silent for users; just log to console for debugging
            console.error('Error restoring form data:', e);
        }
    }

    function checkPasswordStrength(password) {
        let strength = 0;
        let strengthText = '';
        let strengthClass = '';

        if (password.length >= 8) strength++;
        if (password.match(/[a-z]/)) strength++;
        if (password.match(/[A-Z]/)) strength++;
        if (password.match(/[0-9]/)) strength++;
        if (password.match(/[^a-zA-Z0-9]/)) strength++;

        switch (strength) {
            case 0:
            case 1:
                strengthText = 'Very Weak';
                strengthClass = 'very-weak';
                break;
            case 2:
                strengthText = 'Weak';
                strengthClass = 'weak';
                break;
            case 3:
                strengthText = 'Fair';
                strengthClass = 'fair';
                break;
            case 4:
                strengthText = 'Good';
                strengthClass = 'good';
                break;
            case 5:
                strengthText = 'Strong';
                strengthClass = 'strong';
                break;
        }

        return { strength, strengthText, strengthClass };
    }

    function isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    function initPasswordStrength() {
        const passwordInput = document.getElementById('password');
        if (!passwordInput) {
            return;
        }
        passwordInput.addEventListener('input', function () {
            const password = this.value;
            const strength = checkPasswordStrength(password);
            const strengthFill = document.getElementById('strength-fill');
            const strengthText = document.getElementById('strength-text');

            if (!strengthFill || !strengthText) {
                return;
            }

            strengthFill.style.width = (strength.strength * 20) + '%';
            strengthFill.className = 'strength-fill ' + strength.strengthClass;
            strengthText.textContent = strength.strengthText;
        });
    }

    function initFormValidation() {
        const form = document.querySelector('.register-form');
        if (!form) {
            return;
        }

        form.addEventListener('submit', function (e) {
            const firstName = document.getElementById('firstName').value;
            const lastName = document.getElementById('lastName').value;
            const username = document.getElementById('username').value;
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;
            const terms = document.getElementById('terms').checked;

            if (!firstName || !lastName || !username || !email || !password || !confirmPassword) {
                e.preventDefault();
                window.showToast && window.showToast('Please fill in all required fields');
                return;
            }

            if (username.length < 3) {
                e.preventDefault();
                window.showToast && window.showToast('Username must be at least 3 characters long');
                return;
            }

            if (!isValidEmail(email)) {
                e.preventDefault();
                window.showToast && window.showToast('Please enter a valid email address');
                return;
            }

            if (password !== confirmPassword) {
                e.preventDefault();
                window.showToast && window.showToast('Passwords do not match');
                return;
            }

            if (password.length < 8) {
                e.preventDefault();
                window.showToast && window.showToast('Password must be at least 8 characters long');
                return;
            }

            if (!terms) {
                e.preventDefault();
                window.showToast && window.showToast('Please accept the Terms of Service and Privacy Policy');
                return;
            }

            sessionStorage.removeItem('registerFormData');
        });
    }

    function initCardAnimation() {
        const registerCard = document.querySelector('.register-card');
        if (!registerCard) {
            return;
        }

        registerCard.style.opacity = '0';
        registerCard.style.transform = 'translateY(30px)';

        setTimeout(() => {
            registerCard.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
            registerCard.style.opacity = '1';
            registerCard.style.transform = 'translateY(0)';
        }, 100);
    }

    function initSocialButtons() {
        const googleBtn = document.querySelector('.btn-google');
        if (googleBtn) {
            googleBtn.addEventListener('click', function () {
                console.log('Google registration clicked');
            });
        }

        const appleBtn = document.querySelector('.btn-apple');
        if (appleBtn) {
            appleBtn.addEventListener('click', function () {
                console.log('Apple registration clicked');
            });
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        restoreFormData();
        initPasswordStrength();
        initFormValidation();
        initCardAnimation();
        initSocialButtons();
    });

    // expose for inline links in the template
    window.saveFormData = saveFormData;
})();


