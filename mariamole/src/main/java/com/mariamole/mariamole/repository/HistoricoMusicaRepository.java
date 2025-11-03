package com.mariamole.mariamole.repository;

import com.mariamole.mariamole.model.HistoricoMusica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HistoricoMusicaRepository extends JpaRepository<HistoricoMusica, Long> {

    List<HistoricoMusica> findByHorarioCadastroBetween(LocalDateTime start, LocalDateTime end);

    Optional<HistoricoMusica> findFirstByVideoIdAndCpfUsuarioAndHorarioExibicaoIsNullOrderByHorarioCadastroDesc(String videoId, String cpfUsuario);
}