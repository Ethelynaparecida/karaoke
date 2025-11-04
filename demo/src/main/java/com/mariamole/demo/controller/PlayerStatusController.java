package com.mariamole.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mariamole.demo.service.PlayerStateService;

import java.util.Map; 

@RestController
@RequestMapping("/api/player")
@CrossOrigin(origins = "*")
public class PlayerStatusController {

    private final PlayerStateService playerStateService;


    @Autowired
    public PlayerStatusController(PlayerStateService playerStateService) {
        this.playerStateService = playerStateService;
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        return ResponseEntity.ok(Map.of("isPaused", playerStateService.isPaused()));
    }
}
