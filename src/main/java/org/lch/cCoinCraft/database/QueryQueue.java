package org.lch.cCoinCraft.database;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * DB 작업을 순차적으로 처리하는 큐(별도 스레드).
 */
public class QueryQueue {

    private final BlockingQueue<QueryTask> queue = new LinkedBlockingQueue<>();
    private boolean running = true;

    public QueryQueue() {
        // 별도 스레드 시작
        Thread worker = new Thread(() -> {
            while (running) {
                try {
                    QueryTask task = queue.take();
                    // 실행
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "CCoinCraft-DBWorker");
        worker.start();
    }

    public void addTask(QueryTask task) {
        queue.add(task);
    }

    public void stopQueue() {
        running = false;
    }
}