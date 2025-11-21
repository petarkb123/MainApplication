(function () {
    'use strict';

    const data = window.__pageDataEdit || {};
    const exercisesRaw = Array.isArray(data.exercises) ? data.exercises : [];
    const existingItemsData = Array.isArray(data.items) ? data.items : [];
    const SYSTEM_USER_ID = data.systemUserId || '';
    const isPro = Boolean(data.isPro);

    const MG_ORDER = ['CHEST', 'BACK', 'LEGS', 'SHOULDERS', 'BICEPS', 'TRICEPS', 'FOREARMS', 'HAMSTRINGS', 'CALVES', 'CORE', 'OTHER'];

    const exercises = exercisesRaw.map(ex => ({
        ...ex,
        muscleGroup: ex.muscleGroup ?? ex.primaryMuscle ?? 'OTHER',
        builtIn: String(ex.ownerUserId ?? '') === String(SYSTEM_USER_ID)
    }));

    let exerciseCounter = 0;
    let currentDropSetExerciseIndex = null;

    function showToast(message, type = 'error') {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.style.position = 'fixed';
            container.style.top = '20px';
            container.style.left = '50%';
            container.style.transform = 'translateX(-50%)';
            container.style.zIndex = '9999';
            container.style.display = 'flex';
            container.style.flexDirection = 'column';
            container.style.gap = '10px';
            document.body.appendChild(container);
        }

        const toast = document.createElement('div');
        toast.style.padding = '12px 16px';
        toast.style.borderRadius = '12px';
        toast.style.backdropFilter = 'blur(10px)';
        toast.style.border = '1px solid rgba(255,255,255,0.15)';
        toast.style.boxShadow = '0 8px 25px rgba(0,0,0,0.25)';
        toast.style.minWidth = '280px';
        toast.style.textAlign = 'left';
        toast.style.display = 'flex';
        toast.style.alignItems = 'center';
        toast.style.gap = '10px';
        toast.style.color = '#fff';
        toast.style.background = type === 'success' ? 'rgba(76,175,80,0.18)' : 'rgba(244,67,54,0.18)';
        toast.style.borderColor = type === 'success' ? 'rgba(76,175,80,0.35)' : 'rgba(244,67,54,0.35)';
        toast.innerHTML = `<i class="fas ${type === 'success' ? 'fa-check-circle' : 'fa-exclamation-triangle'}"></i><span>${message}</span>`;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transition = 'opacity .3s';
        }, 3200);
        setTimeout(() => toast.remove(), 3600);
    }
    window.showToast = showToast;

    function groupByMuscle(list) {
        return list.reduce((acc, ex) => {
            const key = String(ex.muscleGroup || 'OTHER');
            (acc[key] ||= []).push(ex);
            return acc;
        }, {});
    }

    function optgroupsFor(prefixLabel, list) {
        const grouped = groupByMuscle(list);
        let html = '';

        MG_ORDER.forEach(mg => {
            const arr = (grouped[mg] || []).slice().sort((a, b) => a.name.localeCompare(b.name));
            if (arr.length) {
                html += `<optgroup label="${prefixLabel} — ${mg}">`;
                html += arr.map(ex => `<option value="${ex.id}">${ex.name}</option>`).join('');
                html += '</optgroup>';
            }
        });

        Object.keys(grouped)
            .filter(k => !MG_ORDER.includes(k))
            .sort()
            .forEach(mg => {
                const arr = grouped[mg].slice().sort((a, b) => a.name.localeCompare(b.name));
                html += `<optgroup label="${prefixLabel} — ${mg}">`;
                html += arr.map(ex => `<option value="${ex.id}">${ex.name}</option>`).join('');
                html += '</optgroup>';
            });

        return html;
    }

    function optionHtml() {
        if (!exercises.length) {
            return '<option value="">No exercises</option>';
        }
        const mine = exercises.filter(ex => !ex.builtIn);
        const builtIns = exercises.filter(ex => ex.builtIn);

        let html = '<option value="">Select Exercise</option>';
        html += optgroupsFor('My exercises', mine);
        if (mine.length && builtIns.length) {
            html += '<option value="" disabled>&nbsp;</option>';
        }
        html += optgroupsFor('Built-in', builtIns);
        return html;
    }

    function populateExerciseSelect() {
        const select = document.getElementById('exerciseSelect');
        if (select) {
            select.innerHTML = optionHtml();
        }
    }

    function openAddExerciseModal() {
        populateExerciseSelect();
        const modal = document.getElementById('addExerciseModal');
        if (!modal) return;
        modal.style.display = 'flex';
        setTimeout(() => modal.classList.add('show'), 10);
    }

    function closeAddExerciseModal() {
        const modal = document.getElementById('addExerciseModal');
        if (!modal) return;
        modal.classList.remove('show');
        setTimeout(() => modal.style.display = 'none', 300);
    }

    function addExerciseToTemplate() {
        const select = document.getElementById('exerciseSelect');
        const exerciseId = select?.value;
        const targetSetsInput = document.getElementById('targetSetsInput');
        const targetSets = parseInt(targetSetsInput?.value ?? '3', 10) || 3;

        if (!exerciseId) {
            showToast('Please select an exercise');
            return;
        }

        const exercise = exercises.find(ex => String(ex.id) === String(exerciseId));
        if (!exercise) {
            showToast('Exercise not found');
            return;
        }

        addExerciseCard(exercise, targetSets);
        if (select) select.value = '';
        if (targetSetsInput) targetSetsInput.value = '3';
        closeAddExerciseModal();
    }

    function addExerciseCard(exercise, targetSets = 3) {
        const exercisesList = document.getElementById('exercisesList');
        const emptyState = document.getElementById('empty-exercises');
        if (!exercisesList) return;
        if (emptyState) emptyState.style.display = 'none';

        const exerciseCard = document.createElement('div');
        exerciseCard.className = 'exercise-card';
        exerciseCard.id = `exercise-${exerciseCounter}`;
        exerciseCard.setAttribute('data-exercise-id', exercise.id);
        exerciseCard.setAttribute('data-target-sets', String(targetSets));

        exerciseCard.innerHTML = `
            <div class="exercise-header">
                <div class="exercise-info">
                    <h3 class="exercise-name">${exercise.name}</h3>
                    <span class="exercise-muscle">${exercise.muscleGroup}</span>
                </div>
                <button type="button" class="btn-icon btn-remove" onclick="removeExercise(${exerciseCounter})">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
            <div class="sets-container">
                <div class="sets-header">
                    <span>Target Sets: ${targetSets}</span>
                    <div class="sets-actions">
                        <button type="button" class="btn btn-sm btn-outline" onclick="changeTargetSets(${exerciseCounter}, -1)">
                            <i class="fas fa-minus"></i>
                        </button>
                        <button type="button" class="btn btn-sm btn-outline" onclick="changeTargetSets(${exerciseCounter}, 1)">
                            <i class="fas fa-plus"></i>
                        </button>
                        ${isPro ? `<button type="button" class="btn btn-sm btn-outline" onclick="openDropSetModal(${exerciseCounter})" title="Add drop set to specific set"><i class="fas fa-layer-group"></i> Drop Set</button>` : ''}
                    </div>
                </div>
                <div class="sets-list" id="sets-${exerciseCounter}">
                    <div class="set-display">${targetSets} sets planned</div>
                </div>
            </div>
        `;

        exercisesList.appendChild(exerciseCard);
        exerciseCounter += 1;
    }

    function changeTargetSets(exerciseIndex, delta) {
        const card = document.getElementById(`exercise-${exerciseIndex}`);
        if (!card) return;
        let currentSets = parseInt(card.getAttribute('data-target-sets') || '1', 10);
        currentSets = Math.max(1, Math.min(20, currentSets + delta));
        card.setAttribute('data-target-sets', String(currentSets));

        const header = card.querySelector('.sets-header span');
        if (header) header.textContent = `Target Sets: ${currentSets}`;

        const display = card.querySelector('.set-display');
        if (display) display.textContent = `${currentSets} sets planned`;
    }

    function openDropSetModal(exerciseIndex) {
        currentDropSetExerciseIndex = exerciseIndex;
        const card = document.getElementById(`exercise-${exerciseIndex}`);
        if (!card) return;
        const targetSets = parseInt(card.getAttribute('data-target-sets') || '1', 10);

        const select = document.getElementById('setNumberSelect');
        if (select) {
            select.innerHTML = '<option value="">Select a set...</option>';
            for (let i = 1; i <= targetSets; i += 1) {
                select.innerHTML += `<option value="${i}">Set ${i}</option>`;
            }
        }

        const dropCountInput = document.getElementById('dropSetsCountInput');
        if (dropCountInput) dropCountInput.value = '1';

        const modal = document.getElementById('dropSetModal');
        if (!modal) return;
        modal.style.display = 'flex';
        setTimeout(() => modal.classList.add('show'), 10);
    }

    function closeDropSetModal() {
        const modal = document.getElementById('dropSetModal');
        if (!modal) return;
        modal.classList.remove('show');
        setTimeout(() => (modal.style.display = 'none'), 300);
        currentDropSetExerciseIndex = null;
    }

    function addDropSetToExercise() {
        if (currentDropSetExerciseIndex === null) return;

        const setNumber = parseInt(document.getElementById('setNumberSelect')?.value ?? '', 10);
        const dropSetsCount = parseInt(document.getElementById('dropSetsCountInput')?.value ?? '', 10);

        if (!setNumber || dropSetsCount < 1) {
            showToast('Please select a set number and number of drop sets');
            return;
        }

        const card = document.getElementById(`exercise-${currentDropSetExerciseIndex}`);
        if (!card) return;
        const setsList = card.querySelector('.sets-list');
        if (!setsList) return;

        const existingIndicators = setsList.querySelectorAll(`[data-set-number="${setNumber}"]`);
        existingIndicators.forEach(indicator => indicator.remove());

        for (let i = 1; i <= dropSetsCount; i += 1) {
            const dropIndicator = document.createElement('div');
            dropIndicator.className = 'drop-set-indicator';
            dropIndicator.setAttribute('data-set-number', String(setNumber));
            dropIndicator.innerHTML = `
                <span class="drop-badge">Set ${setNumber} - Drop ${i}</span>
                <button type="button" class="btn-icon btn-remove" onclick="removeDropSet(this)">
                    <i class="fas fa-times"></i>
                </button>
            `;
            setsList.appendChild(dropIndicator);
        }

        closeDropSetModal();
    }

    function removeDropSet(btn) {
        const indicator = btn.closest('.drop-set-indicator');
        if (indicator) indicator.remove();
    }

    function removeExercise(exIdx) {
        const card = document.getElementById(`exercise-${exIdx}`);
        if (card) {
            card.remove();
        }
        const list = document.getElementById('exercisesList');
        const empty = document.getElementById('empty-exercises');
        if (list && list.children.length === 0 && empty) {
            empty.style.display = 'flex';
        }
    }

    function generateUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
            const r = Math.random() * 16 | 0;
            const v = c === 'x' ? r : (r & 0x3) | 0x8;
            return v.toString(16);
        });
    }

    function createExerciseCardElement(exercise, targetSets) {
        const exerciseCard = document.createElement('div');
        exerciseCard.className = 'exercise-card';
        exerciseCard.id = `exercise-${exerciseCounter}`;
        exerciseCard.setAttribute('data-exercise-id', exercise.id);
        exerciseCard.setAttribute('data-target-sets', String(targetSets));

        exerciseCard.innerHTML = `
            <div class="exercise-header">
                <div class="exercise-info">
                    <h3 class="exercise-name">${exercise.name}</h3>
                    <span class="exercise-muscle">${exercise.muscleGroup}</span>
                </div>
                <button type="button" class="btn-icon btn-remove" onclick="removeExercise(${exerciseCounter})">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
            <div class="sets-container">
                <div class="sets-header">
                    <span>Target Sets: ${targetSets}</span>
                    <div class="sets-actions">
                        <button type="button" class="btn btn-sm btn-outline" onclick="changeTargetSets(${exerciseCounter}, -1)">
                            <i class="fas fa-minus"></i>
                        </button>
                        <button type="button" class="btn btn-sm btn-outline" onclick="changeTargetSets(${exerciseCounter}, 1)">
                            <i class="fas fa-plus"></i>
                        </button>
                        ${isPro ? `<button type="button" class="btn btn-sm btn-outline" onclick="openDropSetModal(${exerciseCounter})" title="Add drop set to specific set"><i class="fas fa-layer-group"></i> Drop Set</button>` : ''}
                    </div>
                </div>
                <div class="sets-list" id="sets-${exerciseCounter}">
                    <div class="set-display">${targetSets} sets planned</div>
                </div>
            </div>
        `;

        exerciseCounter += 1;
        return exerciseCard;
    }

    function loadExistingItems() {
        if (!existingItemsData || existingItemsData.length === 0) return;

        const sortedItems = [...existingItemsData].sort((a, b) => (a.position || 0) - (b.position || 0));

        const groups = {};
        sortedItems.forEach(item => {
            if (item.groupId) {
                const groupKey = `${item.groupId}_${item.groupType}`;
                if (!groups[groupKey]) {
                    groups[groupKey] = [];
                }
                groups[groupKey].push(item);
            }
        });

        const processed = new Set();

        sortedItems.forEach(item => {
            if (processed.has(item.id)) return;

            if (item.groupType === 'DROP_SET' && item.groupId) {
                const groupKey = `${item.groupId}_DROP_SET`;
                const group = groups[groupKey] || [];

                if (group.length > 0 && (item.groupOrder === 0 || item.groupOrder === '0')) {
                    group.sort((a, b) => (a.groupOrder || 0) - (b.groupOrder || 0));
                    const mainItem = group[0];
                    const exercise = exercises.find(ex => String(ex.id) === String(mainItem.exerciseId));

                    if (exercise) {
                        const totalSets = mainItem.targetSets || 1;
                        const card = createExerciseCardElement(exercise, totalSets);
                        const exercisesList = document.getElementById('exercisesList');
                        if (exercisesList) exercisesList.appendChild(card);

                        const currentIndex = exerciseCounter - 1;
                        const dropSets = group.filter(item => item.groupOrder > 0);

                        const dropSetsBySetNumber = new Map();
                        dropSets.forEach(dropSet => {
                            const setNumber = dropSet.setNumber || 1;
                            if (!dropSetsBySetNumber.has(setNumber)) {
                                dropSetsBySetNumber.set(setNumber, []);
                            }
                            dropSetsBySetNumber.get(setNumber).push(dropSet);
                        });

                        dropSetsBySetNumber.forEach((sets, setNumber) => {
                            const cardEl = document.getElementById(`exercise-${currentIndex}`);
                            const setsList = cardEl?.querySelector('.sets-list');
                            if (!setsList) return;

                            for (let i = 0; i < sets.length; i += 1) {
                                const dropIndicator = document.createElement('div');
                                dropIndicator.className = 'drop-set-indicator';
                                dropIndicator.setAttribute('data-set-number', String(setNumber));
                                dropIndicator.innerHTML = `
                                    <span class="drop-badge">Set ${setNumber} - Drop ${i + 1}</span>
                                    <button type="button" class="btn-icon btn-remove" onclick="removeDropSet(this)">
                                        <i class="fas fa-times"></i>
                                    </button>
                                `;
                                setsList.appendChild(dropIndicator);
                            }
                        });

                        group.forEach(g => processed.add(g.id));
                    }
                }
            } else if (!item.groupType || !item.groupId) {
                const exercise = exercises.find(ex => String(ex.id) === String(item.exerciseId));
                if (exercise) {
                    addExerciseCard(exercise, item.targetSets || 3);
                    processed.add(item.id);
                }
            }
        });

        const exercisesList = document.getElementById('exercisesList');
        if (exercisesList && exercisesList.children.length > 0) {
            const emptyState = document.getElementById('empty-exercises');
            if (emptyState) emptyState.style.display = 'none';
        }
    }

    function addHiddenInput(form, name, value) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value ?? '';
        form.appendChild(input);
    }

    function handleTemplateFormSubmit(event) {
        event.preventDefault();
        const form = event.currentTarget;

        document.querySelectorAll('input[name^="items["]').forEach(el => el.remove());

        const cards = document.querySelectorAll('.exercise-card');
        let itemIndex = 0;

        cards.forEach(card => {
            const exerciseId = card.getAttribute('data-exercise-id');
            if (!exerciseId) {
                console.error('Exercise card missing exerciseId', card);
                return;
            }

            const targetSets = parseInt(card.getAttribute('data-target-sets') || '1', 10);
            const dropSetIndicators = card.querySelectorAll('.drop-set-indicator');

            if (dropSetIndicators.length > 0) {
                const dropSetsBySetNumber = new Map();
                dropSetIndicators.forEach(indicator => {
                    const setNumber = parseInt(indicator.getAttribute('data-set-number') || '0', 10);
                    if (!dropSetsBySetNumber.has(setNumber)) {
                        dropSetsBySetNumber.set(setNumber, []);
                    }
                    dropSetsBySetNumber.get(setNumber).push(indicator);
                });

                dropSetsBySetNumber.forEach((indicators, setNumber) => {
                    const groupId = generateUUID();

                    addHiddenInput(form, `items[${itemIndex}].exerciseId`, exerciseId);
                    addHiddenInput(form, `items[${itemIndex}].sets`, targetSets);
                    addHiddenInput(form, `items[${itemIndex}].orderIndex`, itemIndex);
                    addHiddenInput(form, `items[${itemIndex}].groupId`, groupId);
                    addHiddenInput(form, `items[${itemIndex}].groupType`, 'DROP_SET');
                    addHiddenInput(form, `items[${itemIndex}].groupOrder`, '0');
                    addHiddenInput(form, `items[${itemIndex}].setNumber`, setNumber.toString());
                    itemIndex += 1;

                    for (let i = 1; i <= indicators.length; i += 1) {
                        addHiddenInput(form, `items[${itemIndex}].exerciseId`, exerciseId);
                        addHiddenInput(form, `items[${itemIndex}].sets`, targetSets);
                        addHiddenInput(form, `items[${itemIndex}].orderIndex`, itemIndex);
                        addHiddenInput(form, `items[${itemIndex}].groupId`, groupId);
                        addHiddenInput(form, `items[${itemIndex}].groupType`, 'DROP_SET');
                        addHiddenInput(form, `items[${itemIndex}].groupOrder`, i.toString());
                        addHiddenInput(form, `items[${itemIndex}].setNumber`, setNumber.toString());
                        itemIndex += 1;
                    }
                });
            } else {
                addHiddenInput(form, `items[${itemIndex}].exerciseId`, exerciseId);
                addHiddenInput(form, `items[${itemIndex}].sets`, targetSets);
                addHiddenInput(form, `items[${itemIndex}].orderIndex`, itemIndex);
                itemIndex += 1;
            }
        });

        if (itemIndex === 0) {
            showToast('Please add at least one exercise to the template');
            return;
        }

        form.submit();
    }

    window.openAddExerciseModal = openAddExerciseModal;
    window.closeAddExerciseModal = closeAddExerciseModal;
    window.addExerciseToTemplate = addExerciseToTemplate;
    window.changeTargetSets = changeTargetSets;
    window.openDropSetModal = openDropSetModal;
    window.closeDropSetModal = closeDropSetModal;
    window.addDropSetToExercise = addDropSetToExercise;
    window.removeDropSet = removeDropSet;
    window.removeExercise = removeExercise;

    document.addEventListener('DOMContentLoaded', () => {
        populateExerciseSelect();
        loadExistingItems();
        const templateForm = document.getElementById('templateForm');
        if (templateForm) {
            templateForm.addEventListener('submit', handleTemplateFormSubmit);
        }
    });
})();

