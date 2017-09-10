package biz.jared.domain;

import biz.jared.strategy.DispatchStrategy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 多电梯系统的任务总调度器
 *
 * @author jared
 */
public class Dispatcher {
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
                    System.out.println("dispatcher can't select one elevator, maybe all of them are in max load , retry dispatch...");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("dispatch task:" + task + " result: give it to " + elevator);
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
        System.out.println("Redispatch task:" + task);
        dispatch(task);
    }

    /**
     * 一个电梯无任务退出
     *
     * @param elevator
     */
    void quit(Elevator elevator) {
        //不用 elevatorList.remove(o) 为了防止产生 concurrent exception
        Iterator<Elevator> iterator = elevatorList.iterator();
        while (iterator.hasNext() && iterator.next().equals(elevator)) {
            iterator.remove();
        }
        //无电梯可调度时要shutdown线程池
        if (elevatorList.isEmpty()) {
            executorService.shutdownNow();
        }
    }
}
