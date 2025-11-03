package com.mariamole.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class PlayerStateService {

    private final AtomicBoolean isPaused = new AtomicBoolean(false);

    public void pause() {
        this.isPaused.set(true);
    }

    public void play() {
        this.isPaused.set(false);
    }

    public boolean isPaused() {
        return this.isPaused.get();
    }
}