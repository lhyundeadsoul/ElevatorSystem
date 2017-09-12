package biz.jared.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import biz.jared.strategy.DispatchStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        executorService.submit(() -> {
            Elevator elevator;
            //如果选不出来电梯，就一直重试
            while ((elevator = dispatchStrategy.select(elevatorList, task)) == null) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                    LOGGER.warn(
                        "dispatcher can't select one elevator, maybe all of them are in max load , retry dispatch...");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            LOGGER.info("dispatch task:{} result: give it to {}", task, elevator);
            elevator.receive(task);
        });
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
        //不用 elevatorList.remove(o) 为了防止产生 concurrent exception
        elevatorList.removeIf(elevator1 -> elevator1.equals(elevator));
        //无电梯可调度时要shutdown线程池
        if (elevatorList.isEmpty()) {
            executorService.shutdownNow();
        }
    }
}
