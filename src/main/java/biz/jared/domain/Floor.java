package biz.jared.domain;

import biz.jared.domain.enumeration.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
     * 已经有人表达说要去的方向集合
     */
    private Map<Direction, Task> waitingDirectionMap = new HashMap<>(Direction.values().length);
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
        if (direction.equals(Direction.UP)) {
            waitingUpUserSet.add(user);
        } else if (direction.equals(Direction.DOWN)) {
            waitingDownUserSet.add(user);
        }
        //给楼层加当前方向上的任务
        addDirectionTask(direction);
    }

    private void addDirectionTask(Direction direction) {
        //只有之前没人说要去的方向才可以建任务，已经有人说要去的方向就不用再说一次了
        if (!waitingDirectionMap.containsKey(direction)) {
            Task task = Task.generate(this, direction);
            waitingDirectionMap.put(direction, task);
            dispatcher.dispatch(task);
        }
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
        if (direction.equals(Direction.UP)) {
            waitingUserSet = waitingUpUserSet;
        } else if (direction.equals(Direction.DOWN)) {
            waitingUserSet = waitingDownUserSet;
        }
        //电梯剩余负载大于所有等候人数的情况，全上。否则只上随机的一部分
        if (waitingUserSet != null) {
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
            waitingDirectionMap.remove(direction);
        }
    }

    void done(Direction direction) {
        if (waitingDirectionMap.containsKey(direction)) {
            waitingDirectionMap.remove(direction);
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
