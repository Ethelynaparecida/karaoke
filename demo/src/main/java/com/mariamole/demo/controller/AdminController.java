package com.mariamole.demo.controller;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mariamole.demo.model.HistoricoMusica;
import com.mariamole.demo.model.MusicaFila;
import com.mariamole.demo.repository.HistoricoMusicaRepository;
import com.mariamole.demo.service.PlayerStateService;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final HistoricoMusicaRepository historicoRepository;
    private final PlayerStateService playerStateService;
    private final MusicQueueController musicQueueController;

    @Autowired
    public AdminController(HistoricoMusicaRepository historicoRepository, 
                           PlayerStateService playerStateService,
                           MusicQueueController musicQueueController) {
        this.historicoRepository = historicoRepository;
        this.playerStateService = playerStateService;
        this.musicQueueController = musicQueueController;
    }

    @GetMapping("/log/dia")
    public ResponseEntity<List<HistoricoMusica>> getLogDoDia() {
        LocalDateTime inicioDoDia = LocalDate.now().atStartOfDay();
        LocalDateTime fimDoDia = inicioDoDia.plusDays(1).minusNanos(1); 
        List<HistoricoMusica> log = historicoRepository.findByHorarioCadastroBetween(inicioDoDia, fimDoDia);
        
        return ResponseEntity.ok(log);
    }

    @PostMapping("/player/skip")
    public ResponseEntity<?> skipSong() {
        boolean skipped = musicQueueController.pularMusicaAtual();
        
        if (skipped) {
            playerStateService.skip();
            return ResponseEntity.ok().body(Map.of("message", "Música pulada."));
        } else {
            return ResponseEntity.status(404).body(Map.of("message", "Nenhuma música a tocar para pular."));
        }
    }

    @GetMapping("/queue/view")
    public ResponseEntity<List<MusicaFila>> getQueueView() {
        List<MusicaFila> fila = musicQueueController.getSnapshotDaFila();
        return ResponseEntity.ok(fila);
    }


    @PostMapping("/player/pause")
    public ResponseEntity<?> pausePlayer() {
        playerStateService.pause();
        System.out.println("ADMIN: Player PAUSADO");
        return ResponseEntity.ok().build();
    }


    @PostMapping("/player/play")
    public ResponseEntity<?> playPlayer() {
        playerStateService.play();
        System.out.println("ADMIN: Player a TOCAR (play)");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/player/restart")
    public ResponseEntity<?> restartSong() {
        playerStateService.restart();
        System.out.println("ADMIN: Comando 'Recomeçar' enviado.");
        return ResponseEntity.ok().body(Map.of("message", "Música reiniciada."));
    }
}
