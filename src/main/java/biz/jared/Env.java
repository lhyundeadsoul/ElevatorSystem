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
    public static final int FLOOR_NUM = 10;
    /**
     * 电梯数
     */
    public static final int ELEVATOR_NUM = 1;
}
