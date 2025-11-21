package project.fitnessapplication.web;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import project.fitnessapplication.schedule.dto.CreateScheduledWorkoutRequest;
import project.fitnessapplication.schedule.dto.ScheduledWorkoutDto;
import project.fitnessapplication.schedule.service.ScheduleService;
import project.fitnessapplication.user.service.UserService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleApiController {

    private final ScheduleService service;
    private final UserService users;

    @PostMapping
    public ScheduledWorkoutDto create(@AuthenticationPrincipal UserDetails me,
                                      @RequestBody CreateScheduledWorkoutRequest req) {
        UUID userId = users.findByUsernameOrThrow(me.getUsername()).getId();
        return service.create(userId, req);
    }

    @GetMapping
    public List<ScheduledWorkoutDto> list(@AuthenticationPrincipal UserDetails me,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID userId = users.findByUsernameOrThrow(me.getUsername()).getId();
        return service.findInRange(userId, from, to);
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal UserDetails me, @PathVariable UUID id) {
        UUID userId = users.findByUsernameOrThrow(me.getUsername()).getId();
        service.delete(userId, id);
    }
}
