package com.mariamole.mariamole.controller;

import com.mariamole.mariamole.model.HistoricoMusica;
import com.mariamole.mariamole.model.MusicaFila;
import com.mariamole.mariamole.repository.HistoricoMusicaRepository;
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
    @RequestBody Map<String, String> payload
  ) {
    String videoId = payload.get("videoId");
    if (videoId == null) {
      return ResponseEntity.badRequest().body("Missing videoId");
    }

    songQueue
      .stream()
      .filter(musica ->
        musica.getVideoId().equals(videoId) && !musica.isJaTocou()
      )
      .findFirst()
      .ifPresent(musica -> musica.setJaTocou(true));

    songQueue.removeIf(musica ->
      musica.getVideoId().equals(videoId) && musica.isJaTocou()
    );

    return ResponseEntity.ok().build();
  }

  @PostMapping("/add")
  public ResponseEntity<?> addSong(@RequestBody Map<String, String> payload) {
    String cpf = payload.get("cpf");
    String videoId = payload.get("videoId");
    String titulo = payload.get("titulo");
    String nome = payload.get("nome");
   /**Valida se o usuario ja possui uma musica na fila */
    boolean jaTemMusica = songQueue
      .stream()
      .anyMatch(m -> m.getCpfUsuario().equals(cpf) && !m.isJaTocou());

    if (jaTemMusica) {
      return ResponseEntity
        .status(409)
        .body("Utilizador já tem uma música na fila.");
    }

    MusicaFila novaMusica = new MusicaFila(videoId, titulo, nome, cpf);
    songQueue.add(novaMusica);

    try {
      HistoricoMusica historico = new HistoricoMusica(
        nome,
        cpf,
        videoId,
        titulo,
        LocalDateTime.now()
      );
      historicoRepository.save(historico);
    } catch (Exception e) {
      System.err.println("Falha ao salvar no histórico: " + e.getMessage());
    }

    int position = getPosicaoPorCpf(cpf);
    Map<String, Object> response = new HashMap<>();
    response.put("message", "Música adicionada!");
    response.put("position", position);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/position/{cpf}")
  public ResponseEntity<?> getPosition(@PathVariable String cpf) {
    int position = getPosicaoPorCpf(cpf);

    Map<String, Object> response = new HashMap<>();
    response.put("position", position);

    return ResponseEntity.ok(response);
  }

  private void atualizarHorarioExibicao(MusicaFila musica) {
    try {
      Optional<HistoricoMusica> optHistorico = historicoRepository.findFirstByVideoIdAndCpfUsuarioAndHorarioExibicaoIsNullOrderByHorarioCadastroDesc(
        musica.getVideoId(),
        musica.getCpfUsuario()
      );

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

  /*** Encontrar a posição de um utilizador na fila.
   * @return Posição (int).
   * -1 = Já tocou / Não está na fila
   * 0 = A tocar agora
   * >0 = Posição na fila (ex: 1 = próximo, 2 = segundo na fila...)*/
  private int getPosicaoPorCpf(String cpf) {
    List<MusicaFila> filaPorTocar = songQueue
      .stream()
      .filter(m -> !m.isJaTocou())
      .collect(Collectors.toList());

    for (int i = 0; i < filaPorTocar.size(); i++) {
      if (filaPorTocar.get(i).getCpfUsuario().equals(cpf)) {
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
        "ADMIN: Música pulada: " + musicaParaPular.getTitulo()
      );
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
}
