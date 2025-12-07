package project.fitnessapplicationexam.template;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import project.fitnessapplicationexam.template.dto.TemplateItemData;
import project.fitnessapplicationexam.template.model.TemplateItem;
import project.fitnessapplicationexam.template.model.WorkoutTemplate;
import project.fitnessapplicationexam.template.repository.TemplateItemRepository;
import project.fitnessapplicationexam.template.repository.WorkoutTemplateRepository;
import project.fitnessapplicationexam.template.service.TemplateService;
import project.fitnessapplicationexam.exercise.model.Exercise;
import project.fitnessapplicationexam.exercise.repository.ExerciseRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

	@Mock
	private WorkoutTemplateRepository workoutTemplateRepository;
	@Mock
	private TemplateItemRepository templateItemRepository;
	@Mock
	private ExerciseRepository exerciseRepository;

	@InjectMocks
	private TemplateService templateService;

	@Test
	void createTemplate_success() {
		UUID owner = UUID.randomUUID();
		UUID exerciseId = UUID.randomUUID();
		when(workoutTemplateRepository.existsByOwnerUserIdAndNameIgnoreCase(eq(owner), eq("Push"))).thenReturn(false);
		when(workoutTemplateRepository.save(any(WorkoutTemplate.class))).thenAnswer(inv -> {
			WorkoutTemplate t = inv.getArgument(0);
			t.setId(UUID.randomUUID());
			return t;
		});
		when(exerciseRepository.findAllById(anyList())).thenReturn(List.of(Exercise.builder()
				.id(exerciseId)
				.ownerUserId(owner)
				.build()));

		TemplateItemData item = new TemplateItemData(exerciseId, 3, 0, null, null, null, null);
		WorkoutTemplate result = templateService.createTemplate(owner, "Push", List.of(item));
		assertNotNull(result);
		verify(templateItemRepository).saveAll(anyList());
	}

	@Test
	void createTemplate_emptyName_throwsException() {
		UUID exerciseId = UUID.randomUUID();
		TemplateItemData item = new TemplateItemData(exerciseId, 3, 0, null, null, null, null);
		assertThrows(IllegalArgumentException.class, () -> templateService.createTemplate(UUID.randomUUID(), "  ", List.of(item)));
	}

	@Test
	void createTemplate_duplicateName_throwsException() {
		UUID owner = UUID.randomUUID();
		when(workoutTemplateRepository.existsByOwnerUserIdAndNameIgnoreCase(owner, "Existing")).thenReturn(true);

		UUID exerciseId = UUID.randomUUID();
		TemplateItemData item = new TemplateItemData(exerciseId, 3, 0, null, null, null, null);
		assertThrows(IllegalArgumentException.class, () -> templateService.createTemplate(owner, "Existing", List.of(item)));
	}

	@Test
	void createTemplate_noExercises_throwsException() {
		UUID owner = UUID.randomUUID();
		when(workoutTemplateRepository.existsByOwnerUserIdAndNameIgnoreCase(owner, "Empty")).thenReturn(false);

		assertThrows(IllegalArgumentException.class, () -> templateService.createTemplate(owner, "Empty", List.of()));
	}

	@Test
	void createTemplate_exerciseNotAllowed_throwsException() {
		UUID owner = UUID.randomUUID();
		UUID otherOwner = UUID.randomUUID();
		UUID exerciseId = UUID.randomUUID();
		when(workoutTemplateRepository.existsByOwnerUserIdAndNameIgnoreCase(owner, "Test")).thenReturn(false);
		when(workoutTemplateRepository.save(any(WorkoutTemplate.class))).thenAnswer(inv -> {
			WorkoutTemplate t = inv.getArgument(0);
			t.setId(UUID.randomUUID());
			return t;
		});
		when(exerciseRepository.findAllById(anyList())).thenReturn(List.of(Exercise.builder()
				.id(exerciseId)
				.ownerUserId(otherOwner)
				.build()));

		TemplateItemData item = new TemplateItemData(exerciseId, 3, 0, null, null, null, null);
		assertThrows(IllegalArgumentException.class, () -> templateService.createTemplate(owner, "Test", List.of(item)));
	}

	@Test
	void list_returnsTemplates() {
		UUID owner = UUID.randomUUID();
		when(workoutTemplateRepository.findAllByOwnerUserIdOrderByCreatedOnDesc(owner)).thenReturn(List.of());

		List<WorkoutTemplate> result = templateService.list(owner);
		assertNotNull(result);
	}

	@Test
	void getAvailableExercises_returnsExercises() {
		UUID userId = UUID.randomUUID();
		when(exerciseRepository.findAllByOwnerUserIdInOrderByNameAsc(anyList())).thenReturn(List.of());

		List<Exercise> result = templateService.getAvailableExercises(userId);
		assertNotNull(result);
	}

	@Test
	void findByIdAndOwner_returnsTemplate() {
		UUID templateId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		when(workoutTemplateRepository.findByIdAndOwnerUserId(templateId, ownerId)).thenReturn(Optional.empty());

		Optional<WorkoutTemplate> result = templateService.findByIdAndOwner(templateId, ownerId);
		assertTrue(result.isEmpty());
	}

	@Test
	void getTemplateItems_returnsItems() {
		UUID templateId = UUID.randomUUID();
		when(templateItemRepository.findAllByTemplateIdOrderByPositionAsc(templateId)).thenReturn(List.of());

		List<TemplateItem> result = templateService.getTemplateItems(templateId);
		assertNotNull(result);
	}

	@Test
	void getExercisesByIds_returnsMap() {
		UUID exerciseId = UUID.randomUUID();
		Exercise exercise = Exercise.builder().id(exerciseId).build();
		when(exerciseRepository.findAllById(anyList())).thenReturn(List.of(exercise));

		Map<UUID, Exercise> result = templateService.getExercisesByIds(List.of(exerciseId));
		assertEquals(1, result.size());
	}

	@Test
	void createTemplate_createsTemplate() {
		UUID ownerId = UUID.randomUUID();
		UUID exerciseId = UUID.randomUUID();
		WorkoutTemplate saved = new WorkoutTemplate();
		saved.setId(UUID.randomUUID());
		when(workoutTemplateRepository.existsByOwnerUserIdAndNameIgnoreCase(ownerId, "Test")).thenReturn(false);
		when(workoutTemplateRepository.save(any(WorkoutTemplate.class))).thenReturn(saved);
		when(exerciseRepository.findAllById(anyList())).thenReturn(List.of(Exercise.builder()
				.id(exerciseId)
				.ownerUserId(ownerId)
				.build()));

		TemplateItemData item = new TemplateItemData(exerciseId, 3, 0, null, null, null, null);
		WorkoutTemplate result = templateService.createTemplate(ownerId, "Test", List.of(item));
		assertNotNull(result);
	}

	@Test
	void createTemplate_withItems_createsTemplate() {
		UUID ownerId = UUID.randomUUID();
		UUID templateId = UUID.randomUUID();
		UUID exerciseId = UUID.randomUUID();
		
		WorkoutTemplate saved = new WorkoutTemplate();
		saved.setId(templateId);
		when(workoutTemplateRepository.save(any(WorkoutTemplate.class))).thenReturn(saved);

		TemplateItemData item = new TemplateItemData(exerciseId, 3, 0, null, null, null, null);
		when(exerciseRepository.findAllById(any())).thenReturn(List.of(Exercise.builder()
				.id(exerciseId)
				.ownerUserId(ownerId)
				.build()));
		WorkoutTemplate result = templateService.createTemplate(ownerId, "Test", List.of(item));
		assertNotNull(result);
		verify(templateItemRepository).saveAll(anyList());
	}

	@Test
	void deleteTemplate_deletesTemplate() {
		UUID templateId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		WorkoutTemplate template = new WorkoutTemplate();
		template.setId(templateId);
		when(workoutTemplateRepository.findByIdAndOwnerUserId(templateId, ownerId)).thenReturn(Optional.of(template));

		templateService.deleteTemplate(templateId, ownerId);
		verify(workoutTemplateRepository).delete(template);
		verify(templateItemRepository).deleteByTemplateId(templateId);
	}

	@Test
	void deleteTemplate_notFound_doesNothing() {
		UUID templateId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		when(workoutTemplateRepository.findByIdAndOwnerUserId(templateId, ownerId)).thenReturn(Optional.empty());

		templateService.deleteTemplate(templateId, ownerId);
		verify(workoutTemplateRepository, never()).delete(any());
	}

	@Test
	void updateTemplate_updatesTemplate() {
		UUID templateId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		WorkoutTemplate template = new WorkoutTemplate();
		template.setId(templateId);
		when(workoutTemplateRepository.findByIdAndOwnerUserId(templateId, ownerId)).thenReturn(Optional.of(template));
		when(workoutTemplateRepository.save(any(WorkoutTemplate.class))).thenReturn(template);

		templateService.updateTemplate(templateId, ownerId, "New Name", List.of());
		verify(workoutTemplateRepository).save(any());
	}

	@Test
	void updateTemplate_withItems_updatesTemplate() {
		UUID templateId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		UUID exerciseId = UUID.randomUUID();
		WorkoutTemplate template = new WorkoutTemplate();
		template.setId(templateId);
		when(workoutTemplateRepository.findByIdAndOwnerUserId(templateId, ownerId)).thenReturn(Optional.of(template));
		when(workoutTemplateRepository.save(any(WorkoutTemplate.class))).thenReturn(template);
		when(exerciseRepository.findAllById(any())).thenReturn(List.of(Exercise.builder()
				.id(exerciseId)
				.ownerUserId(ownerId)
				.build()));

		TemplateItemData item = new TemplateItemData(exerciseId, 3, 0, null, null, null, null);
		templateService.updateTemplate(templateId, ownerId, "New Name", List.of(item));
		verify(templateItemRepository).deleteByTemplateId(templateId);
		verify(templateItemRepository).saveAll(anyList());
	}

	@Test
	void updateTemplate_notFound_throwsException() {
		UUID templateId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		when(workoutTemplateRepository.findByIdAndOwnerUserId(templateId, ownerId)).thenReturn(Optional.empty());

		assertThrows(ResponseStatusException.class, () -> templateService.updateTemplate(templateId, ownerId, "Name", List.of()));
	}

	@Test
	void isNameTaken_returnsTrue() {
		UUID ownerId = UUID.randomUUID();
		when(workoutTemplateRepository.existsByOwnerUserIdAndNameIgnoreCase(ownerId, "Existing")).thenReturn(true);

		assertTrue(templateService.isNameTaken(ownerId, "Existing"));
	}

	@Test
	void isNameTaken_returnsFalse() {
		UUID ownerId = UUID.randomUUID();
		when(workoutTemplateRepository.existsByOwnerUserIdAndNameIgnoreCase(ownerId, "New")).thenReturn(false);

		assertFalse(templateService.isNameTaken(ownerId, "New"));
	}
}
