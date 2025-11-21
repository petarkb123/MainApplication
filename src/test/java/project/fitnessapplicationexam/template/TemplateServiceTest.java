package project.fitnessapplicationexam.template;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import project.fitnessapplicationexam.template.dto.TemplateItemData;
import project.fitnessapplicationexam.template.form.TemplateForm;
import project.fitnessapplicationexam.template.form.TemplateItemForm;
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
	private WorkoutTemplateRepository templateRepo;
	@Mock
	private TemplateItemRepository itemRepo;
	@Mock
	private ExerciseRepository exerciseRepo;

	@InjectMocks
	private TemplateService service;

	@Test
	void create_success() {
		UUID owner = UUID.randomUUID();
		when(templateRepo.existsByOwnerUserIdAndNameIgnoreCase(eq(owner), eq("Push"))).thenReturn(false);
		when(templateRepo.save(any(WorkoutTemplate.class))).thenAnswer(inv -> {
			WorkoutTemplate t = inv.getArgument(0);
			t.setId(UUID.randomUUID());
			return t;
		});
		when(exerciseRepo.findById(any(UUID.class))).thenReturn(Optional.of(Exercise.builder().ownerUserId(owner).build()));

		TemplateItemForm item = new TemplateItemForm();
		item.setExerciseId(UUID.randomUUID());
		item.setSets(3);
		TemplateForm form = new TemplateForm();
		form.setName("Push");
		form.setItems(List.of(item));

		UUID id = service.create(owner, form);
		assertNotNull(id);
		verify(itemRepo).saveAll(anyList());
	}

	@Test
	void create_emptyName_throwsException() {
		TemplateForm form = new TemplateForm();
		form.setName("  ");

		assertThrows(IllegalArgumentException.class, () -> service.create(UUID.randomUUID(), form));
	}

	@Test
	void create_duplicateName_throwsException() {
		UUID owner = UUID.randomUUID();
		when(templateRepo.existsByOwnerUserIdAndNameIgnoreCase(owner, "Existing")).thenReturn(true);

		TemplateForm form = new TemplateForm();
		form.setName("Existing");

		assertThrows(IllegalArgumentException.class, () -> service.create(owner, form));
	}

	@Test
	void create_noExercises_throwsException() {
		UUID owner = UUID.randomUUID();
		when(templateRepo.existsByOwnerUserIdAndNameIgnoreCase(owner, "Empty")).thenReturn(false);
		when(templateRepo.save(any(WorkoutTemplate.class))).thenAnswer(inv -> {
			WorkoutTemplate t = inv.getArgument(0);
			t.setId(UUID.randomUUID());
			return t;
		});

		TemplateForm form = new TemplateForm();
		form.setName("Empty");
		form.setItems(List.of());

		assertThrows(IllegalArgumentException.class, () -> service.create(owner, form));
	}

	@Test
	void create_exerciseNotAllowed_throwsException() {
		UUID owner = UUID.randomUUID();
		UUID otherOwner = UUID.randomUUID();
		when(templateRepo.existsByOwnerUserIdAndNameIgnoreCase(owner, "Test")).thenReturn(false);
		when(templateRepo.save(any(WorkoutTemplate.class))).thenAnswer(inv -> {
			WorkoutTemplate t = inv.getArgument(0);
			t.setId(UUID.randomUUID());
			return t;
		});
		when(exerciseRepo.findById(any(UUID.class))).thenReturn(Optional.of(Exercise.builder().ownerUserId(otherOwner).build()));

		TemplateItemForm item = new TemplateItemForm();
		item.setExerciseId(UUID.randomUUID());
		TemplateForm form = new TemplateForm();
		form.setName("Test");
		form.setItems(List.of(item));

		assertThrows(IllegalArgumentException.class, () -> service.create(owner, form));
	}

	@Test
	void list_returnsTemplates() {
		UUID owner = UUID.randomUUID();
		when(templateRepo.findAllByOwnerUserIdOrderByCreatedOnDesc(owner)).thenReturn(List.of());

		List<WorkoutTemplate> result = service.list(owner);
		assertNotNull(result);
	}

	@Test
	void getAvailableExercises_returnsExercises() {
		UUID userId = UUID.randomUUID();
		when(exerciseRepo.findAllByOwnerUserIdInOrderByNameAsc(anyList())).thenReturn(List.of());

		List<Exercise> result = service.getAvailableExercises(userId);
		assertNotNull(result);
	}

	@Test
	void findByIdAndOwner_returnsTemplate() {
		UUID templateId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		when(templateRepo.findByIdAndOwnerUserId(templateId, ownerId)).thenReturn(Optional.empty());

		Optional<WorkoutTemplate> result = service.findByIdAndOwner(templateId, ownerId);
		assertTrue(result.isEmpty());
	}

	@Test
	void getTemplateItems_returnsItems() {
		UUID templateId = UUID.randomUUID();
		when(itemRepo.findAllByTemplateIdOrderByPositionAsc(templateId)).thenReturn(List.of());

		List<TemplateItem> result = service.getTemplateItems(templateId);
		assertNotNull(result);
	}

	@Test
	void getExercisesByIds_returnsMap() {
		UUID exerciseId = UUID.randomUUID();
		Exercise exercise = Exercise.builder().id(exerciseId).build();
		when(exerciseRepo.findAllById(anyList())).thenReturn(List.of(exercise));

		Map<UUID, Exercise> result = service.getExercisesByIds(List.of(exerciseId));
		assertEquals(1, result.size());
	}

	@Test
	void createTemplate_createsTemplate() {
		UUID ownerId = UUID.randomUUID();
		WorkoutTemplate saved = new WorkoutTemplate();
		saved.setId(UUID.randomUUID());
		when(templateRepo.save(any(WorkoutTemplate.class))).thenReturn(saved);

		WorkoutTemplate result = service.createTemplate(ownerId, "Test", null);
		assertNotNull(result);
	}

	@Test
	void createTemplate_withItems_createsTemplate() {
		UUID ownerId = UUID.randomUUID();
		UUID templateId = UUID.randomUUID();
		UUID exerciseId = UUID.randomUUID();
		
		WorkoutTemplate saved = new WorkoutTemplate();
		saved.setId(templateId);
		when(templateRepo.save(any(WorkoutTemplate.class))).thenReturn(saved);

		TemplateItemData item = new TemplateItemData(exerciseId, 3, 0, null, null, null, null);
		when(exerciseRepo.findAllById(any())).thenReturn(List.of(Exercise.builder()
				.id(exerciseId)
				.ownerUserId(ownerId)
				.build()));
		WorkoutTemplate result = service.createTemplate(ownerId, "Test", List.of(item));
		assertNotNull(result);
		verify(itemRepo).saveAll(anyList());
	}

	@Test
	void deleteTemplate_deletesTemplate() {
		UUID templateId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		WorkoutTemplate template = new WorkoutTemplate();
		template.setId(templateId);
		when(templateRepo.findByIdAndOwnerUserId(templateId, ownerId)).thenReturn(Optional.of(template));

		service.deleteTemplate(templateId, ownerId);
		verify(templateRepo).delete(template);
		verify(itemRepo).deleteByTemplateId(templateId);
	}

	@Test
	void deleteTemplate_notFound_doesNothing() {
		UUID templateId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		when(templateRepo.findByIdAndOwnerUserId(templateId, ownerId)).thenReturn(Optional.empty());

		service.deleteTemplate(templateId, ownerId);
		verify(templateRepo, never()).delete(any());
	}

	@Test
	void updateTemplate_updatesTemplate() {
		UUID templateId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		WorkoutTemplate template = new WorkoutTemplate();
		template.setId(templateId);
		when(templateRepo.findByIdAndOwnerUserId(templateId, ownerId)).thenReturn(Optional.of(template));
		when(templateRepo.save(any(WorkoutTemplate.class))).thenReturn(template);

		service.updateTemplate(templateId, ownerId, "New Name", List.of());
		verify(templateRepo).save(any());
	}

	@Test
	void updateTemplate_withItems_updatesTemplate() {
		UUID templateId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		UUID exerciseId = UUID.randomUUID();
		WorkoutTemplate template = new WorkoutTemplate();
		template.setId(templateId);
		when(templateRepo.findByIdAndOwnerUserId(templateId, ownerId)).thenReturn(Optional.of(template));
		when(templateRepo.save(any(WorkoutTemplate.class))).thenReturn(template);
		when(exerciseRepo.findAllById(any())).thenReturn(List.of(Exercise.builder()
				.id(exerciseId)
				.ownerUserId(ownerId)
				.build()));

		TemplateItemData item = new TemplateItemData(exerciseId, 3, 0, null, null, null, null);
		service.updateTemplate(templateId, ownerId, "New Name", List.of(item));
		verify(itemRepo).deleteByTemplateId(templateId);
		verify(itemRepo).saveAll(anyList());
	}

	@Test
	void updateTemplate_notFound_throwsException() {
		UUID templateId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		when(templateRepo.findByIdAndOwnerUserId(templateId, ownerId)).thenReturn(Optional.empty());

		assertThrows(ResponseStatusException.class, () -> service.updateTemplate(templateId, ownerId, "Name", List.of()));
	}

	@Test
	void isNameTaken_returnsTrue() {
		UUID ownerId = UUID.randomUUID();
		when(templateRepo.existsByOwnerUserIdAndNameIgnoreCase(ownerId, "Existing")).thenReturn(true);

		assertTrue(service.isNameTaken(ownerId, "Existing"));
	}

	@Test
	void isNameTaken_returnsFalse() {
		UUID ownerId = UUID.randomUUID();
		when(templateRepo.existsByOwnerUserIdAndNameIgnoreCase(ownerId, "New")).thenReturn(false);

		assertFalse(service.isNameTaken(ownerId, "New"));
	}
}
