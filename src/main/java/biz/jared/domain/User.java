package biz.jared.domain;

import biz.jared.domain.enumeration.Direction;

public class User {


    private String name;
    /**
     * 当前正在乘坐的电梯
     */
    private Elevator elevator;
    /**
     * 目标楼层
     */
    private Floor targetFloor;

    public User(String name, Floor targetFloor) {
        this.name = name;
        this.targetFloor = targetFloor;
    }

    public boolean cancel(Task task){
        return task.cancel();
    }

    public boolean select(Floor floor){
        Task task = Task.generate(floor);
        elevator.receive(task);
        return false;
    }

    public String getName() {
        return name;
    }

    public Floor getTargetFloor() {
        return targetFloor;
    }
}
