package biz.jared.domain;

import biz.jared.domain.enumeration.Direction;

/**
 * @author jared
 */
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

    /**
     * 用户选择了一个楼层
     *
     * @param targetFloor 目标楼层
     */
    void select(Floor targetFloor) {
        Task task = Task.generate(targetFloor, Direction.SAME);
        System.out.println(this+" select " + targetFloor + " create "+ task);
        elevator.receive(task);
    }

    void enterElevator(Elevator elevator) {
        this.elevator = elevator;
    }

    Floor getTargetFloor() {
        return targetFloor;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", targeFloor=" + targetFloor.getFloorNo() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        User user = (User)o;

        return name.equals(user.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
