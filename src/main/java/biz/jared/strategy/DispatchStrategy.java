package biz.jared.strategy;

import biz.jared.domain.Elevator;
import biz.jared.domain.Task;

import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * 任务分配策略
 */
public interface DispatchStrategy {
    /**
     * 新任务产生待分配时的任务分配逻辑
     * @param elevatorList 备选电梯集合
     * @param task 负载任务
     * @return 被选电梯
     */
    Elevator select(List<Elevator> elevatorList, Task task);

    /**
     * 在一个电梯集合里，某一电梯来索要任务时的任务分配逻辑
     * @param elevatorList 电梯集合
     * @param elevator 索要任务的电梯
     * @param taskQueue 任务集合
     * @return 分配的任务
     */
    Task select(List<Elevator> elevatorList, Elevator elevator, BlockingQueue<Task> taskQueue);
}
