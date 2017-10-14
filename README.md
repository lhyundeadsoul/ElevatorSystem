[![Build Status](https://travis-ci.org/lhyundeadsoul/ElevatorSystem.svg?branch=master)](https://travis-ci.org/lhyundeadsoul/ElevatorSystem)
[![CircleCI](https://circleci.com/gh/lhyundeadsoul/ElevatorSystem.svg?style=svg)](https://circleci.com/gh/lhyundeadsoul/ElevatorSystem)
[![codebeat badge](https://codebeat.co/badges/8863dd20-d5c9-4191-9825-2f86a27b449c)](https://codebeat.co/projects/github-com-lhyundeadsoul-elevatorsystem-master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/9084b4d0243b4e8c8314168ed2a7deda)](https://www.codacy.com/app/lhyundeadsoul/ElevatorSystem?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=lhyundeadsoul/ElevatorSystem&amp;utm_campaign=Badge_Grade)
![](https://img.shields.io/badge/language-java-blue.svg)
![](https://img.shields.io/github/issues/lhyundeadsoul/ElevatorSystem.svg)
![](https://img.shields.io/github/forks/lhyundeadsoul/ElevatorSystem.svg)
![](https://img.shields.io/github/stars/lhyundeadsoul/ElevatorSystem.svg)
# ElevatorSystem
a Multi-elevator System

## Design
![arch](src/main/resources/ElevatorSystem.png)

# Run it
```$xslt
mvn clean -DskipTests=true package
nohup java -jar ~/Sources/ElevatorSystem/target/elevator-system-1.0-SNAPSHOT-jar-with-dependencies.jar {dispatch-strategy} {priority-strategy} >/dev/null 2>&1 &
```
> dispatch-strategy = RandomDispatch / PriorityFirstDispatch
 
> priority-strategy = SameDirectionNearestFirst

## Feature
1. support multi-task/multi-elevator dispatch
2. support elevator maximum load
3. support most of operations of elevator e.g. grab/cancel/abandon
4. support dispatch strategy extensible endpoint

## Legacy Problem
- [x] 电梯来了，和用户方向不同，用户也不能上
- [x] dispatcher任务分配优先级还没排查
- [x] 电梯任务抢占
- [x] todo
- [x] UT
- [x] CI
- [x] 全部电梯满载时会死循环
- [x] 加入吞吐、平均等待时间等指标
- [x] RejectedExecutionException
- [x] 梳理所有竞争代码块，检查是否有未处理的情况
- [ ] 尝试设计新的strategy
- [x] 系统开发中遇到的细节问题总结梳理

## 遇到的细节问题总结
*  整体设计
	*  任务怎么分配和分配后放在什么优先级执行是两回事
	*  多线程下，某状态有读取操作并要依据读取结果做后续逻辑走向的决策，这时要做读写互斥的状态保护
		*  电梯要保护的状态：当前运行状态、当前任务、当前负载、当前楼层
		*  dispatcher要保护的状态：电梯列表
		*  楼层要保护的状态：上/下行的等候队列、已经产生的任务map
	* 判断任务与电梯是否同方向时，要考虑电梯idle状态和任务方向None，这两种特殊情况
	* 计算任务优先级时，同相距离最近优先策略，计算逻辑要考虑电梯运动方向、是否同向以及是否顺路三个维度确定计算方法
		  * x = 任务所处楼层号
	     * y = 电梯所处楼层号
	     * e = 总楼层数
		* 电梯向上走时的计算逻辑：
	     * 1、同向、顺路 -> p = x - y
	     * 2、同向、不顺路 -> p = 2 * e - y + x
	     * 3、不同向 -> p = 2 * e - x - y
	   * 电梯向下走时的计算逻辑：
	     * 1、同向、顺路 -> p = y - x
	     * 2、同向、不顺路 -> p = 2 * e - x + y
	     * 3、不同向 -> p = x + y
	   * 电梯idle时:Math.abs(x - y)
* 任务分配器
	*  分配任务时，不能给已经满载的电梯分配任务
	*  分配任务要异步顺序进行，以免在分配不出电梯时，后面的人也没法产生任务了
* 楼层
	*  楼层向上和向下走是两个不同的等待队列，不能因为都在同一层就认为只有一个等待队列
	*  一个楼层的一个方向不管几人等候，只产生一个任务即可
	*  当电梯因为满载而无法全部把人全带走时，要为剩下的人继续产生新的任务
*  电梯
	*  接收新任务前，要刷新所有任务的优先级
	*  判断当前任务是否可以被抢占时，要重新计算当前任务的优先级，并和当前收到的新任务进行比较
	*  电梯的行为可以归纳为：从任务队列取任务，然后执行它（走到任务所在的楼层，卸载再装载），然后idle
	*  电梯可以放弃执行当前任务的情况有三种：当前满载、楼层产生的任务在前往的过程中被抢占、任务被取消
	*  电梯内用户任务被抢占，只能还是当前电梯处理其任务，和楼层产生任务被抢占处理逻辑不同
	*  一定要先改变电梯的当前楼层，再楼层移动耗时。原因：当电梯门关上后，刚刚开始启动，这时即使还没到下一层楼，也要按下一层楼算了，因为当前楼层已经没机会上了，这和现实也是符合的
* 任务
	* 任务的方向除了向上走，向下走，还有“到地就停”（Direction.None）
	* 任务被抢占后，要有让出电梯的行为
