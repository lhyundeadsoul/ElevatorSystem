package biz.jared.domain;

import biz.jared.strategy.Strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * 多电梯系统的任务总调度器
 * @author jared
 */
public class Dispatcher {
    /**
     * 电梯调度系统的任务列表
     */
    private BlockingQueue<Task> taskQueue = new PriorityBlockingQueue<>();
    /**
     * 可以调度的电梯列表
     */
    private List<Elevator> elevatorList = new ArrayList<>();
    /**
     * 任务分配策略
     */
    private Strategy dispatchStrategy;

    public Dispatcher(List<Elevator> elevatorList, Strategy dispatchStrategy) {
        this.elevatorList = elevatorList;
        this.dispatchStrategy = dispatchStrategy;
    }

    void receive(Task task) {

    }

    void cancel(Task task) {
        task.cancel();
    }

    public void redispatch(Task task) {
        receive(task);
    }

}
