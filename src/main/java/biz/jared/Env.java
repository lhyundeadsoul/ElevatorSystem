package biz.jared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 系统运行依赖的环境参数
 *
 * @author jared
 * @date 2017/09/06
 */
public class Env {
    //============电梯运行的环境参数============
    /**
     * 总楼层数
     */
    public static final int FLOOR_NUM = 30;
    /**
     * 最大负载人数
     */
    public static final int MAX_LOAD = 2;
    /**
     * 电梯数
     */
    static final int ELEVATOR_NUM = 2;
    /**
     * 总用户数
     */
    static final int USER_NUM = 22;
    /**
     * 时间单位
     */
    public static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
    /**
     * 所有操作需要流逝的时间长度
     */
    public static final int ELAPSED_TIME = 10;

    //============电梯运行效率的评价指标============
    /**
     * 用户总等待时间
     */
    public static AtomicLong TOTAL_USER_WAIT_TIME = new AtomicLong();
    /**
     * 电梯运行总里程
     */
    public static AtomicLong TOTAL_ELEVATOR_MOVE_DISTANCE = new AtomicLong();

    //============其它============

    public static CountDownLatch LATCH = new CountDownLatch(ELEVATOR_NUM);
    private static final Logger LOGGER = LoggerFactory.getLogger(Env.class);

    static void show() {
        LOGGER.info("average wait time {}", TOTAL_USER_WAIT_TIME.get() / (double) USER_NUM);
        LOGGER.info("average elevator move distance {}", TOTAL_ELEVATOR_MOVE_DISTANCE.get() / (double) ELEVATOR_NUM);
    }

    public static void elapsed() {
        try {
            TIME_UNIT.sleep(ELAPSED_TIME);
        } catch (InterruptedException e) {
            LOGGER.error("elapsed timeout");
        }
    }
}
