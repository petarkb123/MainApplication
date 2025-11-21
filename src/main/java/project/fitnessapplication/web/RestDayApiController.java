package project.fitnessapplication.web;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import project.fitnessapplication.restday.dto.CreateRestDayRequest;
import project.fitnessapplication.restday.dto.RestDayDto;
import project.fitnessapplication.restday.service.RestDayService;
import project.fitnessapplication.user.service.UserService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rest-days")
@RequiredArgsConstructor
public class RestDayApiController {

    private final RestDayService restDayService;
    private final UserService users;

    @PostMapping
    public RestDayDto create(@AuthenticationPrincipal UserDetails me, @RequestBody CreateRestDayRequest request) {
        UUID userId = users.findByUsernameOrThrow(me.getUsername()).getId();
        return restDayService.createRestDay(userId, request);
    }

    @GetMapping
    public List<RestDayDto> list(@AuthenticationPrincipal UserDetails me,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID userId = users.findByUsernameOrThrow(me.getUsername()).getId();
        if (from != null && to != null) {
            return restDayService.getRestDaysInRange(userId, from, to);
        }
        return restDayService.getRestDaysByUser(userId);
    }

    @PutMapping("/{id}")
    public RestDayDto update(@AuthenticationPrincipal UserDetails me, @PathVariable UUID id,
                             @RequestBody CreateRestDayRequest request) {
        UUID userId = users.findByUsernameOrThrow(me.getUsername()).getId();
        return restDayService.updateRestDay(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal UserDetails me, @PathVariable UUID id) {
        UUID userId = users.findByUsernameOrThrow(me.getUsername()).getId();
        restDayService.deleteRestDay(userId, id);
    }
}
