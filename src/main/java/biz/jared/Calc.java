package biz.jared;

import biz.jared.domain.Elevator;
import biz.jared.domain.Floor;
import biz.jared.domain.Task;
import biz.jared.domain.enumeration.Direction;
import biz.jared.domain.enumeration.ElevatorStatus;

/**
 * @author jared
 */
public class Calc {
    /**
     * 两个楼层的相对距离，可以为负
     *
     * @param src
     * @param target
     * @return src - target
     */
    public static int calcDistance(Floor src, Floor target) {
        return src.getFloorNo() - target.getFloorNo();
    }

    /**
     * 是否任务和电梯同相
     * 因为用户一定会选一个与电梯相同方向的任务，所以任务方向是NONE也算
     * 电梯idle和所有方向算同相
     *
     * @param elevator
     * @param task
     * @return
     */
    public static boolean isSameDirection(Elevator elevator, Task task) {
        return (elevator.getStatus().equals(ElevatorStatus.RUNNING_UP) && task.getDirection().equals(Direction.UP))
            || (elevator.getStatus().equals(ElevatorStatus.RUNNING_DOWN) && task.getDirection().equals(Direction.DOWN))
            || task.getDirection().equals(Direction.NONE)
            || elevator.getStatus().equals(ElevatorStatus.IDLE);
    }

}
