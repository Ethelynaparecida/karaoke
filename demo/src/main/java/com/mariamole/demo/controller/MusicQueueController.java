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

import jakarta.servlet.http.HttpServletRequest;

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
    public ResponseEntity<?> addSong(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        
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

        String ipUsuario = request.getHeader("X-Forwarded-For");
        if (ipUsuario == null || ipUsuario.isEmpty() || "unknown".equalsIgnoreCase(ipUsuario)) {
            ipUsuario = request.getRemoteAddr();
        }
        if (ipUsuario != null && ipUsuario.contains(",")) {
            ipUsuario = ipUsuario.split(",")[0].trim();
        }
        
        int position = musicQueueService.addSong(telefone, videoId, titulo, nome, ipUsuario);
        
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

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelSong(@RequestBody Map<String, String> payload) {
        String telefone = payload.get("telefone");
        String mensagem = payload.get("mensagem");

        if (telefone == null) {
            return ResponseEntity.badRequest().body("O telefone do utilizador é obrigatório.");
        }

        boolean cancelada = musicQueueService.cancelarMusica(telefone, mensagem);
        
        if (cancelada) {
            return ResponseEntity.ok(Map.of("message", "Música cancelada e utilizador notificado."));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("erro", "Nenhuma música pendente encontrada para este utilizador."));
        }
    }

    @GetMapping("/cancel-status/{telefone}")
    public ResponseEntity<?> checkCancelStatus(@PathVariable String telefone) {
        String mensagem = musicQueueService.checarMensagemCancelamento(telefone);
        
        if (mensagem != null) {
            return ResponseEntity.ok(Map.of("cancelada", true, "mensagem", mensagem));
        }
        return ResponseEntity.ok(Map.of("cancelada", false));
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

    @GetMapping("/count")
    public ResponseEntity<Integer> getQueueSize() { 
        int queueSize = musicQueueService.getQueueSize();
        return ResponseEntity.ok(queueSize);
    }

    @PostMapping("/error/notify")
    public ResponseEntity<?> notifyVideoError(@RequestBody Map<String, String> payload) {
        String videoId = payload.get("videoId");
        String url = payload.get("url");
        String message = payload.get("message");
        String userName = payload.get("userName");
         System.out.println("userName ** " +userName);
        
        
        // Chama o serviço para guardar o erro
       musicQueueService.reportVideoError(videoId, url, message, userName);
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/error/status")
    public ResponseEntity<Map<String, String>> getErrorStatus() {
        return ResponseEntity.ok(musicQueueService.getErrorStatus());
    }

    @PostMapping("/error/clear")
    public ResponseEntity<?> clearVideoError() {
        // Chama o serviço para pular e limpar
        boolean resolved = musicQueueService.resolveErrorAndSkip();
        
        if (resolved) {
            return ResponseEntity.ok().body(Map.of("message", "Erro limpo e música pulada."));
        } else {
            return ResponseEntity.ok().body(Map.of("message", "Erro limpo, mas não havia música para pular."));
        }
    }
}