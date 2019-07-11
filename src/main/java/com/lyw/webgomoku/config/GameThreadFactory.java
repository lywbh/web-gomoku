package com.lyw.webgomoku.config;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class GameThreadFactory implements ThreadFactory {

    private AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Thread newThread(Runnable r) {
        return new GameThread(r, counter);
    }

}
