package biz.jared.domain;

import biz.jared.Calc;
import biz.jared.Env;
import biz.jared.domain.enumeration.Direction;
import biz.jared.domain.enumeration.ElevatorStatus;
import biz.jared.domain.enumeration.TaskStatus;
import biz.jared.exception.CannotExecTaskException;
import biz.jared.exception.TaskCancelledException;
import biz.jared.exception.UserInElevatorTaskGrabbedException;
import biz.jared.exception.UserInFloorTaskGrabbedException;
import biz.jared.strategy.PriorityCalculationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static biz.jared.Env.MAX_LOAD;

/**
 * @author jared
 */
public class Elevator implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Elevator.class);
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
     * 电梯的任务列表，读写操作都在电梯线程这一个线程里，所以不用锁保护
     */
    private BlockingQueue<Task> taskQueue = new PriorityBlockingQueue<>();
    /**
     * 任务优先级计算策略
     */
    private PriorityCalculationStrategy priorityCalculationStrategy;

    //================many lock to protect elevator status===================
    /**
     * 用于锁楼层（在读取楼层并做任务优先的决策时，不能改楼层数据）
     */
    private final ReadWriteLock currFloorLock = new ReentrantReadWriteLock();
    /**
     * dispatcher里的线程池要"连续读"currTask这个属性，而电梯线程可以"同时写"currTask这个属性，存在并发错误的可能，且不会引发任何异常，需要加锁
     */
    private final ReadWriteLock currTaskLock = new ReentrantReadWriteLock();
    /**
     * currLoad属性可能同时在电梯线程里写，在dispatchStrategy-select时读，所以要加锁保护
     */
    private final ReadWriteLock currLoadLock = new ReentrantReadWriteLock();
    /**
     * status属性可能同时在电梯线程里写，在dispatcher-tryReceive时读，所以要加锁保护
     */
    private final ReadWriteLock statusLock = new ReentrantReadWriteLock();

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
        //updateAllTaskPriorityOnReceive doReceive needGrab 三个方法都需要读取 currFloor 一起做出正确的决策，所以要加读锁保证中间不会有写操作
        currFloorLock.readLock().lock();
        if (task != null && !taskQueue.contains(task)) {
            //接收新任务前重新计算所有任务的优先级
            updateAllTaskPriorityOnReceive();
            //当前定好优先级再放入队列
            doReceive(task);
            //如果当前任务比电梯正在执行的任务优先级还优先（priority较小，相等都不算），则发生任务抢占
            currTaskLock.readLock().lock();
            if (currTask != null && needGrab(task)) {
                LOGGER.trace("{} grab {}", task, currFloor);
                currTask.yield();
            }
            currTaskLock.readLock().unlock();
        }
        currFloorLock.readLock().unlock();
    }

    /**
     * 计算任务优先级，然后放入优先队列
     *
     * @param task 要入队的任务
     */
    private void doReceive(Task task) {
        int priority = tryReceive(task);
        task.setPriority(priority);
        LOGGER.trace("get receive task {} priority {}", task, priority);
        try {
            taskQueue.put(task);
        } catch (InterruptedException e) {
            LOGGER.error("do receive task error", e);
        }
    }

    /**
     * 当接收任务时，更新所有任务列表里的任务优先级
     */
    private void updateAllTaskPriorityOnReceive() {
        Collection<Task> tempTaskList = new ArrayList<>();
        taskQueue.drainTo(tempTaskList);
        tempTaskList.forEach(this::doReceive);
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
        LOGGER.trace("update current {} priority {}", currTask, newPriority);
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
                task = taskQueue.poll(10 * Env.ELAPSED_TIME, Env.TIME_UNIT);
                if (task == null) {
                    throw new InterruptedException();
                }
                //execute it
                execTask(task);
            } catch (InterruptedException e) {
                dispatcher.quit(this);
                LOGGER.warn("{} has no task for a long time, so quit...", this);
                break;
            } catch (TaskCancelledException e) {//任务被取消
                LOGGER.warn("{} task {} has been cancelled", this, task);
            } catch (CannotExecTaskException | UserInFloorTaskGrabbedException e) {//不能执行的任务要重新分配
                LOGGER.warn("{} can not be executed by {} caused by {} so re-dispatching...", task, this, e);
                dispatcher.redispatch(task);
            } catch (UserInElevatorTaskGrabbedException e) {//电梯内用户任务被抢占，只能还是当前电梯处理其任务
                LOGGER.warn("{} has been grabbed so delay execute ...", task);
                receive(task);
            } catch (Throwable e) {//其它情况
                LOGGER.error("unknown error:", e);
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
     * @throws TaskCancelledException  执行过程中任务被取消的情况
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
        LOGGER.info("{} start to execute {}", this, currTask);
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
            getCurrLoadLock().writeLock().lock();
            currLoad.addAll(reduceSet);
            getCurrLoadLock().writeLock().unlock();
            LOGGER.info("{} loading {} users: {}", this, reduceSet.size(), reduceSet);
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
            getCurrLoadLock().writeLock().lock();
            currLoad.removeAll(unloadSet);
            getCurrLoadLock().writeLock().unlock();
            LOGGER.info("{} unloading {} users:{}", this, unloadSet.size(), unloadSet);
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
            //一定要先改变电梯的当前楼层，再楼层移动耗时。原因：当电梯门关上后，刚刚开始启动，这时即使还没到下一层楼，也要按下一层楼算了，因为当前楼层已经没机会上了，这和现实也是符合的
            setCurrFloor(currFloor.next(relativeDirection));
            Env.elapsed();
            //电梯运行总里程+1
            Env.TOTAL_ELEVATOR_MOVE_DISTANCE.incrementAndGet();
            LOGGER.info("{} moving {}", this, getStatus());
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

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public ElevatorStatus getStatus() {
        return status;
    }

    private void setStatus(ElevatorStatus status) {
        getStatusLock().writeLock().lock();
        this.status = status;
        getStatusLock().writeLock().unlock();
    }

    public Floor getCurrFloor() {
        return currFloor;
    }

    private void setCurrFloor(Floor currFloor) {
        currFloorLock.writeLock().lock();
        this.currFloor = currFloor;
        currFloorLock.writeLock().unlock();
    }

    private void setCurrTask(Task currTask) {
        currTaskLock.writeLock().lock();
        this.currTask = currTask;
        currTaskLock.writeLock().unlock();
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public Set<User> getCurrLoad() {
        return currLoad;
    }

    public ReadWriteLock getCurrLoadLock() {
        return currLoadLock;
    }

    public ReadWriteLock getStatusLock() {
        return statusLock;
    }

    @Override

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Elevator elevator = (Elevator) o;

        return id == elevator.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
