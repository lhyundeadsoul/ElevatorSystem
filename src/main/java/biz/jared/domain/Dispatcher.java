package biz.jared.domain;

import biz.jared.strategy.DispatchStrategy;

import java.util.ArrayList;
import java.util.List;

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

    public Dispatcher(List<Elevator> elevatorList, DispatchStrategy dispatchStrategy) {
        this.elevatorList = elevatorList;
        this.dispatchStrategy = dispatchStrategy;
    }

    /**
     * 给一个任务分配电梯
     *
     * @param task
     */
    Elevator dispatch(Task task) {
        if (task == null) {
            return null;
        }
        Elevator elevator = dispatchStrategy.select(elevatorList, task);
        //如果选不出来电梯，就先放dispatcher这里暂存，否则交给电梯执行
        System.out.println("dispatch task:" + task + " result: give it to " + elevator);
        elevator.receive(task);
        return elevator;
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

}
