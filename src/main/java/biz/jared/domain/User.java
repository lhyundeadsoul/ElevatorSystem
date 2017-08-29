package biz.jared.domain;

import biz.jared.domain.enumeration.Direction;

public class User {


    private String name;
    private Floor floor;

    public User(String name, Floor floor) {
        this.name = name;
        this.floor = floor;
    }

    public Task use(Direction direction){
        return Task.generate(getFloor(), direction);
    }

    public boolean cancel(Task task){
        return task.cancel();
    }

    public boolean select(Floor floor){
        //todo
        return false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Floor getFloor() {
        return floor;
    }

    public void setFloor(Floor floor) {
        this.floor = floor;
    }
}
