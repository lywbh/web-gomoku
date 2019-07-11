package com.lyw.webgomoku.config;

import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ThreadPoolConfig {

    private static ThreadFactory gameThreadFactory = new GameThreadFactory();

    public static ExecutorService gamePool = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS, new SynchronousQueue<>(), gameThreadFactory);


    public static ExecutorService watcherPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS, new SynchronousQueue<>(), gameThreadFactory);

}
