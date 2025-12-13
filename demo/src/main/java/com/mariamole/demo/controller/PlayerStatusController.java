package com.mariamole.demo.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mariamole.demo.service.MusicQueueService; 
import com.mariamole.demo.service.PlayerStateService;

@RestController
@RequestMapping("/api/player")
@CrossOrigin(origins = "*")
public class PlayerStatusController {

    private final PlayerStateService playerStateService;
    private MusicQueueService musicQueueService;


    @Autowired
    public PlayerStatusController(PlayerStateService playerStateService, MusicQueueService musicQueueService) {
        this.playerStateService = playerStateService;
        this.musicQueueService = musicQueueService;
    }
    

   @GetMapping("/status")
    public ResponseEntity<PlayerStateService.PlayerStatus> getStatus() {

        PlayerStateService.PlayerStatus status = playerStateService.getStatus();
        Map<String, String> errorData = musicQueueService.getErrorStatus();

        status.errorVideoId = errorData.get("errorVideoId");
        status.errorVideoUrl = errorData.get("errorVideoUrl");
        status.errorMessage = errorData.get("errorMessage");
        status.errorUserName = errorData.get("errorUserName");

        return ResponseEntity.ok(status);
    }
}
