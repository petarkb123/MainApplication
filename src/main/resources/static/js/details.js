(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        processExerciseGrouping();

        setTimeout(() => {
            const cards = document.querySelectorAll('.exercise-detail-card');
            cards.forEach((card, index) => {
                card.style.opacity = '0';
                card.style.transform = 'translateY(20px)';

                setTimeout(() => {
                    card.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
                    card.style.opacity = '1';
                    card.style.transform = 'translateY(0)';
                }, 100 + (index * 100));
            });
        }, 50);

        const summaryCards = document.querySelectorAll('.summary-card');
        summaryCards.forEach((card, index) => {
            card.style.opacity = '0';
            card.style.transform = 'translateY(20px)';

            setTimeout(() => {
                card.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
                card.style.opacity = '1';
                card.style.transform = 'translateY(0)';
            }, 50 + (index * 50));
        });
    });

    function processExerciseGrouping() {
        const container = document.getElementById('exercisesDetails');
        if (!container) return;

        const exerciseCards = Array.from(container.querySelectorAll('.exercise-detail-card'));
        const processedExercises = [];
        const supersetGroups = new Map();

        const allExercises = exerciseCards.map(card => {
            const exerciseId = card.getAttribute('data-exercise-id');
            const exerciseName = card.getAttribute('data-exercise-name');
            const exerciseMuscle = card.getAttribute('data-exercise-muscle');

            const sets = [];
            const setElements = card.querySelectorAll('.set-data');
            setElements.forEach(setEl => {
                sets.push({
                    weight: parseFloat(setEl.getAttribute('data-weight')),
                    reps: parseInt(setEl.getAttribute('data-reps')),
                    groupId: setEl.getAttribute('data-group-id') || null,
                    groupType: setEl.getAttribute('data-group-type') || null,
                    groupOrder: setEl.getAttribute('data-group-order') ? parseInt(setEl.getAttribute('data-group-order')) : null,
                    setNumber: setEl.getAttribute('data-set-number') ? parseInt(setEl.getAttribute('data-set-number')) : null
                });
            });

            return { exerciseId, exerciseName, exerciseMuscle, sets };
        });

        allExercises.forEach(exercise => {
            const supersetSet = exercise.sets.find(s => s.groupType === 'SUPERSET' && s.groupId && s.groupId !== 'null');
            if (supersetSet) {
                const groupId = supersetSet.groupId;
                if (!supersetGroups.has(groupId)) {
                    supersetGroups.set(groupId, []);
                }
                supersetGroups.get(groupId).push(exercise);
            }
        });

        const supersetExerciseIds = new Set();
        supersetGroups.forEach(group => {
            group.forEach(ex => supersetExerciseIds.add(ex.exerciseId));
        });

        const processedSupersetGroups = new Set();

        allExercises.forEach(exercise => {
            const supersetSet = exercise.sets.find(s => s.groupType === 'SUPERSET' && s.groupId && s.groupId !== 'null');

            if (supersetSet) {
                const groupId = supersetSet.groupId;

                if (!processedSupersetGroups.has(groupId)) {
                    const group = supersetGroups.get(groupId);
                    if (group && group.length === 2) {
                        group.sort((a, b) => {
                            const orderA = a.sets.find(s => s.groupType === 'SUPERSET')?.groupOrder || 0;
                            const orderB = b.sets.find(s => s.groupType === 'SUPERSET')?.groupOrder || 0;
                            return orderA - orderB;
                        });

                        processedExercises.push({
                            type: 'superset',
                            groupId,
                            exercises: group
                        });
                        processedSupersetGroups.add(groupId);
                    }
                }
            } else {
                processedExercises.push({
                    type: 'regular',
                    exerciseId: exercise.exerciseId,
                    exerciseName: exercise.exerciseName,
                    exerciseMuscle: exercise.exerciseMuscle,
                    sets: exercise.sets
                });
            }
        });

        container.innerHTML = '';

        processedExercises.forEach(item => {
            if (item.type === 'superset') {
                container.appendChild(createSupersetCard(item));
            } else {
                container.appendChild(createRegularCard(item));
            }
        });
    }

    function createSupersetCard(data) {
        const card = document.createElement('div');
        card.className = 'exercise-detail-card superset-detail-card';

        const ex1 = data.exercises[0];
        const ex2 = data.exercises[1];

        const supersetSets = ex1.sets.filter(s => s.groupType === 'SUPERSET');
        const totalVolume = supersetSets.reduce((sum, s, i) => {
            const ex2Set = ex2.sets.filter(s => s.groupType === 'SUPERSET')[i];
            return sum + (s.weight * s.reps) + (ex2Set ? ex2Set.weight * ex2Set.reps : 0);
        }, 0);

        let setsHTML = '';
        supersetSets.forEach((set1, index) => {
            const set2 = ex2.sets.filter(s => s.groupType === 'SUPERSET')[index];
            setsHTML += `
                <div class="superset-set-row">
                    <span class="set-number">${index + 1}</span>
                    <div class="superset-set-details">
                        <div class="exercise-set-detail">
                            <span class="exercise-label">${ex1.exerciseName}</span>
                            <span class="set-value">${set1.weight} kg × ${set1.reps} reps</span>
                        </div>
                        <div class="superset-divider">+</div>
                        <div class="exercise-set-detail">
                            <span class="exercise-label">${ex2.exerciseName}</span>
                            <span class="set-value">${set2.weight} kg × ${set2.reps} reps</span>
                        </div>
                    </div>
                    <span class="volume">${(set1.weight * set1.reps + set2.weight * set2.reps).toFixed(1)} kg</span>
                </div>
            `;
        });

        card.innerHTML = `
            <div class="exercise-header">
                <div class="exercise-info">
                    <h3 class="exercise-name">
                        <span class="superset-badge">SUPERSET</span>
                        <span class="superset-exercise-titles-desktop">${ex1.exerciseName} + ${ex2.exerciseName}</span>
                    </h3>
                    <div class="superset-exercise-titles-mobile">
                        ${ex1.exerciseName} + ${ex2.exerciseName}
                    </div>
                    <span class="exercise-muscle">${ex1.exerciseMuscle} / ${ex2.exerciseMuscle}</span>
                </div>
                <div class="exercise-stats">
                    <span class="stat-badge">
                        <i class="fas fa-layer-group"></i>
                        <span>${supersetSets.length} sets</span>
                    </span>
                </div>
            </div>
            <div class="sets-details">
                <div class="sets-table superset-table">
                    <div class="table-header">
                        <span>Set</span>
                        <span>Exercises</span>
                        <span>Volume</span>
                    </div>
                    <div class="table-body">
                        ${setsHTML}
                    </div>
                </div>
                <div class="exercise-summary">
                    <div class="summary-stat">
                        <span class="stat-label">Total Volume:</span>
                        <span class="stat-value">${totalVolume.toFixed(1)} kg</span>
                    </div>
                    <div class="summary-stat">
                        <span class="stat-label">Sets Completed:</span>
                        <span class="stat-value">${supersetSets.length} sets</span>
                    </div>
                </div>
            </div>
        `;

        return card;
    }

    function createRegularCard(data) {
        const card = document.createElement('div');
        card.className = 'exercise-detail-card';

        const totalVolume = data.sets.reduce((sum, s) => sum + (s.weight * s.reps), 0);
        const totalReps = data.sets.reduce((sum, s) => sum + s.reps, 0);
        const avgWeight = totalVolume / totalReps;

        const regularSetCount = data.sets.filter(s => !s.groupType || s.groupType !== 'DROP_SET' || (s.groupOrder === 0 || s.groupOrder === '0')).length;

        let setsHTML = '';

        const setsBySetNumber = new Map();
        data.sets.forEach(set => {
            const setNum = set.setNumber || 1;
            if (!setsBySetNumber.has(setNum)) {
                setsBySetNumber.set(setNum, []);
            }
            setsBySetNumber.get(setNum).push(set);
        });

        const sortedSetNumbers = Array.from(setsBySetNumber.keys()).sort((a, b) => a - b);

        for (const setNum of sortedSetNumbers) {
            const setsForThisNumber = setsBySetNumber.get(setNum);

            const mainSet = setsForThisNumber.find(s => !s.groupType || s.groupType !== 'DROP_SET' || s.groupOrder === 0);
            const dropSets = setsForThisNumber.filter(s => s.groupType === 'DROP_SET' && s.groupOrder > 0);

            if (mainSet) {
                setsHTML += `
                    <div class="set-row">
                        <span class="set-number">${setNum}</span>
                        <span class="weight">${mainSet.weight} kg</span>
                        <span class="reps">${mainSet.reps} reps</span>
                        <span class="volume">${(mainSet.weight * mainSet.reps).toFixed(1)} kg</span>
                    </div>
                `;

                dropSets.forEach(dropSet => {
                    setsHTML += `
                        <div class="set-row drop-set-row">
                            <span class="set-number">
                                <span class="drop-badge">Drop ${dropSet.groupOrder}</span>
                            </span>
                            <span class="weight">${dropSet.weight} kg</span>
                            <span class="reps">${dropSet.reps} reps</span>
                            <span class="volume">${(dropSet.weight * dropSet.reps).toFixed(1)} kg</span>
                        </div>
                    `;
                });
            } else {
                setsForThisNumber.forEach(set => {
                    setsHTML += `
                        <div class="set-row">
                            <span class="set-number">${setNum}</span>
                            <span class="weight">${set.weight} kg</span>
                            <span class="reps">${set.reps} reps</span>
                            <span class="volume">${(set.weight * set.reps).toFixed(1)} kg</span>
                        </div>
                    `;
                });
            }
        }

        card.innerHTML = `
            <div class="exercise-header">
                <div class="exercise-info">
                    <h3 class="exercise-name">${data.exerciseName}</h3>
                    <span class="exercise-muscle">${data.exerciseMuscle}</span>
                </div>
                <div class="exercise-stats">
                    <span class="stat-badge">
                        <i class="fas fa-layer-group"></i>
                        <span>${regularSetCount} sets</span>
                    </span>
                </div>
            </div>
            <div class="sets-details">
                <div class="sets-table">
                    <div class="table-header">
                        <span>Set</span>
                        <span>Weight</span>
                        <span>Reps</span>
                        <span>Volume</span>
                    </div>
                    <div class="table-body">
                        ${setsHTML}
                    </div>
                </div>
                <div class="exercise-summary">
                    <div class="summary-stat">
                        <span class="stat-label">Total Volume:</span>
                        <span class="stat-value">${totalVolume.toFixed(1)} kg</span>
                    </div>
                    <div class="summary-stat">
                        <span class="stat-label">Average Weight:</span>
                        <span class="stat-value">${avgWeight.toFixed(1)} kg</span>
                    </div>
                    <div class="summary-stat">
                        <span class="stat-label">Total Reps:</span>
                        <span class="stat-value">${totalReps} reps</span>
                    </div>
                </div>
            </div>
        `;

        return card;
    }
})();


