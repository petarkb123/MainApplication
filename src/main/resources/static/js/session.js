(function () {
    'use strict';

    const data = window.__pageDataSession || {};
    const exercisesRaw = Array.isArray(data.exercises) ? data.exercises : [];
    const workoutSessionId = data.sessionId || null;
    const SYSTEM_USER_ID = data.systemUserId || null;
    const existingSetsRaw = data.existingSets || null;
    const existingSets = existingSetsRaw || [];
    const startedAtIso = data.startedAt || null;
    const isPro = Boolean(data.isPro);
    const draftKey = workoutSessionId ? (`workoutDraft:${workoutSessionId}`) : null;


    // showToast is now in utils.js
    let exerciseCounter = 0;

    const exercises = (exercisesRaw || []).map(ex => {
        const mg = ex.muscleGroup ?? ex.primaryMuscle ?? 'OTHER';
        const hasBuiltProp = Object.prototype.hasOwnProperty.call(ex, 'builtIn');
        const builtIn = hasBuiltProp ? !!ex.builtIn
            : String(ex.ownerUserId ?? '') === String(SYSTEM_USER_ID ?? '');
        return { ...ex, muscleGroup: mg, builtIn };
    });
    

    function groupByMuscle(items) {
        return items.reduce((acc, ex) => {
            const k = ex.muscleGroup || 'OTHER';
            (acc[k] ||= []).push(ex);
            return acc;
        }, {});
    }

    function optgroupsFor(prefix, arr) {
        const by = groupByMuscle(arr);
        const order = ['CHEST','BACK','LEGS','SHOULDERS','BICEPS','TRICEPS','FOREARMS','HAMSTRINGS','CALVES','CORE','OTHER'];
        return order
            .filter(k => by[k] && by[k].length)
            .map(k => {
                const opts = by[k]
                    .slice()
                    .sort((a,b) => a.name.localeCompare(b.name))
                    .map(ex => `<option value="${ex.id}">${ex.name}</option>`)
                    .join('');
                return `<optgroup label="${prefix} — ${k}">${opts}</optgroup>`;
            })
            .join('');
    }

    function optionHtml() {
        if (!exercises || exercises.length === 0) return '<option value="">No exercises</option>';

        const mine     = exercises.filter(ex => !ex.builtIn);
        const builtIns = exercises.filter(ex =>  ex.builtIn);

        let html = '<option value="">Select Exercise</option>';
        html += optgroupsFor('My exercises', mine);
        html += '<option value="" disabled>&nbsp;</option>';
        html += optgroupsFor('Built-in', builtIns);
        return html;
    }


    function populateExerciseSelect() {
        const sel = document.getElementById('exerciseSelect');
        if (sel) sel.innerHTML = optionHtml();
    }

    function openAddExerciseModal() {
        populateExerciseSelect(); // keep options fresh
        const m = document.getElementById('addExerciseModal');
        m.style.display = 'flex'; setTimeout(() => m.classList.add('show'), 10);
    }
    function closeAddExerciseModal() {
        const m = document.getElementById('addExerciseModal');
        m.classList.remove('show'); setTimeout(() => m.style.display = 'none', 300);
    }

    function addExerciseToWorkout() {
        const select = document.getElementById('exerciseSelect');
        const exerciseId = select.value;
        if (!exerciseId) { showToast('Please select an exercise'); return; }
        const exercise = exercises.find(ex => String(ex.id) === String(exerciseId));
        if (!exercise) { showToast('Exercise not found'); return; }
        addExerciseCard(exercise);
        select.value = '';
        closeAddExerciseModal();
    }

    function addExerciseCard(exercise) {
        const exercisesList = document.getElementById('exercisesList');
        const emptyWorkout = document.getElementById('emptyWorkout');
        if (emptyWorkout) emptyWorkout.style.display = 'none';

        const exerciseCard = document.createElement('div');
        exerciseCard.className = 'exercise-card';
        exerciseCard.id = `exercise-${exerciseCounter}`;
        exerciseCard.setAttribute('data-exercise-id', exercise.id);

        exerciseCard.innerHTML = `
      <div class="exercise-header">
        <div class="exercise-info">
          <h3 class="exercise-name">${exercise.name}</h3>
          <span class="exercise-muscle">${exercise.muscleGroup}</span>
        </div>
        <button class="btn-icon btn-remove" onclick="removeExercise(${exerciseCounter})">
          <i class="fas fa-trash"></i>
        </button>
      </div>
      <div class="sets-container">
        <div class="sets-header">
          <span>Sets</span>
          <button class="btn btn-sm btn-outline" onclick="addSet(${exerciseCounter})">
            <i class="fas fa-plus"></i> Add Set
          </button>
        </div>
        <div class="sets-list" id="sets-${exerciseCounter}"></div>
      </div>
    `;

        exercisesList.appendChild(exerciseCard);
        
        const targetSets = exercise.targetSets || 1;
        for (let i = 0; i < targetSets; i++) {
            addSet(exerciseCounter);
        }
        
        exerciseCounter++;
    }

    function addSet(exerciseIndex, isDropSet = false, dropSetLevel = 0) {
        const setsList = document.getElementById(`sets-${exerciseIndex}`);
        const setNumber = setsList.children.length + 1;

        const row = document.createElement('div');
        row.className = 'set-row' + (isDropSet ? ' drop-set-row' : '');
        row.setAttribute('data-drop-level', dropSetLevel);
        
        const dropSetLabel = isDropSet ? ` <span class="drop-badge">Drop ${dropSetLevel}</span>` : '';
        
        row.innerHTML = `
      <div class="set-number">${setNumber}${dropSetLabel}</div>
      <div class="set-inputs">
        <input type="number" class="form-input" placeholder="Weight" min="0" step="0.5">
        <span class="input-label">kg</span>
        <input type="number" class="form-input" placeholder="Reps" min="1">
        <span class="input-label">reps</span>
      </div>
      <div class="set-actions">
        ${!isDropSet ? (isPro ? '<button class="btn-icon btn-drop" onclick="addDropSet(this)" title="Add drop set"><i class="fas fa-layer-group"></i></button>' : '') : ''}
        <button class="btn-icon btn-remove" onclick="removeSet(this)">
          <i class="fas fa-trash"></i>
        </button>
      </div>
    `;
        setsList.appendChild(row);
        renumberSets(exerciseIndex);
    }
    
    function addDropSet(btn) {
        const currentRow = btn.closest('.set-row');
        const setsList = currentRow.parentElement;
        const exerciseIndex = setsList.id.replace('sets-', '');
        const currentLevel = parseInt(currentRow.getAttribute('data-drop-level') || '0');
        
        if (currentLevel === 0) {
            currentRow.setAttribute('data-has-drops', 'true');
            if (!currentRow.getAttribute('data-group-id')) {
            currentRow.setAttribute('data-group-id', generateUUID());
            }
        }
        
        let nextDropLevel = currentLevel + 1;
        let insertAfter = currentRow;
        
        let nextSibling = currentRow.nextElementSibling;
        while (nextSibling && nextSibling.classList.contains('drop-set-row')) {
            const siblingLevel = parseInt(nextSibling.getAttribute('data-drop-level') || '0');
            if (siblingLevel <= currentLevel) break;
            nextDropLevel = Math.max(nextDropLevel, siblingLevel + 1);
            insertAfter = nextSibling;
            nextSibling = nextSibling.nextElementSibling;
        }
        
        const dropRow = document.createElement('div');
        dropRow.className = 'set-row drop-set-row';
        dropRow.setAttribute('data-drop-level', nextDropLevel);
        dropRow.style.marginLeft = (nextDropLevel * 20) + 'px';
        
        dropRow.innerHTML = `
      <div class="set-number"><span class="drop-badge">Drop ${nextDropLevel}</span></div>
      <div class="set-inputs">
        <input type="number" class="form-input" placeholder="Weight" min="0" step="0.5">
        <span class="input-label">kg</span>
        <input type="number" class="form-input" placeholder="Reps" min="1">
        <span class="input-label">reps</span>
      </div>
      <div class="set-actions">
        <button class="btn-icon btn-drop" th:if="${isPro}" onclick="addDropSet(this)" title="Add another drop"><i class="fas fa-layer-group"></i></button>
        <button class="btn-icon btn-remove" onclick="removeSet(this)">
          <i class="fas fa-trash"></i>
        </button>
      </div>
    `;
        
        insertAfter.after(dropRow);
        renumberSets(exerciseIndex);
    }
    
    function renumberSets(exerciseIndex) {
        const setsList = document.getElementById(`sets-${exerciseIndex}`);
        const rows = setsList.querySelectorAll('.set-row');
        let mainSetNum = 0;
        
        rows.forEach((row) => {
            const isDropSet = row.classList.contains('drop-set-row');
            if (!isDropSet) {
                mainSetNum++;
                const dropLevel = parseInt(row.getAttribute('data-drop-level') || '0');
                row.querySelector('.set-number').innerHTML = `${mainSetNum}`;
            }
        });
    }

    function removeSet(btn) {
        const row = btn.closest('.set-row');
        const list = row.parentElement;
        const exerciseIndex = list.id.replace('sets-', '');
        
        // If removing a main set, also remove its drop sets
        if (!row.classList.contains('drop-set-row')) {
            let nextSibling = row.nextElementSibling;
            while (nextSibling && nextSibling.classList.contains('drop-set-row')) {
                const toRemove = nextSibling;
                nextSibling = nextSibling.nextElementSibling;
                toRemove.remove();
            }
        }
        
        row.remove();
        renumberSets(exerciseIndex);
    }

    function removeExercise(exIdx) {
        const card = document.getElementById(`exercise-${exIdx}`);
        card.remove();
        const list = document.getElementById('exercisesList');
        const empty = document.getElementById('emptyWorkout');
        if (list.children.length === 0 && empty) empty.style.display = 'block';
    }

    function finishWorkout() {
        const cards = document.querySelectorAll('.exercise-card');
        if (cards.length === 0) { showToast('Please add at least one exercise'); return; }

        const payload = { sessionId: workoutSessionId, exercises: [] };

        cards.forEach(card => {
            const exerciseId = card.getAttribute('data-exercise-id');
            if (!exerciseId) {
                console.error('Card missing exercise ID:', card);
                return;
            }
            
            const sets = [];
            let mainSetIndex = 0;
            
            card.querySelectorAll('.set-row').forEach(row => {
                const isDropSet = row.classList.contains('drop-set-row');
                const dropLevel = parseInt(row.getAttribute('data-drop-level') || '0');
                
                const inputs = row.querySelectorAll('input[placeholder]');
                const w = inputs[0]?.value;
                const r = inputs[1]?.value;
                const weight = w ? parseFloat(w) : 0;
                const reps = r ? parseInt(r, 10) : 0;
                    
                    if (weight > 0 && reps > 0) {
                        const setData = { weight, reps };
                        
                        if (isDropSet && dropLevel > 0) {
                            let mainSetRow = null;
                            let mainSetNumber = null;
                            
                            let currentRow = row.previousElementSibling;
                            while (currentRow && currentRow.classList.contains('drop-set-row')) {
                                currentRow = currentRow.previousElementSibling;
                            }
                            
                            if (currentRow && !currentRow.classList.contains('drop-set-row')) {
                                mainSetRow = currentRow;
                                const allMainSets = Array.from(card.querySelectorAll('.set-row:not(.drop-set-row)'));
                                mainSetNumber = allMainSets.indexOf(mainSetRow) + 1;
                            }
                            
                            if (mainSetRow && mainSetRow.getAttribute('data-group-id')) {
                                setData.groupId = mainSetRow.getAttribute('data-group-id');
                                setData.setNumber = mainSetNumber;
                            } else {
                                setData.groupId = generateUUID();
                                setData.setNumber = mainSetNumber || 1;
                            }
                            setData.groupType = 'DROP_SET';
                            setData.groupOrder = dropLevel;
                            
                        } else {
                            mainSetIndex++;
                            if (row.getAttribute('data-has-drops') === 'true') {
                                const groupId = row.getAttribute('data-group-id') || generateUUID();
                                setData.groupId = groupId;
                                setData.groupType = 'DROP_SET';
                                setData.groupOrder = 0;
                                setData.setNumber = mainSetIndex;
                                row.setAttribute('data-group-id', groupId);
                            } else {
                                setData.setNumber = mainSetIndex;
                            }
                        }
                        
                        sets.push(setData);
                    }
                });
                
                if (exerciseId && sets.length > 0) {
                    payload.exercises.push({ exerciseId, sets });
                }
        });

        if (payload.exercises.length === 0) { showToast('Please log at least one set before finishing the workout'); return; }

        
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
        const csrfToken  = document.querySelector('meta[name="_csrf"]').content;

        fetch('/workouts/finish', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
            body: JSON.stringify(payload)
        })
            .then(r => { 
                if (r.ok) {
                    // Clear draft on success
                    try { if (draftKey) localStorage.removeItem(draftKey); } catch(_) {}
                    window.location.href = '/workouts'; 
                } else {
                    r.text().then(() => { showToast('Error finishing workout: ' + r.status + ' ' + r.statusText); });
                }
            })
            .catch(err => { showToast('Error finishing workout: ' + err.message); });
    }

    function generateUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            const r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }

    function getSelectedCards() {
        return Array.from(document.querySelectorAll('.exercise-card'))
            .filter(card => card.querySelector('.exercise-select')?.checked);
    }
    
    function createExerciseCard(exerciseId, exerciseName, exerciseMuscle) {
        const card = document.createElement('div');
        card.className = 'exercise-card';
        card.id = `exercise-${exerciseCounter}`;
        card.setAttribute('data-exercise-id', exerciseId);
        
        card.innerHTML = `
      <div class="exercise-header">
        <div class="exercise-info">
          <h3 class="exercise-name">${exerciseName}</h3>
          <span class="exercise-muscle">${exerciseMuscle}</span>
        </div>
        <button class="btn-icon btn-remove" onclick="removeExercise(${exerciseCounter})">
          <i class="fas fa-trash"></i>
        </button>
      </div>
      <div class="sets-container">
        <div class="sets-header">
          <span>Sets</span>
          <button class="btn btn-sm btn-outline" onclick="addSet(${exerciseCounter})">
            <i class="fas fa-plus"></i> Add Set
          </button>
        </div>
        <div class="sets-list" id="sets-${exerciseCounter}"></div>
      </div>
    `;
        
        exerciseCounter++;
        return card;
    }
    
    function createExerciseCardWithData(exerciseId, exerciseName, exerciseMuscle, setData) {
        const card = document.createElement('div');
        card.className = 'exercise-card';
        const currentIndex = exerciseCounter;
        card.id = `exercise-${currentIndex}`;
        card.setAttribute('data-exercise-id', exerciseId);
        
        card.innerHTML = `
      <div class="exercise-header">
        <div class="exercise-info">
          <h3 class="exercise-name">${exerciseName}</h3>
          <span class="exercise-muscle">${exerciseMuscle}</span>
        </div>
        <button class="btn-icon btn-remove" onclick="removeExercise(${currentIndex})">
          <i class="fas fa-trash"></i>
        </button>
      </div>
      <div class="sets-container">
        <div class="sets-header">
          <span>Sets</span>
          <button class="btn btn-sm btn-outline" onclick="addSet(${currentIndex})">
            <i class="fas fa-plus"></i> Add Set
          </button>
        </div>
        <div class="sets-list" id="sets-${currentIndex}"></div>
      </div>
    `;
        
        exerciseCounter++;
        
        const setsList = card.querySelector('.sets-list');
        setData.forEach((data, index) => {
            const setNumber = index + 1;
            const row = document.createElement('div');
            row.className = 'set-row';
            row.innerHTML = `
        <div class="set-number">${setNumber}</div>
        <div class="set-inputs">
          <input type="number" class="form-input" placeholder="Weight" min="0" step="0.5" value="${data.weight}">
          <span class="input-label">kg</span>
          <input type="number" class="form-input" placeholder="Reps" min="1" value="${data.reps}">
          <span class="input-label">reps</span>
        </div>
        <div class="set-actions">
          <button class="btn-icon btn-drop" th:if="${isPro}" onclick="addDropSet(this)" title="Add drop set"><i class="fas fa-layer-group"></i></button>
          <button class="btn-icon btn-remove" onclick="removeSet(this)">
            <i class="fas fa-trash"></i>
          </button>
        </div>
      `;
            setsList.appendChild(row);
        });
        
        return card;
    }

    function loadTemplateExercises(templateExercises) {
        const sortedExercises = [...templateExercises].sort((a, b) => {
            const posA = a.position !== undefined ? a.position : 0;
            const posB = b.position !== undefined ? b.position : 0;
            return posA - posB;
        });
        
        const dropSetGroups = new Map();
        const processed = new Set();
        
        for (const ex of sortedExercises) {
            if (ex.groupId && ex.groupType === 'DROP_SET') {
                if (!dropSetGroups.has(ex.groupId)) {
                    dropSetGroups.set(ex.groupId, []);
                }
                dropSetGroups.get(ex.groupId).push(ex);
            }
        }
        
        for (let i = 0; i < sortedExercises.length; i++) {
            if (processed.has(i)) continue;
            const ex = sortedExercises[i];
            
            if (ex.groupId && ex.groupType === 'DROP_SET') {
                const group = dropSetGroups.get(ex.groupId);
                if (group && group.length > 0) {
                    group.sort((a, b) => (a.groupOrder || 0) - (b.groupOrder || 0));
                    
                    if (ex.groupOrder === 0) {
                        const exercisesList = document.getElementById('exercisesList');
                        const emptyState = document.getElementById('emptyWorkout');
                        if (emptyState) emptyState.style.display = 'none';
                        
                        const card = document.createElement('div');
                        card.className = 'exercise-card';
                        card.id = `exercise-${exerciseCounter}`;
                        card.setAttribute('data-exercise-id', ex.id);
                        card.innerHTML = `
                  <div class="exercise-header">
                    <div class="exercise-info">
                      <h3 class="exercise-name">${ex.name}</h3>
                      <span class="exercise-muscle">${ex.muscleGroup}</span>
                    </div>
                    <div class="exercise-actions">
                      <button class="btn-icon btn-drop" th:if="${isPro}" onclick="addDropSet(this)" title="Add drop set">
                        <i class="fas fa-layer-group"></i>
                      </button>
                      <button class="btn-icon btn-remove" onclick="removeExercise(${exerciseCounter})">
                        <i class="fas fa-trash"></i>
                      </button>
                    </div>
                  </div>
                  <div class="sets-container">
                    <div class="sets-header">
                      <span>Sets</span>
                      <button class="btn btn-sm btn-outline" onclick="addSet(${exerciseCounter})">
                        <i class="fas fa-plus"></i> Add Set
                      </button>
                    </div>
                    <div class="sets-list" id="sets-${exerciseCounter}"></div>
                  </div>
                `;
                        
                        exercisesList.appendChild(card);
                        
                        const targetSets = ex.targetSets || 3;
                        for (let i = 0; i < targetSets; i++) {
                            addSet(exerciseCounter);
                        }
                        
                        const dropSets = group.filter(item => item.groupOrder > 0);
                        const setsList = card.querySelector('.sets-list');
                        const allSetRows = setsList.querySelectorAll('.set-row:not(.drop-set-row)');
                        
                        if (dropSets.length > 0) {
                            const dropSetsBySetNumber = new Map();
                            dropSets.forEach(dropSet => {
                                const setNumber = dropSet.setNumber || ex.targetSets;
                                if (!dropSetsBySetNumber.has(setNumber)) {
                                    dropSetsBySetNumber.set(setNumber, []);
                                }
                                dropSetsBySetNumber.get(setNumber).push(dropSet);
                            });
                            
                            dropSetsBySetNumber.forEach((sets, setNumber) => {
                                const targetSetRow = allSetRows[setNumber - 1];
                                if (targetSetRow) {
                                    targetSetRow.setAttribute('data-has-drops', 'true');
                                    const groupId = ex.groupId || generateUUID();
                                    targetSetRow.setAttribute('data-group-id', groupId);
                                    
                                    sets.forEach((dropSet, index) => {
                                        const dropLevel = dropSet.groupOrder || (index + 1);
                                        const dropRow = document.createElement('div');
                                        dropRow.className = 'set-row drop-set-row';
                                        dropRow.setAttribute('data-drop-level', dropLevel);
                                        dropRow.style.marginLeft = (dropLevel * 20) + 'px';
                                        
                                        dropRow.innerHTML = `
                                  <div class="set-number"><span class="drop-badge">Drop ${dropLevel}</span></div>
                                  <div class="set-inputs">
                                    <input type="number" class="form-input" placeholder="Weight" min="0" step="0.5">
                                    <span class="input-label">kg</span>
                                    <input type="number" class="form-input" placeholder="Reps" min="1">
                                    <span class="input-label">reps</span>
                                  </div>
                                  <div class="set-actions">
                                    <button class="btn-icon btn-drop" onclick="addDropSet(this)" title="Add another drop">
                                      <i class="fas fa-layer-group"></i>
                                    </button>
                                    <button class="btn-icon btn-remove" onclick="removeSet(this)">
                                      <i class="fas fa-trash"></i>
                                    </button>
                                  </div>
                                `;
                                        
                                        let insertAfter = targetSetRow;
                                        let nextSibling = targetSetRow.nextElementSibling;
                                        while (nextSibling && nextSibling.classList.contains('drop-set-row')) {
                                            insertAfter = nextSibling;
                                            nextSibling = nextSibling.nextElementSibling;
                                        }
                                        insertAfter.insertAdjacentElement('afterend', dropRow);
                                    });
                                }
                            });
                        }
                        
                        for (let j = 0; j < sortedExercises.length; j++) {
                            const item = sortedExercises[j];
                            if (item.groupId === ex.groupId && item.groupType === 'DROP_SET') {
                                processed.add(j);
                            }
                        }
                        
                        exerciseCounter++;
                    }
                }
            }
            else if (!ex.groupId) {
                addExerciseCard(ex);
                processed.add(i);
            }
        }
    }
    
    function loadExistingSets() {
        if (!existingSets || existingSets.length === 0) return;
        
        const byExercise = new Map();
        
        for (const set of existingSets) {
            const exId = set.exerciseId;
            
            if (!byExercise.has(exId)) {
                byExercise.set(exId, []);
            }
            byExercise.get(exId).push(set);
        }

        for (const [exId, sets] of byExercise.entries()) {
            const ex = exercises.find(e => String(e.id) === String(exId));
            if (!ex) continue;

            const exercisesList = document.getElementById('exercisesList');
            const emptyState = document.getElementById('emptyWorkout');
            if (emptyState) emptyState.style.display = 'none';
            
            const exerciseCard = document.createElement('div');
            exerciseCard.className = 'exercise-card';
            exerciseCard.id = `exercise-${exerciseCounter}`;
            exerciseCard.setAttribute('data-exercise-id', ex.id);
            
            exerciseCard.innerHTML = `
                <div class="exercise-header">
                    <div class="exercise-info">
                        <h3 class="exercise-name">${ex.name}</h3>
                        <span class="exercise-muscle">${ex.muscleGroup}</span>
                    </div>
                    <button class="btn-icon btn-remove" onclick="removeExercise(${exerciseCounter})">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
                <div class="sets-container">
                    <div class="sets-header">
                        <span>Sets</span>
                        <button class="btn btn-sm btn-outline" onclick="addSet(${exerciseCounter})">
                            <i class="fas fa-plus"></i> Add Set
                        </button>
                    </div>
                    <div class="sets-list" id="sets-${exerciseCounter}"></div>
                </div>
            `;
            
            exercisesList.appendChild(exerciseCard);
            
            const setsList = exerciseCard.querySelector('.sets-list');
            
            const dropSetGroups = new Map();
            
            for (const set of sets) {
                if (set.groupId && set.groupType === 'DROP_SET') {
                    if (!dropSetGroups.has(set.groupId)) {
                        dropSetGroups.set(set.groupId, []);
                    }
                    dropSetGroups.get(set.groupId).push(set);
                }
            }
            
            for (const set of sets) {
                if (set.groupId && set.groupType === 'DROP_SET' && set.groupOrder > 0) {
                    continue;
                }
                
                const setNumber = setsList.children.length + 1;
                const isMainSetWithDrops = set.groupId && set.groupType === 'DROP_SET' && set.groupOrder === 0;
                
                const row = document.createElement('div');
                row.className = 'set-row';
                row.setAttribute('data-drop-level', '0');
                if (isMainSetWithDrops) {
                    row.setAttribute('data-group-id', set.groupId);
                    row.setAttribute('data-has-drops', 'true');
                }
                
                row.innerHTML = `
                    <div class="set-number">${setNumber}</div>
                    <div class="set-inputs">
                        <input type="number" class="form-input" placeholder="Weight" min="0" step="0.5" value="${set.weight || ''}">
                        <span class="input-label">kg</span>
                        <input type="number" class="form-input" placeholder="Reps" min="1" value="${set.reps || ''}">
                        <span class="input-label">reps</span>
                    </div>
                    <div class="set-actions">
                        <button class="btn-icon btn-drop" th:if="${isPro}" onclick="addDropSet(this)" title="Add drop set"><i class="fas fa-layer-group"></i></button>
                        <button class="btn-icon btn-remove" onclick="removeSet(this)">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                `;
                setsList.appendChild(row);
                
                if (isMainSetWithDrops && dropSetGroups.has(set.groupId)) {
                    const drops = dropSetGroups.get(set.groupId)
                        .filter(d => d.groupOrder > 0)
                        .sort((a, b) => a.groupOrder - b.groupOrder);
                    
                    for (const dropSet of drops) {
                        const dropLevel = dropSet.groupOrder;
                        const dropRow = document.createElement('div');
                        dropRow.className = 'set-row drop-set-row';
                        dropRow.setAttribute('data-drop-level', dropLevel);
                        dropRow.style.marginLeft = (dropLevel * 20) + 'px';
                        
                        dropRow.innerHTML = `
                            <div class="set-number"><span class="drop-badge">Drop ${dropLevel}</span></div>
                            <div class="set-inputs">
                                <input type="number" class="form-input" placeholder="Weight" min="0" step="0.5" value="${dropSet.weight || ''}">
                                <span class="input-label">kg</span>
                                <input type="number" class="form-input" placeholder="Reps" min="1" value="${dropSet.reps || ''}">
                                <span class="input-label">reps</span>
                            </div>
                            <div class="set-actions">
                                <button class="btn-icon btn-drop" th:if="${isPro}" onclick="addDropSet(this)" title="Add another drop"><i class="fas fa-layer-group"></i></button>
                                <button class="btn-icon btn-remove" onclick="removeSet(this)">
                                    <i class="fas fa-trash"></i>
                                </button>
                            </div>
                        `;
                        setsList.appendChild(dropRow);
                    }
                }
            }
            
            exerciseCounter++;
        }
    }
    
    document.addEventListener('DOMContentLoaded', function () {
        populateExerciseSelect();
        attachAutosaveHandlers();

        if (existingSets && existingSets.length > 0) {
            loadExistingSets();
            return;
        }

        const templateId = new URLSearchParams(location.search).get('templateId');
        if (templateId) {
            fetch(`/workouts/templates/${templateId}/exercises`)
                .then(r => r.json())
                .then(list => loadTemplateExercises(list))
                .catch(err => console.error('Template preload failed:', err));
        } else {
            setTimeout(loadDraftIfAny, 0);
        }
    });

    (function initTimer(){
        const el = document.getElementById('timerText');
        if (!el) return;
        const start = startedAtIso ? new Date(startedAtIso) : new Date();
        function tick(){
            const diffMs = Date.now() - start.getTime();
            const sec = Math.floor(diffMs/1000);
            const h = String(Math.floor(sec/3600)).padStart(2,'0');
            const m = String(Math.floor((sec%3600)/60)).padStart(2,'0');
            const s = String(sec%60).padStart(2,'0');
            el.textContent = `${h}:${m}:${s}`;
        }
        tick();
        setInterval(tick, 1000);
    })();

    let autosaveTimer = null;
    function attachAutosaveHandlers(){
        document.addEventListener('input', function(e){
            if (!draftKey) return;
            if (!e.target.closest('.workout-session')) return;
            if (autosaveTimer) clearTimeout(autosaveTimer);
            autosaveTimer = setTimeout(saveDraft, 400);
        });
    }

    function saveDraft(){
        if (!draftKey) return;
        const cards = Array.from(document.querySelectorAll('.exercise-card'));
        const draft = [];
        cards.forEach(card => {
            const exerciseId = card.getAttribute('data-exercise-id');
            const rows = Array.from(card.querySelectorAll('.set-row'));
            const sets = rows.map(row => {
                const inputs = row.querySelectorAll('input');
                const isDrop = row.classList.contains('drop-set-row');
                const dropLevel = parseInt(row.getAttribute('data-drop-level')||'0');
                return { type: isDrop ? 'DROP' : 'MAIN', level: dropLevel, weight: inputs[0]?.value||'', reps: inputs[1]?.value||'' };
            });
            draft.push({ kind:'REGULAR', exerciseId, sets });
        });
        try { localStorage.setItem(draftKey, JSON.stringify(draft)); } catch(_) {}
    }

    function loadDraftIfAny(){
        if (!draftKey) return;
        let raw = null;
        try { raw = localStorage.getItem(draftKey); } catch(_) {}
        if (!raw) return;
        let data = null;
        try { data = JSON.parse(raw); } catch(_) { return; }
        if (!Array.isArray(data) || data.length === 0) return;

        const listEl = document.getElementById('exercisesList');
        const empty = document.getElementById('emptyWorkout');
        if (empty) empty.style.display = 'none';

        data.forEach(item => {
            if (item.kind === 'REGULAR') {
                const ex = exercises.find(e => String(e.id)===String(item.exerciseId));
                if (!ex) return;
                addExerciseCard(ex);
                const idx = exerciseCounter-1;
                const setsList = document.getElementById(`sets-${idx}`);
                setsList.innerHTML = '';
                item.sets.forEach(s => {
                    if (s.type === 'MAIN') {
                        addSet(idx);
                        const last = setsList.lastElementChild;
                        const inputs = last.querySelectorAll('input');
                        inputs[0].value = s.weight || '';
                        inputs[1].value = s.reps || '';
                    } else {
                        const mainExists = setsList.querySelector('.set-row:not(.drop-set-row):last-child');
                        if (!mainExists) { addSet(idx); }
                        const lastMain = setsList.querySelector('.set-row:not(.drop-set-row):last-child');
                        const dropBtn = lastMain.querySelector('.btn-drop');
                        if (dropBtn) { addDropSet(dropBtn); }
                        const lastRow = setsList.lastElementChild;
                        const inputs = lastRow.querySelectorAll('input');
                        inputs[0].value = s.weight || '';
                        inputs[1].value = s.reps || '';
                    }
                });
            }
        });
    }
    window.showToast = showToast;
    window.openAddExerciseModal = openAddExerciseModal;
    window.closeAddExerciseModal = closeAddExerciseModal;
    window.addExerciseToWorkout = addExerciseToWorkout;
    window.addSet = addSet;
    window.addDropSet = addDropSet;
    window.removeSet = removeSet;
    window.removeExercise = removeExercise;
    window.finishWorkout = finishWorkout;
    window.generateUUID = generateUUID;
})();
