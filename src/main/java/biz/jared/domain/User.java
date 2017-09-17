package biz.jared.domain;

import biz.jared.Env;
import biz.jared.domain.enumeration.Direction;
import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jared
 */
public class User {

    private static final Logger LOGGER = LoggerFactory.getLogger(User.class);
    private String name;
    /**
     * 当前正在乘坐的电梯
     */
    private Elevator elevator;
    /**
     * 目标楼层
     */
    private Floor targetFloor;
    /**
     * 计时器对等待时间计时
     */
    private Stopwatch stopwatch = Stopwatch.createUnstarted();

    public User(String name, Floor targetFloor) {
        this.name = name;
        this.targetFloor = targetFloor;
    }

    /**
     * 用户选择了一个楼层
     *
     * @param targetFloor 目标楼层
     */
    void select(Floor targetFloor) {
        Task task = Task.generate(targetFloor, Direction.NONE);
        LOGGER.info("{} select {} create {}", this, targetFloor, task);
        elevator.receive(task);
    }

    void enterElevator(Elevator elevator) {
        //进入电梯
        this.elevator = elevator;
        //总等待时间增加
        long elapsed = stopwatch.stop().elapsed(Env.TIME_UNIT);
        Env.TOTAL_USER_WAIT_TIME.addAndGet(elapsed);
    }

    Floor getTargetFloor() {
        return targetFloor;
    }

    @Override
    public String toString() {
        return "User{" +
            "name='" + name + '\'' +
            ", targetFloor=" + targetFloor.getFloorNo() +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        User user = (User)o;
        return name.equals(user.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public Stopwatch getStopwatch() {
        return stopwatch;
    }
}
