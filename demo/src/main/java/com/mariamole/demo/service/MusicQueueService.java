package com.mariamole.demo.service;

import com.mariamole.demo.model.HistoricoMusica;
import com.mariamole.demo.model.MusicaFila;
import com.mariamole.demo.repository.HistoricoMusicaRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MusicQueueService {

  // Fila em memória e as dependências
  private final List<MusicaFila> songQueue = new CopyOnWriteArrayList<>();
  private final HistoricoMusicaRepository historicoRepository;
  private final PlayerStateService playerStateService;
  
  // AQUI ESTAVA O ERRO: Agora está Map<String, CancelamentoInfo> corretamente
  private final Map<String, CancelamentoInfo> mensagensCancelamento = new ConcurrentHashMap<>();
  
  private String errorVideoId = null;
  private String errorVideoUrl = null;
  private String errorMessage = null;
  private String errorUserName = null;

  @Autowired
  public MusicQueueService(
    HistoricoMusicaRepository historicoRepository,
    PlayerStateService playerStateService
  ) {
    this.historicoRepository = historicoRepository;
    this.playerStateService = playerStateService;
  }

  public int addSong(String telefone, String videoId, String titulo, String nome, String ipUsuario) {
    boolean jaTemMusica = songQueue
      .stream()
      .anyMatch(m -> m.getTelefoneUsuario().equals(telefone) && !m.isJaTocou());
    if (jaTemMusica) {
      return -2; 
    }

    boolean wasQueueEmpty = songQueue.isEmpty();
    MusicaFila novaMusica = new MusicaFila(videoId, titulo, nome, telefone);
    songQueue.add(novaMusica);

    if (wasQueueEmpty) {
      playerStateService.pause();
    }

    try {
      HistoricoMusica historico = new HistoricoMusica(
        nome,
        telefone,
        videoId,
        titulo,
        LocalDateTime.now()
      );
      
      historico.setIpUsuario(ipUsuario);
      
      historicoRepository.save(historico);
    } catch (Exception e) {
      System.err.println("Falha ao salvar no histórico: " + e.getMessage());
    }

    return getPosicaoPorTelefone(telefone);
  }

  public boolean cancelarMusica(String telefone, String mensagemAdmin) {
    Optional<MusicaFila> musicaCancelada = songQueue
      .stream()
      .filter(m -> m.getTelefoneUsuario().equals(telefone) && !m.isJaTocou())
      .findFirst();

    if (musicaCancelada.isPresent()) {
      MusicaFila musica = musicaCancelada.get();
      songQueue.remove(musica);

      String motivoSalvar = (
          mensagemAdmin != null && !mensagemAdmin.trim().isEmpty()
        )
        ? mensagemAdmin
        : "Sem justificação";

      LocalDateTime dataBaseParaDesbloqueio = LocalDateTime.now();

      try {
        Optional<HistoricoMusica> optHistorico = historicoRepository.findFirstByVideoIdAndTelefoneUsuarioAndHorarioExibicaoIsNullOrderByHorarioCadastroDesc(
          musica.getVideoId(),
          telefone
        );

        if (optHistorico.isPresent()) {
          HistoricoMusica historico = optHistorico.get();
          historico.setMotivoCancelamento(motivoSalvar);
          historicoRepository.save(historico);

          if (historico.getHorarioCadastro() != null) {
            dataBaseParaDesbloqueio = historico.getHorarioCadastro();
          }

          System.out.println(
            "ADMIN: Música " +
            musica.getTitulo() +
            " cancelada. Motivo: " +
            motivoSalvar
          );
        }
      } catch (Exception e) {
        System.err.println( "Falha ao salvar motivo no histórico: " + e.getMessage() );
      }


      mensagensCancelamento.put(
        telefone,
        new CancelamentoInfo(motivoSalvar, dataBaseParaDesbloqueio)
      );

      return true;
    }

    return false;
  }

  public String checarMensagemCancelamento(String telefone) {
    CancelamentoInfo info = mensagensCancelamento.get(telefone);

    if (info != null) {
      if (LocalDateTime.now().isAfter(info.dataReferencia.plusHours(24))) {
        mensagensCancelamento.remove(telefone);
        return null;
      }
      return info.mensagem;
    }
    return null;
  }

  public Optional<MusicaFila> getNextSong() {
    Optional<MusicaFila> nextSongOpt = songQueue
      .stream()
      .filter(m -> !m.isJaTocou())
      .findFirst();

    nextSongOpt.ifPresent(this::atualizarHorarioExibicao); // Marca o início da exibição no DB

    return nextSongOpt;
  }

  public boolean completeSong(String videoId) {
    Optional<MusicaFila> completedSong = songQueue
      .stream()
      .filter(musica ->
        musica.getVideoId().equals(videoId) && !musica.isJaTocou()
      )
      .findFirst();

    completedSong.ifPresent(musica -> musica.setJaTocou(true));

    boolean removed = songQueue.removeIf(musica ->
      musica.getVideoId().equals(videoId) && musica.isJaTocou()
    );

    if (removed) {
      playerStateService.pause(); // Pausa o player para aguardar o admin
      return true;
    }
    return false;
  }

  public boolean removeSongByUserId(String userId) {
    if (userId == null || userId.isEmpty()) {
      return false;
    }

    Optional<MusicaFila> musicaPendente = songQueue
      .stream()
      .filter(musica -> userId.equals(musica.getTelefoneUsuario()) && !musica.isJaTocou())
      .findFirst();

    if (musicaPendente.isPresent()) {
      MusicaFila musica = musicaPendente.get();
      
      songQueue.remove(musica);

      try {
        Optional<HistoricoMusica> optHistorico = historicoRepository.findFirstByVideoIdAndTelefoneUsuarioAndHorarioExibicaoIsNullOrderByHorarioCadastroDesc(
          musica.getVideoId(),
          userId
        );

        if (optHistorico.isPresent()) {
          HistoricoMusica historico = optHistorico.get();
          historico.setMotivoCancelamento("Usuário saiu da fila (Logout)");
          historicoRepository.save(historico);
          
          System.out.println(
            "LOGOUT: Música " + musica.getTitulo() + " do utilizador " + userId + " removida."
          );
        }
      } catch (Exception e) {
        System.err.println("Falha ao registar logout no histórico: " + e.getMessage());
      }

      return true;
    }

    return false;
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
      this.errorVideoId = null;
      this.errorVideoUrl = null;
      this.errorMessage = null;
      this.errorUserName = null;
      playerStateService.skip(); // Envia o comando de skip para o player
      return true;
    }
    return false;
  }

  public void adicionarMusicaComoAdmin(MusicaFila musica) {
    songQueue.add(musica);
    System.out.println(
      "ADMIN: Adicionou à fila (override): " + musica.getTitulo()
    );
  }

  public List<MusicaFila> getSnapshotDaFila() {
    return songQueue
      .stream()
      .filter(m -> !m.isJaTocou())
      .limit(6)
      .collect(Collectors.toList());
  }

  public int getPosicaoPorTelefone(String telefone) {
    List<MusicaFila> filaPorTocar = songQueue
      .stream()
      .filter(m -> !m.isJaTocou())
      .collect(Collectors.toList());

    for (int i = 0; i < filaPorTocar.size(); i++) {
      if (filaPorTocar.get(i).getTelefoneUsuario().equals(telefone)) {
        return i; // 0 = tocando agora, 1 = próximo, etc.
      }
    }
    return -1; // Não está na fila
  }

  private void atualizarHorarioExibicao(MusicaFila musica) {
    try {
      Optional<HistoricoMusica> optHistorico = historicoRepository.findFirstByVideoIdAndTelefoneUsuarioAndHorarioExibicaoIsNullOrderByHorarioCadastroDesc(
        musica.getVideoId(),
        musica.getTelefoneUsuario()
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

  public List<MusicaFila> getAllSongsInQueue() {
    return songQueue;
  }

  public int getQueueSize() {
    return songQueue.size();
  }

  public void reportVideoError(
    String videoId,
    String url,
    String message,
    String userName
  ) {
    this.errorVideoId = videoId;
    this.errorVideoUrl = url;
    this.errorMessage = message;
    this.errorUserName = userName;
    System.out.println(
      "ERRO REPORTADO PELO PLAYER: " + message + " (User: " + userName + ")"
    );
  }

  public Map<String, String> getErrorStatus() {
    Map<String, String> status = new HashMap<>();
    status.put("errorVideoId", this.errorVideoId);
    status.put("errorVideoUrl", this.errorVideoUrl);
    status.put("errorMessage", this.errorMessage);
    status.put("errorUserName", this.errorUserName);
    return status;
  }

  public boolean resolveErrorAndSkip() {
    boolean skipped = pularMusicaAtual();

    this.errorVideoId = null;
    this.errorVideoUrl = null;
    this.errorMessage = null;
    this.errorUserName = null;

    return skipped;
  }

  private static class CancelamentoInfo {

    String mensagem;
    LocalDateTime dataReferencia;

    CancelamentoInfo(String mensagem, LocalDateTime dataReferencia) {
      this.mensagem = mensagem;
      this.dataReferencia = dataReferencia;
    }
  }
}