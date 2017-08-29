package biz.jared.domain;

import biz.jared.domain.enumeration.Direction;
import biz.jared.domain.enumeration.Status;

/**
 * @author jared
 */
public class Task {
    private Floor floor;
    private Direction direction;

    private Status status;
    public Task(Floor floor) {
        this.floor = floor;
        this.status = Status.RUNNABLE;
    }

    public Task(Floor floor, Direction direction) {
        this.floor = floor;
        this.direction = direction;
        this.status = Status.RUNNABLE;
    }
    public static Task generate(Floor floor, Direction direction) {
        return new Task(floor, direction);
    }

    /**
     * 只要不是running状态即可取消
     * @return
     */
    public boolean cancel() {
        synchronized (status){
            if (!Status.RUNNING.equals(status)) {
                status = Status.CANCELLED;
                return true;
            } else {
                return false;
            }
        }
    }

    public static Task generate(Floor floor) {
        return new Task(floor);
    }

    public Floor getFloor() {
        return floor;
    }

    public Direction getDirection() {
        return direction;
    }

    public Status getStatus() {
        return status;
    }
}
