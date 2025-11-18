package com.mariamole.demo.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

@Service
public class PlayerStateService {

  private final AtomicBoolean isPaused = new AtomicBoolean(false);

  private final AtomicLong lastRestartRequestTime = new AtomicLong(0L);
  private final AtomicLong lastSkipRequestTime = new AtomicLong(0L);

  private final AtomicBoolean queueLocked = new AtomicBoolean(false);

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
        this.isPaused.set(false);
    }

    public long getLastSkipRequestTime() {
        return this.lastSkipRequestTime.get();
    }

    public void lockQueue() {
        this.queueLocked.set(true);
    }

    public void unlockQueue() {
        this.queueLocked.set(false);
    }

    public boolean isQueueLocked() {
        return this.queueLocked.get();
    }
}
