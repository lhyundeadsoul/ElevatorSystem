package biz.jared.strategy;

import biz.jared.Calc;
import biz.jared.domain.Elevator;
import biz.jared.domain.Task;

/**
 * @author jared
 * @date 2017/09/05
 */
public class DistanceShortPriorityStrategy implements PriorityCalculationStrategy {

    /**
     * 不同方向的任务，其优先值相差的阈值
     */
    private static final int DIFFERENT_DIRECTION_PRIORITY_THRESHOLD = 100;

    /**
     * 计算任务在当前电梯任务队列里的优先级：相对距离取绝对值，越小越优先执行，相反方向的要用一个大阈值去减
     * @param elevator
     * @param task
     * @return
     */
    @Override
    public int calcPriority(Elevator elevator, Task task) {
        int relativeDistance = Math.abs(Calc.calcDistance(task.getSrcFloor(), elevator.getCurrFloor()));
        return Calc.isSameDirection(elevator.getStatus(), task.getDirection()) ? relativeDistance
            : DIFFERENT_DIRECTION_PRIORITY_THRESHOLD - relativeDistance;
    }
}
