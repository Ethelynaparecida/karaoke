package com.mariamole.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mariamole.demo.model.MusicaFila;
import com.mariamole.demo.service.MusicQueueService;
import com.mariamole.demo.service.PlayerStateService;

@RestController
@RequestMapping("/api/queue")
@CrossOrigin(origins = "*")
public class MusicQueueController {

    private final MusicQueueService musicQueueService;
    private final PlayerStateService playerStateService; // Injetado para verificação de bloqueio

    @Autowired
    public MusicQueueController(MusicQueueService musicQueueService, 
                                PlayerStateService playerStateService) {
        this.musicQueueService = musicQueueService;
        this.playerStateService = playerStateService;
    }

    
    @PostMapping("/add")
    public ResponseEntity<?> addSong(@RequestBody Map<String, String> payload) {
        
        if (playerStateService.isQueueLocked()) {
            return ResponseEntity.status(HttpStatus.LOCKED).body("A fila está temporariamente fechada pelo admin.");
        }

        String telefone = payload.get("telefone");
        String videoId = payload.get("videoId");
        String titulo = payload.get("titulo");
        String nome = payload.get("nome");

        if (telefone == null || videoId == null || nome == null || titulo == null) {
            return ResponseEntity.badRequest().body("Dados incompletos.");
        }
        
        int position = musicQueueService.addSong(telefone, videoId, titulo, nome);
        
        if (position == -2) { // -2 significa que o utilizador já tem música
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Utilizador já tem uma música na fila.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Música adicionada!");
        response.put("position", position);
        
        return ResponseEntity.ok(response);
    }

 
    @GetMapping("/next")
    public ResponseEntity<?> getNextSong() {
        Optional<MusicaFila> nextSongOpt = musicQueueService.getNextSong();
        
        if (nextSongOpt.isPresent()) {
            return ResponseEntity.ok(nextSongOpt.get()); 
        }
        return ResponseEntity.ok().build(); // Fila vazia
    }

  
    @PostMapping("/complete")
    public ResponseEntity<?> completeSong(@RequestBody Map<String, String> payload) {
        String videoId = payload.get("videoId");
        if (videoId == null) {
            return ResponseEntity.badRequest().body("Missing videoId");
        }
        
        musicQueueService.completeSong(videoId);
        return ResponseEntity.ok().build();
    }
  
    @PostMapping("/remove-by-user/{userId}")
    public ResponseEntity<?> removeUserSongOnLogout(@PathVariable String userId) {
        musicQueueService.removeSongByUserId(userId);
        return ResponseEntity.ok().body(Map.of("message", "Comando de remoção de música enviado."));
    }


    @GetMapping("/position/{telefone}")
    public ResponseEntity<?> getPosition(@PathVariable String telefone) {
        int position = musicQueueService.getPosicaoPorTelefone(telefone);
        
        Map<String, Object> response = new HashMap<>();
        response.put("position", position);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<MusicaFila>> getAllSongs() {
        List<MusicaFila> allSongs = musicQueueService.getAllSongsInQueue();
        return ResponseEntity.ok(allSongs);
    }
}