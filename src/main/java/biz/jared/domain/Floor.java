package biz.jared.domain;

import biz.jared.domain.enumeration.Direction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * @author jared
 */
public class Floor {

    private int floorNo;
    private Floor preFloor,nextFloor;
    /**
     * 当前楼层等候人群
     */
    private Set<User> waitingUserSet = new HashSet<>(100);
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

    public Floor getNextFloor(Direction direction) {
        return Direction.UP.equals(direction) ? nextFloor : preFloor;
    }

    public void setPreFloor(Floor preFloor) {
        this.preFloor = preFloor;
    }

    /**
     * 楼层又来了人
     * @param user
     */
    public void add(User user, Direction direction){
        waitingUserSet.add(user);
        //只有之前没人说要去的方向才可以建任务，已经有人说要去的方向就不用再说一次了
        if (!waitingDirectionMap.containsKey(direction)) {
            Task task = Task.generate(this, direction);
            waitingDirectionMap.put(direction,task);
            dispatcher.receive(task);
        }
    }

    /**
     * 楼层可以减少num人
     * @param num 可以减少的人数
     * @return 减少的人集合
     */
    public Set<User> reduce(int num){
        Set<User> reduceSet;
        //电梯剩余负载大于所有等候人数的情况，全上。否则只上随机的一部分
        if (num >= waitingUserSet.size()) {
            reduceSet = waitingUserSet;
        } else {
            reduceSet = new HashSet<User>(num);
            Iterator<User> iterator = waitingUserSet.iterator();
            for (int i = 0; i < num; i++) {
                User next = iterator.next();
                waitingUserSet.remove(next);
                reduceSet.add(next);
            }
        }
        return reduceSet;
    }

    /**
     * 取消某方向上的任务
     * @param direction
     */
    public void cancel(Direction direction){
        if (waitingDirectionMap.containsKey(direction)) {
            dispatcher.cancel(waitingDirectionMap.get(direction));
            waitingDirectionMap.remove(direction);
        }
    }

    @Override
    public String toString() {
        return "Floor{" +
                "floorNo=" + floorNo +
                '}';
    }

    public Floor next(Floor nextFloor) {
        this.nextFloor = nextFloor;
        nextFloor.setPreFloor(this);
        return nextFloor;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        Floor floor = (Floor)o;

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
