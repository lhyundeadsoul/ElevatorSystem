package biz.jared;

import biz.jared.domain.Elevator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 系统运行依赖的环境参数
 *
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
    static final int ELEVATOR_NUM = 2;
    /**
     * 最大负载人数
     */
    public static final int MAX_LOAD = 2;
}
