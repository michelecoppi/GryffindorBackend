package com.gryffindor.SQStepByStep.controller;

import com.gryffindor.SQStepByStep.dto.CigaretteDto;
import com.gryffindor.SQStepByStep.dto.TimerDto;
import com.gryffindor.SQStepByStep.model.Cigarette;
import com.gryffindor.SQStepByStep.model.Timer;
import com.gryffindor.SQStepByStep.model.User;
import com.gryffindor.SQStepByStep.model.UserPrincipal;
import com.gryffindor.SQStepByStep.model.service.abstraction.HistoryService;
import com.gryffindor.SQStepByStep.model.service.abstraction.AbstractUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final AbstractUserService userService;

    @Autowired
    public PlanningController(HistoryService historyService, AbstractUserService userService) {
        this.historyService = historyService;
        this.userService = userService;
    }

    @GetMapping("/users/activetimer")
    public ResponseEntity<?> getActiveTimerForLoggedUser(@AuthenticationPrincipal UserPrincipal userDetails) {
     return getLatestTimerByUserId(userDetails.getUser().getId());
    }
//ONLY FOR TESTS
    @GetMapping("/users/{id}/activetimer")
    public ResponseEntity<?> getActiveTimerByUserId(@PathVariable("id") int userId) {
       return getLatestTimerByUserId(userId);
    }

    private ResponseEntity<?> getLatestTimerByUserId(int userId){
        Optional<User> user = userService.getUserById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Non esiste un utente con questo id "+userId);
        }
        Optional<Timer> latestTimer = historyService.getLatestTimerByUser(user.get());
        if(latestTimer.isPresent()){
            return ResponseEntity.ok(new TimerDto(latestTimer.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Non esistono sigarette per questo utente");
    }


    @GetMapping("/users/activecigarette")
    public ResponseEntity<?> getActiveCigaretteForLoggedUser(@AuthenticationPrincipal UserPrincipal userDetails) {
        return getLatestCigaretteByUserId(userDetails.getUser().getId());
    }
//ONLY FOR TESTS
    @GetMapping("/users/{id}/activecigarette")
    public ResponseEntity<?> getActiveCigaretteByUserId( @PathVariable("id") int userId) {
        return getLatestCigaretteByUserId(userId);
    }

    private ResponseEntity<?> getLatestCigaretteByUserId(int userId){
        Optional<User> user = userService.getUserById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Non esiste un utente con questo id "+userId);
        }
        Optional<Cigarette> latestCigarette = historyService.getLatestCigaretteByUser(user.get());
        if(latestCigarette.isPresent()){
            return ResponseEntity.ok(new CigaretteDto(latestCigarette.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Non esistono sigarette per questo utente");
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

    @GetMapping("/users/savings")
    public ResponseEntity<?> calculateSavingsForLoggedUser(@AuthenticationPrincipal UserPrincipal userDetails) {
        return getSavingsByUserId(userDetails.getUser().getId());
    }

//ONLY FOR TESTS
    @GetMapping("/users/{id}/savings")
    public ResponseEntity<?> calculateSavingsByUserId(@PathVariable("id") int userId) {
        return getSavingsByUserId(userId);
    }

    private ResponseEntity<?> getSavingsByUserId (int userId){
        Optional<User> user = userService.getUserById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Non esiste un utente con questo id "+userId);
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
