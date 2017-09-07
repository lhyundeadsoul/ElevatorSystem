package biz.jared.strategy;

import java.util.List;

import biz.jared.domain.Elevator;
import biz.jared.domain.Task;

/**
 * 任务分配策略
 *
 * @author jared
 */
public interface DispatchStrategy {
    /**
     * 新任务产生待分配时的任务分配逻辑
     *
     * @param elevatorList 备选电梯集合
     * @param task         负载任务
     * @return 被选电梯
     */
    Elevator select(List<Elevator> elevatorList, Task task);

}
