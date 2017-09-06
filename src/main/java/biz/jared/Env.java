package biz.jared;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import biz.jared.domain.Task;
import biz.jared.domain.enumeration.Direction;

/**
 * @author jared
 * @date 2017/09/06
 */
public class Env {
    /**
     * 总楼层数
     */
    public static final int FLOOR_NUM = 5;
    /**
     * 电梯数
     */
    public static final int ELEVATOR_NUM = 1;
    public static void main(String[] args) throws InterruptedException {
        Task t1 = Task.generate(null, Direction.UP);
        t1.setPriority(12);
        Task t2 = Task.generate(null, Direction.DOWN);
        t2.setPriority(12);
        BlockingQueue<Task> b = new PriorityBlockingQueue();
        b.put(t2);
        b.put(t1);
        System.out.println(b.take().getDirection());
        System.out.println(b.take().getDirection());
    }
}
