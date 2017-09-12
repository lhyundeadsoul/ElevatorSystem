package biz.jared.domain;

import java.util.Random;

import biz.jared.domain.enumeration.Direction;
import biz.jared.domain.enumeration.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jared
 */
public class Task implements Comparable<Task> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Task.class);
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

    private Task(int id, Floor srcFloor, Direction direction) {
        this.id = id;
        this.srcFloor = srcFloor;
        setStatus(TaskStatus.RUNNABLE);
        this.direction = direction;
    }

    /**
     * 楼层产生任务
     *
     * @param floor
     * @param direction
     * @return
     */
    static Task generate(Floor floor, Direction direction) {
        return new Task(new Random().nextInt(10000), floor, direction);
    }

    /**
     * 只要不是running状态即可改为取消状态，取消状态的task不会被执行
     *
     * @return 取消成功与否，成功取消 true
     */
    void cancel() {
        setStatus(TaskStatus.CANCELLED);
        LOGGER.info("{} cancelled", this);
    }

    public Floor getSrcFloor() {
        return srcFloor;
    }

    TaskStatus getStatus() {
        return status;
    }

    void setStatus(TaskStatus status) {
        this.status = status;
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

    void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int compareTo(Task o) {
        if (o == null) {
            return -1;
        }
        return priority - o.priority;
    }

    /**
     * 当前任务优先级是否更高
     *
     * @param task
     * @return true when current task's priority is higher
     */
    boolean isPriorityHigherThan(Task task) {
        return compareTo(task) < 0;
    }

    /**
     * 正在执行的任务要让出电梯（状态改为RUNNABLE，电梯在move的过程中会发现并抛弃此任务）
     */
    void yield() {
        if (getStatus().equals(TaskStatus.RUNNING)) {
            setStatus(TaskStatus.RUNNABLE);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Task task = (Task)o;
        //NONE方向的任务和所有方向的任务都equal
        return (srcFloor != null ? srcFloor.equals(task.srcFloor) : task.srcFloor == null)
            && (direction.equals(task.direction) || direction.equals(Direction.NONE) || task.direction.equals(
            Direction.NONE));
    }

    @Override
    public int hashCode() {
        int result = srcFloor != null ? srcFloor.hashCode() : 0;
        result = 31 * result + (direction != null ? direction.hashCode() : 0);
        return result;

    }
}
