package biz.jared;

import biz.jared.domain.Floor;
import biz.jared.domain.enumeration.Direction;
import biz.jared.domain.enumeration.ElevatorStatus;

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

    public static boolean isSameDirection(ElevatorStatus status, Direction direction) {
        return status.equals(ElevatorStatus.RUNNING_UP) && direction.equals(Direction.UP)
            || status.equals(ElevatorStatus.RUNNING_DOWN) && direction.equals(Direction.DOWN);
    }
}
