package biz.jared.domain;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import biz.jared.Calc;
import biz.jared.domain.enumeration.Direction;
import biz.jared.domain.enumeration.ElevatorStatus;
import biz.jared.domain.enumeration.TaskStatus;
import biz.jared.exception.CannotExecTaskException;
import biz.jared.exception.TaskCancelledException;
import biz.jared.exception.UserInElevatorTaskGrabbedException;
import biz.jared.exception.UserInFloorTaskGrabbedException;
import biz.jared.strategy.PriorityCalculationStrategy;

import static biz.jared.Env.MAX_LOAD;

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
     * 电梯没权利选择是否接受任务，接到任务只能尽力去执行（满载、当前任务被抢占才能redispatch），但是执行的优先级可以自己排
     *
     * @param task 待排期任务
     */
    void receive(Task task) {
        if (task != null && !taskQueue.contains(task)) {
            try {
                //定好优先级再放入队列
                task.setPriority(priorityCalculationStrategy.calcPriority(this, task));
                taskQueue.put(task);
                //如果当前任务比电梯正在执行的任务优先级还优先（priority较小，相等都不算），则发生任务抢占
                if (currTask != null && needGrab(task)) {
                    currTask.yield();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 是否可以抢占
     * 重新计算当前任务的优先级，并和当前收到的任务进行比较
     *
     * @param task
     * @return
     */
    private boolean needGrab(Task task) {
        int newPriority = tryReceive(currTask);
        currTask.setPriority(newPriority);
        return task.isPriorityHigherThan(currTask);
    }

    /**
     * 尝试receive task
     *
     * @param task
     * @return 此任务可能的权重
     */
    public int tryReceive(Task task) {
        return priorityCalculationStrategy.calcPriority(this, task);
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
                task = taskQueue.poll(10, TimeUnit.SECONDS);
                if (task == null) {
                    throw new InterruptedException();
                }
                //execute it
                execTask(task);
            } catch (InterruptedException e) {
                System.out.println(this + " has no task for a long time, so quit...");
                dispatcher.quit(this);
                break;
            } catch (TaskCancelledException e) {//任务被取消
                System.out.println(this + " task " + task + " has been cancelled ");
            } catch (CannotExecTaskException | UserInFloorTaskGrabbedException e) {//不能执行的任务要重新分配
                System.out.println(
                    task + " can not be executed by " + this + " caused by " + e + " so redispatching...");
                dispatcher.redispatch(task);
            } catch (UserInElevatorTaskGrabbedException e) {//电梯内用户任务被抢占，只能还是当前电梯处理其任务
                System.out.println(task + " has been grabbed so delay execute ...");
                receive(task);
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
    }

    /**
     * 执行任务逻辑
     *
     * @param task 待执行的任务
     * @throws TaskCancelledException 执行过程中任务被取消的情况
     * @throws CannotExecTaskException 执行过程中发现任务无法再执行的情况
     */
    private void execTask(Task task)
        throws TaskCancelledException, CannotExecTaskException, UserInElevatorTaskGrabbedException,
        InterruptedException, UserInFloorTaskGrabbedException {
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
        if (task.getStatus().equals(TaskStatus.CANCELLED)) {
            throw new TaskCancelledException();
        }
        System.out.println(this + "start to execute " + currTask);
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
        if (!reduceSet.isEmpty()) {
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
    private void move(Task task)
        throws TaskCancelledException, UserInElevatorTaskGrabbedException, InterruptedException,
        UserInFloorTaskGrabbedException {
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
                //电梯内用户的任务只能在当前电梯任务列表里重新分配，而电梯外用户的任务可以redispatch给其它的电梯，处理方式不同所以抛出不同的异常
                if (task.getDirection().equals(Direction.NONE)) {
                    throw new UserInElevatorTaskGrabbedException();
                } else {
                    throw new UserInFloorTaskGrabbedException();
                }
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

    private void setStatus(ElevatorStatus status) {
        this.status = status;
    }

    public Floor getCurrFloor() {
        return currFloor;
    }

    public void setCurrTask(Task currTask) {
        this.currTask = currTask;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public Set<User> getCurrLoad() {
        return currLoad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        Elevator elevator = (Elevator)o;

        return id == elevator.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
