package org.lch.cCoinCraft.database;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QueryQueue {

    private final BlockingQueue<QueryTask> queue = new LinkedBlockingQueue<>();
    private boolean running = true;

    public QueryQueue() {
        // 새로운 스레드로 Queue 태스크 처리
        Thread worker = new Thread(() -> {
            while (running) {
                try {
                    QueryTask task = queue.take(); // 큐에서 작업 꺼냄
                    task.run(); // DB 작업 수행
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        worker.setName("CCoinCraft-DBWorker");
        worker.start();
    }

    public void addTask(QueryTask task) {
        queue.add(task);
    }

    public void stopQueue() {
        running = false;
    }
}