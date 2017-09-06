package biz.jared.domain;

import biz.jared.Calc;
import biz.jared.domain.enumeration.Direction;
import biz.jared.domain.enumeration.ElevatorStatus;
import biz.jared.domain.enumeration.TaskStatus;
import biz.jared.exception.CannotExecTaskException;
import biz.jared.exception.TaskGrabbedException;
import biz.jared.exception.TaskCancelledException;
import biz.jared.strategy.PriorityCalculationStrategy;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
     * 当前运行状态
     */
    private ElevatorStatus status;
    /**
     * 电梯负载人群
     */
    private Set<User> currLoad = new HashSet<>(MAX_LOAD);
    /**
     * 当前正在执行的任务
     */
    private Task currTask;
    /**
     * 最大负载人数
     */
    private static final int MAX_LOAD = 10;
    /**
     * 负责调度此电梯的调度器
     */
    private Dispatcher dispatcher;
    /**
     * 电梯的任务列表，只是单方向的任务序列，逻辑简单、单一
     */
    private BlockingQueue<Task> taskQueue = new PriorityBlockingQueue<>();
    /**
     * 任务优先级计算策略
     */
    private PriorityCalculationStrategy priorityCalculationStrategy;

    public Elevator(int id, Floor initFloor, PriorityCalculationStrategy priorityCalculationStrategy) {
        this.id = id;
        this.currFloor = initFloor;
        setStatus(ElevatorStatus.IDLE);
        this.priorityCalculationStrategy = priorityCalculationStrategy;
    }

    /**
     * 接到任务就要尽力去执行（满载、当前任务被抢占才能redispatch），执行的优先级可以自己排
     *
     * @param task
     */
    void receive(Task task) {
        //当前任务对应楼层的抵达 和当前电梯顺路？   fixme 这段逻辑要移到strategy里去，电梯没权利选择是否接受
        //boolean inSameDirection =
        //    status.equals(ElevatorStatus.IDLE) ||
        //        (currFloor.locate(task.getSrcFloor()).equals(Direction.UP)
        //            && status.equals(ElevatorStatus.RUNNING_DOWN)) ||
        //        (currFloor.locate(task.getSrcFloor()).equals(Direction.DOWN)
        //            && status.equals(ElevatorStatus.RUNNING_UP));
        //if (!taskQueue.contains(task) && inSameDirection) {
        if (task != null && !taskQueue.contains(task)) {
            try {
                //定好优先级再放入队列
                task.setPriority(priorityCalculationStrategy.calcPriority(this, task));
                taskQueue.put(task);
                //如果当前任务比电梯正在执行的任务优先级还优先（priority较小，相等都不算），则发生任务抢占
                if (task.isPriorityHigher(currTask)) {
                    //改成runnable状态可以让电梯在move的过程中发现已被抢占
                    currTask.yield();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
                //get task
                task = taskQueue.take();
                //execute it
                execTask(task);
            } catch (InterruptedException | TaskCancelledException e) {
                System.out.println(this + " take task error: " + e);
            } catch (CannotExecTaskException | TaskGrabbedException e) {//不能执行的任务要重新分配
                System.out.println(
                    task + " can not be executed by " + this + " caused by " + e + " so redispatching...");
                dispatcher.redispatch(task);
            } finally {
                //finish, i'm idle
                onIdle();
            }
        }
    }

    /**
     * 电梯空闲时要主动找dispatcher尝试领task
     */
    private void onIdle() {
        setCurrTask(null);
        setStatus(ElevatorStatus.IDLE);
        dispatcher.dispatch(this);
    }

    /**
     * 执行任务逻辑
     *
     * @param task 待执行的任务
     * @throws TaskCancelledException 执行过程中任务被取消的情况
     * @throws CannotExecTaskException 执行过程中发现任务无法再执行的情况
     */
    private void execTask(Task task)
        throws TaskCancelledException, CannotExecTaskException, TaskGrabbedException, InterruptedException {
        if (task == null) {
            return;
        }
        setCurrTask(task);
        //以下为执行任务逻辑
        //无法执行：已经满载且当前楼层没人下的电梯，要将自身的任务重新交给dispatcher分配
        if (currLoad.size() == MAX_LOAD && !canReduceLoad(task.getSrcFloor())) {
            throw new CannotExecTaskException();
        }
        //在任务执行之前检查已经被取消的任务
        if (task.getStatus().equals(TaskStatus.CANCELLED)){
            throw new TaskCancelledException();
        }
        System.out.println("start to execute " + currTask);
        //执行
        //1. move currFloor
        move(task);
        //1.1 wherever task wanna go , elevator go
        setStatus(task.getDirection().equals(Direction.DOWN) ? ElevatorStatus.RUNNING_DOWN : ElevatorStatus.RUNNING_UP);
        //2. unload user
        unload();
        //3. load user who wanna go task's direction
        load(task.getDirection());
        //4. current floor task has bean done
        currFloor.done(task.getDirection());
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

    private void load(Direction direction) {
        //楼层减少负载
        Set<User> reduceSet = currFloor.reduce(direction, MAX_LOAD - currLoad.size());
        if (reduceSet.size() > 0) {
            //电梯增加负载
            currLoad.addAll(reduceSet);
            System.out.println(this + " loading " + reduceSet.size() + " users:" + reduceSet);
            //每个上电梯的人都按一下想去的楼层
            reduceSet.forEach(user -> {
                user.enterElevator(this);
                user.select(user.getTargetFloor());
            });
        }
    }

    private void unload() {
        //获取已经到目标楼层的人
        Set<User> unloadSet = currLoad.stream()
            .filter(user -> user.getTargetFloor().equals(currFloor))
            .collect(Collectors.toSet());
        if (unloadSet.size() > 0) {
            //卸载掉
            currLoad.removeAll(unloadSet);
            System.out.println(this + " unloading " + unloadSet.size() + " users:" + unloadSet);
        }
    }

    /**
     * 一层一层的去到某楼层，期间要检查任务是否已经被抢占
     *
     * @param task
     */
    private void move(Task task) throws TaskCancelledException, TaskGrabbedException, InterruptedException {
        //设置任务状态
        task.setStatus(TaskStatus.RUNNING);

        //设置电梯运行状态
        Direction relativeDirection = task.getSrcFloor().locate(currFloor);
        setStatus(Direction.UP.equals(relativeDirection) ? ElevatorStatus.RUNNING_UP : ElevatorStatus.RUNNING_DOWN);

        //算好绝对距离，向目标楼层进发
        int distance = Math.abs(Calc.calcDistance(currFloor, task.getSrcFloor()));
        for (int i = 0; i < distance; i++) {
            //执行过程中检查，已取消的任务停止执行
            if (task.getStatus().equals(TaskStatus.CANCELLED)) {
                throw new TaskCancelledException();
            }
            //执行过程中检查，已被抢占的任务停止执行
            if (task.getStatus().equals(TaskStatus.RUNNABLE)) {
                throw new TaskGrabbedException();
            }
            //楼层移动耗时
            TimeUnit.SECONDS.sleep(1);
            //改变电梯的当前楼层
            currFloor = currFloor.next(relativeDirection);
            System.out.println(this + " moving:  " + getStatus());
        }
    }

    @Override
    public String toString() {
        return "Elevator{" +
            "id=" + id +
            ", currFloor=" + currFloor.getFloorNo() +
            ", currLoad=" + currLoad +
            '}';
    }

    public int getId() {
        return id;
    }

    public ElevatorStatus getStatus() {
        return status;
    }

    public Floor getCurrFloor() {
        return currFloor;
    }

    private void setStatus(ElevatorStatus status) {
        this.status = status;
    }

    public void setCurrTask(Task currTask) {
        this.currTask = currTask;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
}
