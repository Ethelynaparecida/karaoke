package com.mariamole.demo.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mariamole.demo.model.HistoricoMusica;
import com.mariamole.demo.model.MusicaFila;
import com.mariamole.demo.model.Usuario;
import com.mariamole.demo.repository.HistoricoMusicaRepository;
import com.mariamole.demo.repository.UsuarioRepository;
import com.mariamole.demo.service.PlayerStateService;
import com.mariamole.demo.service.YouTubeService;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final HistoricoMusicaRepository historicoRepository;
    private final PlayerStateService playerStateService;
    private final MusicQueueController musicQueueController;
    private final UsuarioRepository usuarioRepository;
    private final YouTubeService youTubeService;

    @Autowired
    public AdminController(HistoricoMusicaRepository historicoRepository, 
                           PlayerStateService playerStateService,
                           MusicQueueController musicQueueController,
                           UsuarioRepository usuarioRepository,
                           YouTubeService youTubeService) {
        this.historicoRepository = historicoRepository;
        this.playerStateService = playerStateService;
        this.musicQueueController = musicQueueController;
        this.usuarioRepository = usuarioRepository;
        this.youTubeService = youTubeService;
    }

    @GetMapping("/quota")
    public ResponseEntity<?> getQuotaUsage() {
        long usage = youTubeService.getCurrentQuotaUsage();
        
        int numberOfKeys = youTubeService.getNumberOfKeys();
       
        long limit = 10000 * numberOfKeys; 

        long remainingUnits = (limit > usage) ? (limit - usage) : 0;
        long searchesLeft = remainingUnits / 100; 

        return ResponseEntity.ok(Map.of(
            "unidadesUsadas", usage,
            "limiteDiario", limit, 
            "buscasRestantesEstimadas", searchesLeft 
        ));
    }

    @GetMapping("/log/dia")
    public ResponseEntity<List<HistoricoMusica>> getLogDoDia() {
        LocalDateTime fim = LocalDateTime.now();
        LocalDateTime inicio = fim.minusHours(4);
       List<HistoricoMusica> log = historicoRepository.findByHorarioCadastroBetweenOrderByHorarioCadastroDesc(inicio, fim);

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

    @GetMapping("/users")
    public ResponseEntity<List<Usuario>> getUsuariosRegistados() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        return ResponseEntity.ok(usuarios);
    }

    @PostMapping("/queue/add")
    public ResponseEntity<?> adminAddSong(@RequestBody Map<String, String> payload) {

        String videoId = payload.get("videoId");
        String titulo = payload.get("titulo");
        String nome = payload.get("nome");

        if (nome == null || videoId == null || titulo == null) {
            return ResponseEntity.badRequest().body("Dados incompletos (nome, videoId ou titulo em falta).");
        }

        String telefoneAdmin = "admin-" + nome;

        MusicaFila novaMusica = new MusicaFila(videoId, titulo, nome, telefoneAdmin);

        try {
            HistoricoMusica historico = new HistoricoMusica(nome, telefoneAdmin, videoId, titulo, LocalDateTime.now());
            historicoRepository.save(historico);
        } catch (Exception e) {
            System.err.println("Falha ao salvar no histórico (Admin Add): " + e.getMessage());
        }

        musicQueueController.adicionarMusicaComoAdmin(novaMusica);
        return ResponseEntity.ok(Map.of("message", "Música adicionada pelo admin."));
    }

    @PostMapping("/queue/lock")
    public ResponseEntity<?> lockQueue() {
        playerStateService.lockQueue();
        System.out.println("ADMIN: Fila de músicas BLOQUEADA");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/queue/unlock")
    public ResponseEntity<?> unlockQueue() {
        playerStateService.unlockQueue();
        System.out.println("ADMIN: Fila de músicas DESBLOQUEADA");
        return ResponseEntity.ok().build();
    }
}
