package biz.jared.strategy;

import biz.jared.Calc;
import biz.jared.Env;
import biz.jared.domain.Elevator;
import biz.jared.domain.Task;

/**
 * 同相距离最近优先策略
 *
 * @author jared
 * @date 2017/09/05
 */
public class SameDirectionNearestFirstPriorityStrategy implements PriorityCalculationStrategy {

    /**
     * x = 任务所处楼层号
     * y = 电梯所处楼层号
     * e = 总楼层数
     * 计算逻辑：
     *  1、同相、顺路 -> p = x-y
     *  2、同相、不顺路 -> p = 2 * e - x + y
     *  3、不同相 -> p = 2 * e - x - y
     * @param elevator
     * @param task
     * @return
     */
    @Override
    public int calcPriority(Elevator elevator, Task task) {
        int x = task.getSrcFloor().getFloorNo();
        int y = elevator.getCurrFloor().getFloorNo();
        int priority;
        boolean isSameDirection = Calc.isSameDirection(elevator, task);
        boolean isOnTheWay = x - y > 0;
        if (isOnTheWay && isSameDirection) {
            priority = x - y;
        } else if (!isOnTheWay && isSameDirection) {
            priority = 2 * Env.FLOOR_NUM - x + y;
        } else {
            priority = 2 * Env.FLOOR_NUM - x - y;
        }
        return priority;
    }
}
