package biz.jared.domain;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import biz.jared.Calc;
import biz.jared.domain.enumeration.Direction;
import biz.jared.domain.enumeration.Status;
import biz.jared.exception.CannotExecTaskException;
import biz.jared.exception.TaskCancelledException;

/**
 * @author jared
 */
public class Elevator implements Runnable {

    private int id;
    /**
     * 当前所处楼层
     */
    private Floor currFloor;
    /**
     * 电梯负载人群
     */
    private Set<User> currLoad = new HashSet<>(MAX_LOAD);
    /**
     * 最大负载人数
     */
    private static final int MAX_LOAD = 10;
    /**
     * 负责调度此电梯的调度器
     */
    private Dispatcher dispatcher;
    /**
     * 电梯的任务列表
     */
    private BlockingQueue<Task> taskQueue = new PriorityBlockingQueue<>();

    public Elevator(int id, Floor initFloor) {
        this.id = id;
        this.currFloor = initFloor;
    }

    /**
     * 电梯接收任务时，不会接收重复的任务
     *
     * @param task
     */
    void receive(Task task) {
        if (!taskQueue.contains(task)) {
            taskQueue.add(task);
        }
    }

    /**
     * 电梯运行逻辑
     */
    @Override
    public void run() {
        while (true) {
            Task task = null;
            try {
                //获取任务
                task = taskQueue.take();
                //执行任务
                execTask(task);
            } catch (InterruptedException e) {
                System.out.println(this + " take task error: " + e);
            } catch (CannotExecTaskException e) {//不能执行的任务要重新分配
                System.out.println(task + " can not be executed by " + this + " redispatch...");
                dispatcher.redispatch(task);
            } catch (TaskCancelledException e) {
                System.out.println(task + " has cancelled");
            }
        }
    }

    private void execTask(Task task) throws TaskCancelledException, CannotExecTaskException {
        //以下为执行任务逻辑
        //不用执行：已被取消的任务
        if (task.getStatus().equals(Status.CANCELLED)) {
            throw new TaskCancelledException();
        }
        //无法执行：已经满载且当前楼层没人下的电梯，要将自身的任务重新交给dispatcher分配
        if (currLoad.size() == MAX_LOAD && !canReduceLoad(task.getFloor())) {
            throw new CannotExecTaskException();
        }
        //执行
        //1. go currFloor
        go(task);
        //2. unload user
        unload();
        //3. load user
        load();
    }

    /**
     * 是否有人从floor层下电梯
     *
     * @param floor
     * @return
     */
    private boolean canReduceLoad(Floor floor) {
        return currLoad.stream().anyMatch(user -> user.getTargetFloor().equals(floor));
    }

    private void load() {
        //楼层减少负载
        Set<User> reduceSet = currFloor.reduce(MAX_LOAD - currLoad.size());
        //电梯增加负载
        currLoad.addAll(reduceSet);
        //每个上电梯的人都按一下想去的楼层
        reduceSet.forEach(user -> user.select(user.getTargetFloor()));
    }

    private void unload() {
        currLoad.removeAll(
            currLoad.stream()
                .filter(user -> user.getTargetFloor().equals(currFloor))
                .collect(Collectors.toSet())
        );
    }

    /**
     * 去到某楼层
     *
     * @param task
     */
    private void go(Task task) throws TaskCancelledException {
        int distance = Calc.calcDistance(currFloor, task.getFloor());//相对距离，可为负
        Direction direction = distance > 0 ? Direction.DOWN : Direction.UP;
        for (int i = 0; i < Math.abs(distance); i++) {
            try {
                //楼层移动耗时1s
                TimeUnit.SECONDS.sleep(1);
                //改变电梯的当前楼层
                currFloor = currFloor.getNextFloor(direction);
                //检查任务状态，取消状态要抛异常
                if (task.getStatus().equals(Status.CANCELLED)) {
                    throw new TaskCancelledException();
                }
                System.out.println(this);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        return "Elevator{" +
            "id=" + id +
            ", currFloor=" + currFloor +
            ", currLoad=" + currLoad.size() +
            '}';
    }

    public int getId() {
        return id;
    }
}
