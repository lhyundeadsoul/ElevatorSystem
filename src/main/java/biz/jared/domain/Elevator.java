package biz.jared.domain;

import biz.jared.Calc;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author jared
 */
public class Elevator implements Runnable {
    private int id;
    /**
     * 电梯的任务列表
     */
    private BlockingQueue<Task> taskQueue = new PriorityBlockingQueue<Task>();
    /**
     * 当前所处楼层
     */
    private Floor floor;
    /**
     * 电梯负载人群
     */
    private Set<User> userLoad = new HashSet<>(MAX_LOAD);
    /**
     * 最大负载人数
     */
    private static final int MAX_LOAD = 10;

    public Elevator(int id, Floor initFloor) {
        this.id = id;
        this.floor = initFloor;
    }

    public void receive(Task task) {
        taskQueue.add(task);
    }

    /**
     * 电梯运行逻辑
     */
    public void run() {
        while (true) {

            try {
                Task task = taskQueue.take();
                //1. go floor
                go(task.getFloor());
                //2. unload user
                unload();
                //3. load user
                load();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void load() {
        Set<User> reduceSet = floor.reduce(MAX_LOAD - userLoad.size());
        userLoad.addAll(reduceSet);
        reduceSet.forEach(user -> user.select(user.getTargetFloor()));
    }

    private void unload() {
        userLoad.removeAll(
                userLoad.stream()
                        .filter(user -> user.getTargetFloor().equals(floor))
                        .collect(Collectors.toSet())
        );
    }

    /**
     * 去到某楼层
     * @param floor
     */
    private void go(Floor floor) {
        for (int i = 0; i < Calc.calcAbsDistance(this.floor, floor); i++) {
            try {
                TimeUnit.SECONDS.sleep(1);
                System.out.println(this);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        return "Elevator{" +
                "floor=" + floor +
                '}';
    }
}
