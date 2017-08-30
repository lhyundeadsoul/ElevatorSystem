package biz.jared.domain;

import biz.jared.domain.enumeration.Direction;
import biz.jared.domain.enumeration.TaskStatus;

import java.util.Random;

/**
 * @author jared
 */
public class Task {
    private int id;
    /**
     * 产生此task的源楼层
     */
    private Floor floor;
    /**
     * 这是一个想去什么方向的task？
     */
    private Direction direction;

    private TaskStatus status;

    public Task(int id, Floor floor) {
        this.id = id;
        this.floor = floor;
        this.status = TaskStatus.RUNNABLE;
    }

    public Task(int id, Floor floor, Direction direction) {
        this(id, floor);
        this.direction = direction;
    }

    /**
     * 楼层产生任务
     * @param floor
     * @param direction
     * @return
     */
    public static Task generate(Floor floor, Direction direction) {
        return new Task(new Random().nextInt(10000), floor, direction);
    }

    /**
     * 用户产生任务
     * @param floor
     * @return
     */
    public static Task generate(Floor floor) {
        return new Task(new Random().nextInt(10000), floor);
    }

    /**
     * 只要不是running状态即可改为取消状态，取消状态的task不会被执行
     *
     * @return 取消成功与否，成功取消 true
     */
    public boolean cancel() {
        synchronized (status) {
            if (!TaskStatus.RUNNING.equals(status)) {
                status = TaskStatus.CANCELLED;
                System.out.println(this + " cancelled");
                return true;
            } else {
                return false;
            }
        }
    }

    public Floor getFloor() {
        return floor;
    }

    public Direction getDirection() {
        return direction;
    }

    public TaskStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", floor=" + floor.getFloorNo() +
                ", direction=" + direction +
                '}';
    }
}
