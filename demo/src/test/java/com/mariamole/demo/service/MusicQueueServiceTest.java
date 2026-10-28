package com.mariamole.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mariamole.demo.model.HistoricoMusica;
import com.mariamole.demo.repository.HistoricoMusicaRepository;

@ExtendWith(MockitoExtension.class)
class MusicQueueServiceTest {

    @Mock
    private HistoricoMusicaRepository historicoRepository;

    @Mock
    private PlayerStateService playerStateService;

    private MusicQueueService service;

    @BeforeEach
    void setUp() {
        service = new MusicQueueService(historicoRepository, playerStateService);
    }

    @Test
    void deveRetornarTop10MaisPedidasDosUltimos30Dias() {
        LocalDateTime agora = LocalDateTime.now();

        when(historicoRepository.findByHorarioCadastroBetweenOrderByHorarioCadastroDesc(any(), any()))
            .thenReturn(List.of(
                new HistoricoMusica("Ana", "111", "v1", "Believer", agora.minusDays(5)),
                new HistoricoMusica("Bia", "222", "v2", "Believer", agora.minusDays(7)),
                new HistoricoMusica("Carlos", "333", "v3", "Someone Like You", agora.minusDays(1)),
                new HistoricoMusica("Dani", "444", "v4", "Someone Like You", agora.minusDays(2)),
                new HistoricoMusica("Eli", "555", "v5", "Someone Like You", agora.minusDays(3)),
                new HistoricoMusica("Fabi", "666", "v6", "Hello", agora.minusDays(10))
            ));

        List<Map<String, Object>> resultado = service.getTop10MaisPedidasUltimos30Dias();

        assertFalse(resultado.isEmpty());
        assertEquals("Someone Like You", resultado.get(0).get("titulo"));
        assertEquals(3, resultado.get(0).get("totalPedidos"));
        assertEquals("Believer", resultado.get(1).get("titulo"));
    }

    @Test
    void deveSugerirMusicasPorTextoSemUsarApiDoYoutube() {
        LocalDateTime agora = LocalDateTime.now();

        when(historicoRepository.findAllByOrderByHorarioCadastroDesc())
            .thenReturn(List.of(
                new HistoricoMusica("Ana", "111", "v1", "Someone Like You", agora.minusDays(1)),
                new HistoricoMusica("Bia", "222", "v2", "Dream On", agora.minusDays(2)),
                new HistoricoMusica("Carlos", "333", "v3", "Believer", agora.minusDays(3))
            ));

        List<Map<String, Object>> resultado = service.sugerirMusicasPorTexto("bel");

        assertFalse(resultado.isEmpty());
        assertEquals("Believer", resultado.get(0).get("titulo"));
    }
}
