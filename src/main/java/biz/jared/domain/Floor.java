package biz.jared.domain;

import biz.jared.domain.enumeration.Direction;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author jared
 */
public class Floor {

    private int floorNo;
    private Floor preFloor, nextFloor;
    /**
     * 当前楼层往上走的等候人群
     */
    private Set<User> waitingUpUserSet = new HashSet<>(100);
    /**
     * 当前楼层往下走的等候人群
     */
    private Set<User> waitingDownUserSet = new HashSet<>(100);
    /**
     * 向上等候人群读写锁，杜绝并发异常，因为waitingUpUserSet可能会同时在main线程里写，在电梯线程里读
     */
    private ReadWriteLock upUserSetLock = new ReentrantReadWriteLock();
    /**
     * 向下等候人群读写锁，杜绝并发异常
     */
    private ReadWriteLock downUserSetLock = new ReentrantReadWriteLock();
    /**
     * 已经有人表达说要去的方向集合
     */
    private Map<Direction, Task> waitingDirectionMap = new HashMap<>(Direction.values().length);
    /**
     * waitingDirectionMap可能会同时在main线程里读，在电梯线程里写
     */
    private ReadWriteLock waitingDirectionMapLock = new ReentrantReadWriteLock();
    /**
     * 整个电梯的调度
     */
    private Dispatcher dispatcher;

    public Floor(int floorNo) {
        this.floorNo = floorNo;
    }

    public int getFloorNo() {
        return floorNo;
    }

    Floor next(Direction direction) {
        return Direction.UP.equals(direction) ? nextFloor : preFloor;
    }

    public Floor next(Floor nextFloor) {
        this.nextFloor = nextFloor;
        nextFloor.previous(this);
        return nextFloor;
    }

    private void previous(Floor preFloor) {
        this.preFloor = preFloor;
    }

    /**
     * 楼层又来了人
     *
     * @param user
     */
    public void add(User user, Direction direction) {
        //给相应方向上的等待队列加用户
        if (direction.equals(Direction.UP)) {
            upUserSetLock.writeLock().lock();
            waitingUpUserSet.add(user);
            upUserSetLock.writeLock().unlock();
        } else if (direction.equals(Direction.DOWN)) {
            downUserSetLock.writeLock().lock();
            waitingDownUserSet.add(user);
            downUserSetLock.writeLock().unlock();
        }
        //给楼层加当前方向上的任务
        addDirectionTask(direction);
        //用户等待时间开始计时
        user.getStopwatch().start();
    }

    private void addDirectionTask(Direction direction) {
        //只有之前没人说要去的方向才可以建任务，已经有人说要去的方向就不用再说一次了
        waitingDirectionMapLock.readLock().lock();
        if (!waitingDirectionMap.containsKey(direction)) {
            Task task = Task.generate(this, direction);
            waitingDirectionMap.put(direction, task);
            dispatcher.dispatch(task);
        }
        waitingDirectionMapLock.readLock().unlock();
    }

    /**
     * 楼层可以减少num人
     *
     * @param direction 可以带走向哪个方向走的人
     * @param num       可以减少的人数
     * @return 减少的人集合
     */
    Set<User> reduce(Direction direction, int num) {
        Set<User> reduceSet = new HashSet<>(num);
        //准备接走哪一个方向的人，另一个方向的人不能上
        Set<User> waitingUserSet = null;
        ReadWriteLock userSetLock = null;
        if (direction.equals(Direction.UP)) {
            waitingUserSet = waitingUpUserSet;
            userSetLock = upUserSetLock;
        } else if (direction.equals(Direction.DOWN)) {
            waitingUserSet = waitingDownUserSet;
            userSetLock = downUserSetLock;
        }
        //电梯剩余负载大于所有等候人数的情况，全上。否则只上随机的一部分
        if (waitingUserSet != null) {
            userSetLock.readLock().lock();
            if (num >= waitingUserSet.size()) {
                reduceSet.addAll(waitingUserSet);
                waitingUserSet.clear();
            } else {
                Iterator<User> iterator = waitingUserSet.iterator();
                while (reduceSet.size() < num) {
                    User next = iterator.next();
                    iterator.remove();
                    reduceSet.add(next);
                }
            }
            userSetLock.readLock().unlock();
        }
        return reduceSet;
    }

    /**
     * 取消某方向上的任务
     *
     * @param direction
     */
    public void cancel(Direction direction) {
        if (waitingDirectionMap.containsKey(direction)) {
            dispatcher.cancel(waitingDirectionMap.get(direction));
            waitingDirectionMapLock.writeLock().lock();
            waitingDirectionMap.remove(direction);
            waitingDirectionMapLock.writeLock().unlock();
        }
    }

    void done(Direction direction) {
        if (waitingDirectionMap.containsKey(direction)) {
            waitingDirectionMapLock.writeLock().lock();
            waitingDirectionMap.remove(direction);
            waitingDirectionMapLock.writeLock().unlock();
            //当电梯因为满载而无法全部把人带走时，继续产生新的任务
            Set<User> remainingUsers = direction.equals(Direction.UP) ? waitingUpUserSet : waitingDownUserSet;
            if (!remainingUsers.isEmpty()) {
                addDirectionTask(direction);
            }
        }
    }

    @Override
    public String toString() {
        return "Floor{" +
                "floorNo=" + floorNo +
                '}';
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Floor floor = (Floor) o;

        return floorNo == floor.floorNo;
    }

    @Override
    public int hashCode() {
        return floorNo;
    }

    public Direction locate(Floor targetFloor) {
        return floorNo > targetFloor.getFloorNo() ? Direction.UP : Direction.DOWN;
    }
}
