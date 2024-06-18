package com.gryffindor.SQStepByStep.controller;


import com.gryffindor.SQStepByStep.dto.LoginDto;
import com.gryffindor.SQStepByStep.dto.TokenResponseDto;
import com.gryffindor.SQStepByStep.dto.UserDto;
import com.gryffindor.SQStepByStep.model.Timer;
import com.gryffindor.SQStepByStep.model.User;
import com.gryffindor.SQStepByStep.model.service.abstraction.HistoryService;
import com.gryffindor.SQStepByStep.model.service.implementation.JwtService;
import com.gryffindor.SQStepByStep.model.service.implementation.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {


	private final UserService userService;
	private final HistoryService historyService;

	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;

	@Autowired
	public UserController(UserService userService, HistoryService historyService, JwtService jwtService, AuthenticationManager authenticationManager){
		this.userService = userService;
		this.historyService = historyService;
		this.jwtService = jwtService;
		this.authenticationManager = authenticationManager;
	}
	
	@PostMapping("register")
	public ResponseEntity<UserDto> register(@RequestBody UserDto userDto) {
		User saved = userService.saveUser(userDto.toUser());
		UserDto savedDto = new UserDto(saved);
		int duration = (16 * 60) / (saved.getStartingDailyCigNums() - 1);
		historyService.createTimer(new Timer(saved.getSubscriptionDate(),saved.getSubscriptionDate().plusMonths(1), duration,saved));
		return ResponseEntity.ok(savedDto);
	}

	@PostMapping("login")
	public ResponseEntity<TokenResponseDto> login(@RequestBody LoginDto loginDto){

		Authentication authentication = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword()));

		if(authentication.isAuthenticated()) {
			String token = jwtService.generateToken(loginDto.getUsername());
			return ResponseEntity.ok(new TokenResponseDto(token,null));
		}
		else {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new TokenResponseDto(null,"Login Failed")) ;
		}

	}

}
