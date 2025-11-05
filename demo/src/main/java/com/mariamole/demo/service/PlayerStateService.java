package com.mariamole.demo.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class PlayerStateService {

  private final AtomicBoolean isPaused = new AtomicBoolean(false);

  private final AtomicLong lastRestartRequestTime = new AtomicLong(0L);
  private final AtomicLong lastSkipRequestTime = new AtomicLong(0L);

  public void pause() {
    this.isPaused.set(true);
  }

  public void play() {
    this.isPaused.set(false);
  }

  public boolean isPaused() {
    return this.isPaused.get();
  }

  public void restart() {
    this.lastRestartRequestTime.set(System.currentTimeMillis());
  }

  public long getLastRestartRequestTime() {
    return this.lastRestartRequestTime.get();
  }
  public void skip() {
        this.lastSkipRequestTime.set(System.currentTimeMillis());
        // Também garantimos que o sistema não está em pausa
        this.isPaused.set(false);
    }

    public long getLastSkipRequestTime() {
        return this.lastSkipRequestTime.get();
    }
}
