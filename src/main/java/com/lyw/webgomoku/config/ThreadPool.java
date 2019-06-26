package com.lyw.webgomoku.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {

    public static ExecutorService gamePool = Executors.newCachedThreadPool();

}
