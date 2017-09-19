package biz.jared.domain;

import biz.jared.Env;
import biz.jared.strategy.DispatchStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 多电梯系统的任务总调度器
 *
 * @author jared
 */
public class Dispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(Dispatcher.class);
    /**
     * 可以调度的电梯列表
     */
    private List<Elevator> elevatorList = new ArrayList<>();
    /**
     * 任务分配策略
     */
    private DispatchStrategy dispatchStrategy;
    /**
     * 保证 elevatorList 的读写互斥
     */
    private ReadWriteLock elevatorListLock = new ReentrantReadWriteLock();
    /**
     * 用于异步完成dispatch task
     */
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public Dispatcher(List<Elevator> elevatorList, DispatchStrategy dispatchStrategy) {
        this.elevatorList = elevatorList;
        this.dispatchStrategy = dispatchStrategy;
    }

    /**
     * 给一个任务分配电梯
     *
     * @param task
     */
    void dispatch(Task task) {
        if (task == null) {
            return;
        }
        // !executorService.isShutdown() this for avoid RejectedExecutionException
        if (executorService != null && !executorService.isShutdown()) {
            executorService.submit(() -> {
                Elevator elevator;
                //如果选不出来电梯，就一直重试
                while ((elevator = dispatchStrategy.select(elevatorList, task)) == null) {
                    Env.elapsed();
                    LOGGER.warn(
                            "dispatcher can't select one elevator, maybe all of them are in max load , retry dispatch...");
                }
                LOGGER.info("dispatch task:{} result: give it to {}", task, elevator);
                elevator.receive(task);
            });
        }
    }

    void cancel(Task task) {
        task.cancel();
    }

    /**
     * 电梯发起的任务重分配
     *
     * @param task
     */
    void redispatch(Task task) {
        LOGGER.info("Redispatch task:{}", task);
        dispatch(task);
    }

    /**
     * 一个电梯无任务退出
     *
     * @param elevator
     */
    void quit(Elevator elevator) {
        elevatorListLock.writeLock().lock();
        elevatorList.removeIf(e -> e.equals(elevator));
        elevatorListLock.writeLock().unlock();
        //无电梯可调度时要shutdown线程池
        if (elevatorList.isEmpty()) {
            executorService.shutdown();
        }
        Env.LATCH.countDown();
    }

    public ReadWriteLock getElevatorListLock() {
        return elevatorListLock;
    }
}
