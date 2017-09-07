package biz.jared.strategy;

import biz.jared.domain.Elevator;
import biz.jared.domain.Task;

/**
 * 任务相对于电梯的权重计算策略
 *
 * @author jared
 */
public interface PriorityCalculationStrategy {
    /**
     * 计算权重的逻辑
     *
     * @param elevator
     * @param task
     * @return
     */
    int calcPriority(Elevator elevator, Task task);
}
