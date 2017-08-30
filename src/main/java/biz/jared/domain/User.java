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

    /**
     * 用户选择了一个楼层
     * @param floor 目标楼层
     */
    void select(Floor floor){
        Task task = Task.generate(floor);
        elevator.receive(task);
    }

    Floor getTargetFloor() {
        return targetFloor;
    }

    @Override
    public String toString() {
        return "User{" +
            "name='" + name + '\'' +
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
