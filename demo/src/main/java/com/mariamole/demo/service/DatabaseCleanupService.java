package com.mariamole.demo.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.mariamole.demo.repository.HistoricoMusicaRepository;

@Service
public class DatabaseCleanupService {

    @Autowired
    private HistoricoMusicaRepository historicoRepository;

  
    @Scheduled(cron = "0 0 10 * * ?")
    public void deleteOldMusicHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(10);
        
        System.out.println("SCHEDULER: A apagar histórico de músicas anterior a " + cutoff);
        
        try {
            historicoRepository.deleteByHorarioCadastroBefore(cutoff);
            System.out.println("SCHEDULER: Limpeza concluída.");
        } catch (Exception e) {
            System.err.println("SCHEDULER: Erro ao apagar histórico: " + e.getMessage());
        }
    }
}
