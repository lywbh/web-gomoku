package com.lyw.webgomoku.config;

import java.util.concurrent.atomic.AtomicInteger;

public class GameThread extends Thread {

    private AtomicInteger c;

    GameThread(Runnable r, AtomicInteger c) {
        super(r, "game-thread-" + c);
        this.c = c;
    }

    @Override
    public void run() {
        try {
            super.run();
        } finally {
            c.getAndDecrement();
        }
    }

}
