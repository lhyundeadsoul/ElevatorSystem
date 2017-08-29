package biz.jared.domain;

import biz.jared.strategy.Strategy;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * @author jared
 */
public class Dispatcher {
    /**
     * 电梯调度系统的任务列表
     */
    private BlockingQueue<Task> taskQueue = new PriorityBlockingQueue<>();
    private Strategy strategy;

    public Dispatcher(Strategy strategy) {
        this.strategy = strategy;
    }

    public void receive(Task task) {

    }
}
