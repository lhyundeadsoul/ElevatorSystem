package biz.jared.domain;

import biz.jared.domain.enumeration.Direction;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author jared
 */
public class Floor {
    private int floorNo;
    /**
     * 当前楼层等候人群
     */
    private Set<User> waitingUserSet = new HashSet<User>(100);
    /**
     * 已经有人表达说要去的方向集合
     */
    private Set<Direction> waitingDirectionSet = new HashSet<Direction>(2);
    /**
     * 整个电梯的调度
     */
    private Dispatcher dispatcher;

    public Floor(int floorNo, Dispatcher dispatcher) {
        this.floorNo = floorNo;
        this.dispatcher = dispatcher;
    }

    public int getFloorNo() {
        return floorNo;
    }

    /**
     * 楼层又来了人
     * @param user
     */
    public void add(User user, Direction direction){
        waitingUserSet.add(user);
        Task task = Task.generate(this, direction);
        if (!waitingDirectionSet.contains(direction)) {//只有之前没人说要去的方向才可以建任务，已经有人说要去的方向就不用再说一次了
            waitingDirectionSet.add(direction);
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

    @Override
    public String toString() {
        return "Floor{" +
                "floorNo=" + floorNo +
                '}';
    }
}
