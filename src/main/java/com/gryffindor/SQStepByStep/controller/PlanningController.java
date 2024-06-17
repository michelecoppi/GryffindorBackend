package com.gryffindor.SQStepByStep.controller;

import com.gryffindor.SQStepByStep.dto.CigaretteDto;
import com.gryffindor.SQStepByStep.dto.TimerDto;
import com.gryffindor.SQStepByStep.model.Cigarette;
import com.gryffindor.SQStepByStep.model.Timer;
import com.gryffindor.SQStepByStep.model.User;
import com.gryffindor.SQStepByStep.model.service.abstraction.HistoryService;
import com.gryffindor.SQStepByStep.model.service.abstraction.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/planning")
public class PlanningController {
    private final HistoryService historyService;
    private final UserService userService;

    @Autowired
    public PlanningController(HistoryService historyService, UserService userService) {
        this.historyService = historyService;
        this.userService = userService;
    }

    @GetMapping("/users/{id}/activetimer")
    public ResponseEntity<TimerDto> getActiveTimer(@PathVariable("id") int userId) {
        Optional<User> user = userService.getUserById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Optional<Timer> latestTimer = historyService.getLatestTimerByUser(user.get());
        return latestTimer.map(timer -> ResponseEntity.ok(new TimerDto(timer)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }


    @GetMapping("/users/{id}/activecigarette")
    public ResponseEntity<CigaretteDto> getActiveCigarette(@PathVariable("id") int userId) {
        Optional<User> user = userService.getUserById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Optional<Cigarette> latestCigarette = historyService.getLatestCigaretteByUser(user.get());
        return latestCigarette.map(cigarette -> ResponseEntity.ok(new CigaretteDto(cigarette)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

    @PostMapping("/users/{id}/cigarette")
    public ResponseEntity<CigaretteDto> createCigarette(@RequestBody CigaretteDto dto, @PathVariable("id") int userId) {
        //controllo id query string == id dentro dto
        Optional<User> user = userService.getUserById(dto.getUserId());
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Cigarette c = dto.toCigarette(user.get());
        historyService.createCigarette(c);
        return ResponseEntity.ok(new CigaretteDto(c));
    }

    @PostMapping("/users/{id}/timer")
    public ResponseEntity<TimerDto> createTimer(@RequestBody TimerDto dto, @PathVariable("id") int userId) {
        //controllo id query string == id dentro dto
        Optional<User> user = userService.getUserById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        int duration = (16 * 60) / (user.get().decrementDailyCigNum() - 1);
        LocalDate localDate = LocalDate.parse(dto.getStartDate());
        Timer t = new Timer(localDate, localDate.plusMonths(1), duration, user.get());
        historyService.createTimer(t);
        return ResponseEntity.ok(new TimerDto(t));
    }

    @GetMapping("/users/{id}/saving")
    public ResponseEntity<BigDecimal> calculateSaving(@PathVariable("id") int userId) {
        Optional<User> user = userService.getUserById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        int countCigarette = 0;
        List<Cigarette> userCigarette = user.get().getCigs();
        LocalDateTime today = LocalDate.now().atStartOfDay();
        for (Cigarette cigarette : userCigarette) {
            if(cigarette.getDateTime().isBefore(today)){
                countCigarette++;
            }
        }
        int registeredDays = (int)ChronoUnit.DAYS.between(user.get().getSubscriptionDate(), today);
        int theoreticalCigarettes = registeredDays*user.get().getStartingDailyCigNums();
        BigDecimal saving = user.get().getEachCigPrice().multiply(BigDecimal.valueOf(theoreticalCigarettes - countCigarette));
        return ResponseEntity.ok(saving);
    }




}
