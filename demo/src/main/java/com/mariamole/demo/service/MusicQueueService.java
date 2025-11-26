package com.mariamole.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mariamole.demo.model.HistoricoMusica;
import com.mariamole.demo.model.MusicaFila;
import com.mariamole.demo.repository.HistoricoMusicaRepository;

@Service
public class MusicQueueService {

    // Fila em memória e as dependências
    private final List<MusicaFila> songQueue = new CopyOnWriteArrayList<>();
    private final HistoricoMusicaRepository historicoRepository;
    private final PlayerStateService playerStateService; 

    @Autowired
    public MusicQueueService(HistoricoMusicaRepository historicoRepository, 
                             PlayerStateService playerStateService) {
        this.historicoRepository = historicoRepository;
        this.playerStateService = playerStateService;
    }

    /**
     * Adiciona música à fila, aplicando regras de negócio (Limite de 1 por telefone).
     * @return Posição na fila, ou -2 se o utilizador já tiver música.
     */
    public int addSong(String telefone, String videoId, String titulo, String nome) {
        
        // 1. Validação de Limite
        boolean jaTemMusica = songQueue.stream()
                                  .anyMatch(m -> m.getTelefoneUsuario().equals(telefone) && !m.isJaTocou());
        if (jaTemMusica) {
            return -2; // Código de erro customizado: Usuário já tem música
        }

        // 2. Adiciona a Música
        boolean wasQueueEmpty = songQueue.isEmpty();
        MusicaFila novaMusica = new MusicaFila(videoId, titulo, nome, telefone);
        songQueue.add(novaMusica);
        
        // 3. Se foi a primeira música, força o player a PAUSAR
        if (wasQueueEmpty) {
            playerStateService.pause();
        }
        
        // 4. Salva no histórico
        try {
            HistoricoMusica historico = new HistoricoMusica(nome, telefone, videoId, titulo, LocalDateTime.now());
            historicoRepository.save(historico);
        } catch (Exception e) { 
            System.err.println("Falha ao salvar no histórico: " + e.getMessage());
        }

        return getPosicaoPorTelefone(telefone);
    }
    
    /**
     * Obtém a próxima música para o Player (TV).
     */
    public Optional<MusicaFila> getNextSong() {
        Optional<MusicaFila> nextSongOpt = songQueue.stream()
            .filter(m -> !m.isJaTocou())
            .findFirst();

        nextSongOpt.ifPresent(this::atualizarHorarioExibicao); // Marca o início da exibição no DB

        return nextSongOpt;
    }

    /**
     * Marca a música como tocada e a remove da fila em memória.
     */
    public boolean completeSong(String videoId) {
        Optional<MusicaFila> completedSong = songQueue.stream()
            .filter(musica -> musica.getVideoId().equals(videoId) && !musica.isJaTocou())
            .findFirst();

        completedSong.ifPresent(musica -> musica.setJaTocou(true));
            
        boolean removed = songQueue.removeIf(musica -> musica.getVideoId().equals(videoId) && musica.isJaTocou());
            
        if (removed) {
            playerStateService.pause(); // Pausa o player para aguardar o admin
            return true;
        }
        return false;
    }

    /**
     * Remove a música da fila que foi adicionada por um usuário específico (Logout).
     */
    public boolean removeSongByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return false;
        }
        
        // Remove a primeira música não tocada do usuário com o ID (telefone)
        boolean removed = songQueue.removeIf(musica -> userId.equals(musica.getTelefoneUsuario()) && !musica.isJaTocou());

        if (removed) {
            System.out.println("LOGOUT: Música do usuário " + userId + " removida da fila.");
        }
        return removed;
    }

    /**
     * Força a remoção e atualização da primeira música para o comando 'Pular'.
     */
    public boolean pularMusicaAtual() {
        Optional<MusicaFila> musicaParaPularOpt = songQueue.stream()
                .filter(m -> !m.isJaTocou())
                .findFirst();

        if (musicaParaPularOpt.isPresent()) {
            MusicaFila musicaParaPular = musicaParaPularOpt.get();
            musicaParaPular.setJaTocou(true);
            songQueue.remove(musicaParaPular);
            atualizarHorarioExibicao(musicaParaPular);
            playerStateService.skip(); // Envia o comando de skip para o player
            return true;
        }
        return false; 
    }
    
    /**
     * Adiciona uma música à fila diretamente como Admin (sem verificação de limite).
     */
    public void adicionarMusicaComoAdmin(MusicaFila musica) {
        songQueue.add(musica);
        System.out.println("ADMIN: Adicionou à fila (override): " + musica.getTitulo());
    }
    
    /**
     * Retorna um "snapshot" das próximas 6 músicas na fila (para o Dashboard).
     */
    public List<MusicaFila> getSnapshotDaFila() {
        return songQueue.stream()
                .filter(m -> !m.isJaTocou())
                .limit(6)
                .collect(Collectors.toList());
    }

    /**
     * Função auxiliar para encontrar a posição de um utilizador na fila.
     */
    public int getPosicaoPorTelefone(String telefone) {
        List<MusicaFila> filaPorTocar = songQueue.stream()
                                            .filter(m -> !m.isJaTocou())
                                            .collect(Collectors.toList());

        for (int i = 0; i < filaPorTocar.size(); i++) {
            if (filaPorTocar.get(i).getTelefoneUsuario().equals(telefone)) {
                return i; // 0 = tocando agora, 1 = próximo, etc.
            }
        }
        return -1; // Não está na fila
    }
    
    // ----------------------------------------------------
    // MÉTODOS AUXILIARES (Internal Logic)
    // ----------------------------------------------------
    
    /**
     * Encontra e atualiza o registo no DB com o horário de exibição.
     */
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
            System.err.println("Falha ao atualizar horário de exibição: " + e.getMessage());
        }
    }
}