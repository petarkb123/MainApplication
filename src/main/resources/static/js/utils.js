/**
 * Shared utility functions for the Fitness Application
 */

(function() {
    'use strict';

    /**
     * Shows a toast notification message
     * @param {string} message - The message to display
     * @param {string} type - 'success' or 'error' (default: 'error')
     */
    function showToast(message, type = 'error') {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.style.cssText = `
                position: fixed;
                top: 20px;
                left: 50%;
                transform: translateX(-50%);
                z-index: 9999;
                display: flex;
                flex-direction: column;
                gap: 10px;
            `;
            document.body.appendChild(container);
        }

        const toast = document.createElement('div');
        const isSuccess = type === 'success';
        toast.style.cssText = `
            padding: 12px 16px;
            border-radius: 12px;
            backdrop-filter: blur(10px);
            border: 1px solid ${isSuccess ? 'rgba(76,175,80,0.35)' : 'rgba(244,67,54,0.35)'};
            box-shadow: 0 8px 25px rgba(0,0,0,0.25);
            min-width: 280px;
            text-align: left;
            display: flex;
            align-items: center;
            gap: 10px;
            color: #fff;
            background: ${isSuccess ? 'rgba(76,175,80,0.18)' : 'rgba(244,67,54,0.18)'};
        `;
        toast.innerHTML = `<i class="fas ${isSuccess ? 'fa-check-circle' : 'fa-exclamation-triangle'}"></i><span>${message}</span>`;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transition = 'opacity .3s';
        }, 3200);
        setTimeout(() => toast.remove(), 3600);
    }

    /**
     * Toggles password visibility
     * @param {string} inputId - The ID of the password input element
     */
    function togglePassword(inputId = 'password') {
        const passwordInput = document.getElementById(inputId);
        if (!passwordInput) return;

        const passwordEye = document.getElementById(`${inputId}-eye`);
        if (!passwordEye) return;

        if (passwordInput.type === 'password') {
            passwordInput.type = 'text';
            passwordEye.classList.remove('fa-eye');
            passwordEye.classList.add('fa-eye-slash');
        } else {
            passwordInput.type = 'password';
            passwordEye.classList.remove('fa-eye-slash');
            passwordEye.classList.add('fa-eye');
        }
    }

    // Export to global scope
    window.showToast = showToast;
    window.togglePassword = togglePassword;
})();

