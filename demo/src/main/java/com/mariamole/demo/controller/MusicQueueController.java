package com.mariamole.demo.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mariamole.demo.model.HistoricoMusica;
import com.mariamole.demo.model.MusicaFila;
import com.mariamole.demo.repository.HistoricoMusicaRepository;

@RestController
@RequestMapping("/api/queue")
@CrossOrigin(origins = "*")
public class MusicQueueController {

  private final List<MusicaFila> songQueue = new java.util.concurrent.CopyOnWriteArrayList<>();

  private final HistoricoMusicaRepository historicoRepository;

  @Autowired
  public MusicQueueController(HistoricoMusicaRepository historicoRepository) {
    this.historicoRepository = historicoRepository;
  }

  @GetMapping("/next")
  public ResponseEntity<?> getNextSong() {
    for (MusicaFila musica : songQueue) {
      if (!musica.isJaTocou()) {
        atualizarHorarioExibicao(musica);

        return ResponseEntity.ok(musica);
      }
    }
    return ResponseEntity.ok().build();
  }

  @PostMapping("/complete")
  public ResponseEntity<?> completeSong(
      @RequestBody Map<String, String> payload) {
    String videoId = payload.get("videoId");
    if (videoId == null) {
      return ResponseEntity.badRequest().body("Missing videoId");
    }

    songQueue
        .stream()
        .filter(musica -> musica.getVideoId().equals(videoId) && !musica.isJaTocou())
        .findFirst()
        .ifPresent(musica -> musica.setJaTocou(true));

    songQueue.removeIf(musica -> musica.getVideoId().equals(videoId) && musica.isJaTocou());

    return ResponseEntity.ok().build();
  }

  @PostMapping("/add")
  public ResponseEntity<?> addSong(@RequestBody Map<String, String> payload) {
    String telefone = payload.get("telefone");
    String videoId = payload.get("videoId");
    String titulo = payload.get("titulo");
    String nome = payload.get("nome");

    if (telefone == null || videoId == null || nome == null || titulo == null) {
      return ResponseEntity.badRequest().body("Dados incompletos.");
    }
    boolean jaTemMusica = songQueue.stream()
        .anyMatch(m -> m.getTelefoneUsuario().equals(telefone) && !m.isJaTocou());

    if (jaTemMusica) {
      return ResponseEntity
          .status(409)
          .body("Utilizador já tem uma música na fila.");
    }

    MusicaFila novaMusica = new MusicaFila(videoId, titulo, nome, telefone);
    songQueue.add(novaMusica);

    try {
      HistoricoMusica historico = new HistoricoMusica(
          nome,
          telefone,
          videoId,
          titulo,
          LocalDateTime.now());
      historicoRepository.save(historico);
    } catch (Exception e) {
      System.err.println("Falha ao salvar no histórico: " + e.getMessage());
    }

    int position = getPosicaoPorTelefone(telefone);
    Map<String, Object> response = new HashMap<>();
    response.put("message", "Música adicionada!");
    response.put("position", position);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/position/{telefone}")
  public ResponseEntity<?> getPosition(@PathVariable String telefone) {
    int position = getPosicaoPorTelefone(telefone);

    Map<String, Object> response = new HashMap<>();
    response.put("position", position);

    return ResponseEntity.ok(response);
  }

  private void atualizarHorarioExibicao(MusicaFila musica) {
    try {
Optional<HistoricoMusica> optHistorico = historicoRepository
                .findFirstByVideoIdAndTelefoneUsuarioAndHorarioExibicaoIsNullOrderByHorarioCadastroDesc( 
                    musica.getVideoId(), musica.getTelefoneUsuario());

      if (optHistorico.isPresent()) {
        HistoricoMusica historico = optHistorico.get();
        historico.setHorarioExibicao(LocalDateTime.now());
        historicoRepository.save(historico);
      }
    } catch (Exception e) {
      System.err.println(
        "Falha ao atualizar horário de exibição: " + e.getMessage()
      );
    }
  }

  private int getPosicaoPorTelefone(String telefone) {
    List<MusicaFila> filaPorTocar = songQueue
        .stream()
        .filter(m -> !m.isJaTocou())
        .collect(Collectors.toList());

    for (int i = 0; i < filaPorTocar.size(); i++) {
      if (filaPorTocar.get(i).getTelefoneUsuario().equals(telefone)) {
        return i; // 0 = tocando agora, 1 = próxima
      }
    }

    return -1;
  }

  public boolean pularMusicaAtual() {
    Optional<MusicaFila> musicaParaPularOpt = songQueue
        .stream()
        .filter(m -> !m.isJaTocou())
        .findFirst();

    if (musicaParaPularOpt.isPresent()) {
      MusicaFila musicaParaPular = musicaParaPularOpt.get();

      musicaParaPular.setJaTocou(true);
      songQueue.remove(musicaParaPular);
      atualizarHorarioExibicao(musicaParaPular);

      System.out.println(
          "ADMIN: Música pulada: " + musicaParaPular.getTitulo());
      return true;
    }

    System.out.println("ADMIN: Tentativa de pular, mas a fila está vazia.");
    return false;
  }

  public List<MusicaFila> getSnapshotDaFila() {
    return songQueue
        .stream()
        .filter(m -> !m.isJaTocou())
        .limit(6)
        .collect(Collectors.toList());
  }

  public void adicionarMusicaComoAdmin(MusicaFila musica) {
        songQueue.add(musica);
        System.out.println("ADMIN: Adicionou à fila (override): " + musica.getTitulo());
    }
}
