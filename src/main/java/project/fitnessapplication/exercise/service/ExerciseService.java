package project.fitnessapplication.exercise.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.fitnessapplication.exercise.model.Exercise;
import project.fitnessapplication.exercise.repository.ExerciseRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExerciseService {
    private final ExerciseRepository repo;

    public List<Exercise> byOwner(UUID owner){ return repo.findAllByOwnerUserId(owner); }

    @Transactional public Exercise create(Exercise e){ return repo.save(e); }

    public Exercise get(UUID id){ return repo.findById(id).orElseThrow(); }

    @Transactional public void delete(UUID id){ repo.deleteById(id); }


}
