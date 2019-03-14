package org.generate.model;

import java.util.ArrayList;
import java.util.List;

import org.generate.DagBuilder;
import org.generate.util.CommonParametersUtil;

/**
 * 
 * @ClassName: Random_Dag
 * @Description: DAG对象
 * @author YWJ
 * @date 2017-9-9 下午4:02:42
 */
public class RandomDag {
	public String dagId;
	public int dagLevel;// Dag的层数
	public int dagSize;// Dag的大小
	public int levelCount;// 层数统计
	public int submitTime;// 提交时间
	public int deadlineTime;// 最迟需要完成的时间
	public int[] levelNodeNumber;// 从第二层到最倒数第二层中每一层的任务结点个数。
	public List<TaskNode> taskList;// 任务节点列表
	public List<DagEdge> edgeList;
	public List<TaskNode> lastLevelList;// 上一层节点列表
	public List<TaskNode> newLevelList;// 新一层节点列表
	public List<TaskNode> leafNodeList;// 当前叶子节点
	public boolean isSingle = false;
	public int[] color;

	/**
	 * 
	 * @Title: Random_Dag
	 * @Description: 本程序第一个DAG产生时会调用的代码。所以会生成一个多余的节点
	 * 				 只有生成第一个DAG作业的时候会调用
	 * @param: @param dagId
	 * @throws
	 */
	public RandomDag(int dagId) {
		init(dagId);
		TaskNode root = new TaskNode("root_" + dagId, 0, 0, 0);
		taskList.add(root);
	//	System.out.println("这是生成的第一个作业的任务。root："+dagId+";当前的任务数："+taskList.size());
		lastLevelList.add(root);
		leafNodeList.add(root);
		submitTime = 0;
	}

	public RandomDag(int dagId, TaskNode root, int lastDagTime) {
		init(dagId);
		taskList.add(root);
		//System.out.println("这是后续作业生成的第一个任务。后续："+dagId+";当前的任务数："+taskList.size());
		lastLevelList.add(root);
		leafNodeList.add(root);
		// 随机生成的Dag的提交时间
		// 【上一个Dag提交的时间，当前Dag开始执行的时间 】之间的一个随机值
		submitTime = DagBuilder.randomCreater.randomSubmitTime(lastDagTime,root.startTime);
	}

	/**
	 * 
	 * @Title: init
	 * @Description: 只要生成DAG图就会调用的初始方法，在构造函数中调用
	 * @return void
	 * @throws
	 */
	public void init(int dagId) {
		this.dagId = "dag" + dagId;
		taskList = new ArrayList<TaskNode>();
		edgeList = new ArrayList<DagEdge>();
		lastLevelList = new ArrayList<TaskNode>();
		leafNodeList = new ArrayList<TaskNode>();
		newLevelList = new ArrayList<TaskNode>();

		// 依据 平均DAG任务数 随机生成某 DAG图的任务数
		// dagSize =
		// DagBuilder.randomCreater.randomDagSize(BuildParameters.dagAverageSize);

		/**
		 * 设置第一个作业的任务数不可能为1
		 */
		if(dagId==1){
			dagSize = DagBuilder.randomCreater.randomDagSize(CommonParametersUtil.dagAverageSize);
		}else{
			dagSize = DagBuilder.randomCreater.randomDagSizeWithSingle(CommonParametersUtil.dagAverageSize);
		}
		
		// 判断节点是否为单一节点
		if (dagSize == 1)
			isSingle = true;

		// 依据串行度生成整个DAG图的层数
		dagLevel = DagBuilder.randomCreater.randomLevelNum(dagSize,CommonParametersUtil.dagLevelFlag);

		if(!isSingle){
			// 每层该有多少个任务,下标是从0开始的！！！！！！！！！！！！！！！！
			levelNodeNumber = new int[dagLevel];
			// 随机生成第二层到最倒数第二层中 每一层的任务结点个数。总数目为dagSize。
			// 其实就是这个DAG图，因为后来会归一化，所以可能是第二层和倒数第二
			DagBuilder.randomCreater.randomLevelSizes(levelNodeNumber, dagSize);
			levelCount = 1;
		}else{
			levelNodeNumber = new int[1];
			levelNodeNumber[0] =1;
			
			/**
			 * 
			 */
			levelCount = 1;
			
		}
	}

	public void addToNewLevel(TaskNode taskNode) {
		
		newLevelList.add(taskNode);

	//	System.out.println("dag:"+dagId+"dag的层数:"+dagLevel+" "+"现在的层数:"+levelCount+";任务的id："+taskNode.nodeId);
		
		/**
		 * 这里是报错了的
		 */
		if (newLevelList.size() == levelNodeNumber[levelCount - 1])// 当新一层填满后，变旧一层
		{
			levelCount++;
			lastLevelList.clear();
			lastLevelList.addAll(newLevelList);
			newLevelList.clear();
		}
	}

	/**
	 * 
	 * @Title: generateNode
	 * @Description: 生成新的节点加入List
	 * @return void
	 * @throws
	 */
	public void generateNode(TaskNode taskNode) {
		taskList.add(taskNode);
	}

	/**
	 * 
	 * @Title: generateEdge
	 * @Description:生成新的边加入List
	 * @return void
	 * @throws
	 */
	public void generateEdge(TaskNode head, TaskNode tail) {
		DagEdge dagEdge = new DagEdge(head, tail);
		edgeList.add(dagEdge);
	}

	/**
	 * 
	 * @Title: containTaskNode
	 * @Description: 判断DAG图中是否包含某任务节点
	 * @return boolean
	 * @throws
	 */
	public boolean containTaskNode(TaskNode taskNode) {
		return taskList.contains(taskNode);
	}

	/***
	 * 
	 * @Title: computeDeadLine
	 * @Description: 计算DAG图的截止时间 时间是 提交时间+整个DAG图执行时间*倍数参数（当前默认为1.3）
	 * @return void
	 * @throws
	 */
	public void computeDeadLine() {
		int proceesorEndTime = CommonParametersUtil.timeWindow/ CommonParametersUtil.processorNumber;
		deadlineTime = (int) (submitTime + (taskList.get(taskList.size() - 1).endTime - submitTime)
				* CommonParametersUtil.deadLineTimes);
		if (deadlineTime > proceesorEndTime)
			deadlineTime = proceesorEndTime;
	}

	/**
	 * 
	 * @Title: printDag
	 * @Description: 打印DAG图
	 * @return void
	 * @throws
	 */
	public void printDag() {
		System.out.println(dagId + ":");
		for (TaskNode taskNode : taskList)
			System.out.print(taskNode.nodeId + " ");
		System.out.println();
		System.out.println("节点数:" + taskList.size());
		System.out.println("初始给定的任务数："+dagSize+"----->节点数:"+taskList.size());
//		for (DagEdge edge : edgeList)
//			edge.printEdge();
		System.out.println();
		System.out.println();
	}

}
