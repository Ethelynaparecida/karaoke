package com.mariamole.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mariamole.demo.model.HistoricoMusica;

@Repository
public interface HistoricoMusicaRepository extends JpaRepository<HistoricoMusica, Long> {

    List<HistoricoMusica> findByHorarioCadastroBetweenOrderByHorarioCadastroDesc(LocalDateTime start, LocalDateTime end);

Optional<HistoricoMusica> findFirstByVideoIdAndTelefoneUsuarioAndHorarioExibicaoIsNullOrderByHorarioCadastroDesc(String videoId, String telefoneUsuario);
    void deleteByHorarioCadastroBefore(LocalDateTime cutoffDate);

    List<HistoricoMusica> findAllByOrderByHorarioCadastroDesc();
}