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

    private static final int MAX_PRIORITY = 2 * Env.FLOOR_NUM;

    /**
     * x = 任务所处楼层号
     * y = 电梯所处楼层号
     * e = 总楼层数
     * 电梯向上走时的计算逻辑：
     * 1、同相、顺路 -> p = x - y
     * 2、同相、不顺路 -> p = 2 * e - x + y
     * 3、不同相 -> p = 2 * e - x - y
     *
     * 电梯向下走时的计算逻辑：
     * 1、同相、顺路 -> p = y - x
     * 2、同相、不顺路 -> p = 2 * e - x + y
     * 3、不同相 -> p = x + y
     *
     * @param elevator 参与计算的电梯
     * @param task     参与计算的任务
     * @return 任务优先级
     */
    @Override
    public int calcPriority(Elevator elevator, Task task) {
        int x = task.getSrcFloor().getFloorNo();
        int y = elevator.getCurrFloor().getFloorNo();
        int priority;
        boolean isSameDirection = Calc.isSameDirection(elevator, task);
        boolean isOnTheWay;

        switch (elevator.getStatus()) {
            case RUNNING_UP:
                isOnTheWay = x > y;
                if (isOnTheWay && isSameDirection) {
                    priority = x - y;
                } else if (!isOnTheWay && isSameDirection) {
                    priority = MAX_PRIORITY - x + y;
                } else {
                    priority = MAX_PRIORITY - x - y;
                }
                break;
            case RUNNING_DOWN:
                isOnTheWay = y > x;
                if (isOnTheWay && isSameDirection) {
                    priority = y - x;
                } else if (!isOnTheWay && isSameDirection) {
                    priority = MAX_PRIORITY - x + y;
                } else {
                    priority = x + y;
                }
                break;
            case IDLE:
                priority = Math.abs(task.getSrcFloor().getFloorNo() - elevator.getCurrFloor().getFloorNo());
                break;
            default:
                throw new IllegalArgumentException();
        }
        //priority已经是2倍楼层总数了，优先级要循环
        if (priority == MAX_PRIORITY) {
            priority = 0;
        }
        return priority;
    }
}
