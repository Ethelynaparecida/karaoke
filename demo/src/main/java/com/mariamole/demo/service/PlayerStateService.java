

package com.mariamole.demo.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service; // Necessário para AtomicLong

@Service
public class PlayerStateService {

    private final AtomicBoolean isPaused = new AtomicBoolean(true);
    private final AtomicLong lastRestartRequestTime = new AtomicLong(0L); 
    private final AtomicLong lastSkipRequestTime = new AtomicLong(0L);
    private final AtomicBoolean isQueueLocked = new AtomicBoolean(false); 

  
    public long getLastRestartRequestTime() { 
        return this.lastRestartRequestTime.get(); // Retorna o valor da variável AtomicLong
    }
    
    public long getLastSkipRequestTime() { 
        return this.lastSkipRequestTime.get();
    }
    
   

    public boolean isPaused() { return isPaused.get(); }
    public void pause() { this.isPaused.set(true); }
    public void play() { this.isPaused.set(false); }
    
    public boolean isQueueLocked() { return isQueueLocked.get(); }
    public void lockQueue() { this.isQueueLocked.set(true); }
    public void unlockQueue() { this.isQueueLocked.set(false); }

    public void restart() {
        this.lastRestartRequestTime.set(System.currentTimeMillis()); 
        this.isPaused.set(true); // Força a pausa após o restart
    }

    public void skip() {
        this.lastSkipRequestTime.set(System.currentTimeMillis());
        this.isPaused.set(true); // Força a pausa após o skip
    }

    // Método para o Frontend obter o status
    public PlayerStatus getStatus() {
        return new PlayerStatus(isPaused.get(), lastRestartRequestTime.get(), lastSkipRequestTime.get(), isQueueLocked.get());
    }

    // DTO simples para a resposta do status
    public static class PlayerStatus {
        public boolean isPaused;
        public long lastRestartRequestTime;
        public long lastSkipRequestTime;
        public boolean isQueueLocked;

        public PlayerStatus(boolean isPaused, long lastRestartRequestTime, long lastSkipRequestTime, boolean isQueueLocked) {
            this.isPaused = isPaused;
            this.lastRestartRequestTime = lastRestartRequestTime;
            this.lastSkipRequestTime = lastSkipRequestTime;
            this.isQueueLocked = isQueueLocked;
        }
    }
}