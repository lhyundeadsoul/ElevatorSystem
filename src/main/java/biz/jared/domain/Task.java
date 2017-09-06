package biz.jared.domain;

import biz.jared.domain.enumeration.Direction;
import biz.jared.domain.enumeration.TaskStatus;

import java.util.Random;

/**
 * @author jared
 */
public class Task implements Comparable<Task> {
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
        setStatus(TaskStatus.RUNNABLE);
    }

    private Task(int id, Floor srcFloor, Direction direction) {
        this(id, srcFloor);
        this.direction = direction;
    }

    /**
     * 楼层产生任务
     *
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
    void cancel() {
        setStatus(TaskStatus.CANCELLED);
        System.out.println(this + " cancelled");
    }

    public Floor getSrcFloor() {
        return srcFloor;
    }

    TaskStatus getStatus() {
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

    void setStatus(TaskStatus status) {
        this.status = status;
    }

    @Override
    public int compareTo(Task o) {
        if (o == null) {
            return 1;
        }
        return priority - o.priority;
    }

    /**
     * 当前任务优先级是否更高
     * @param task 被比较的任务
     * @return true when current task's priority is higher
     */
    boolean isPriorityHigher(Task task) {
        return compareTo(task) < 0;
    }

    /**
     * 正在执行的任务要让出电梯（状态改为RUNNABLE，电梯在move的过程中会发现并抛弃此任务）
     */
    void yield(){
        if (getStatus().equals(TaskStatus.RUNNING)){
            setStatus(TaskStatus.RUNNABLE);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        Task task = (Task)o;

        if (srcFloor != null ? !srcFloor.equals(task.srcFloor) : task.srcFloor != null) { return false; }
        return direction == task.direction;
    }

    @Override
    public int hashCode() {
        int result = srcFloor != null ? srcFloor.hashCode() : 0;
        result = 31 * result + (direction != null ? direction.hashCode() : 0);
        return result;
    }
}
