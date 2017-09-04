package biz.jared.domain;

import biz.jared.domain.enumeration.Direction;
import biz.jared.domain.enumeration.TaskStatus;

import java.util.Random;

/**
 * @author jared
 */
public class Task implements Comparable<Task>{
    private int id;
    /**
     * 产生此task的源楼层
     */
    private Floor srcFloor;
    /**
     * 这是一个想去什么方向的task？
     */
    private Direction direction;

    private TaskStatus status;

    private int priority;
    private Task(int id, Floor srcFloor) {
        this.id = id;
        this.srcFloor = srcFloor;
        this.status = TaskStatus.RUNNABLE;
    }

    private Task(int id, Floor srcFloor, Direction direction) {
        this(id, srcFloor);
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

    public Floor getSrcFloor() {
        return srcFloor;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", src floor=" + srcFloor.getFloorNo() +
                ", direction=" + direction +
                '}';
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(Task o) {
        return priority - o.priority;
    }
}
