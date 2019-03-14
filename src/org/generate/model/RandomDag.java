package org.generate.model;

import java.util.ArrayList;
import java.util.List;

import org.generate.DagBuilder;
import org.generate.util.CommonParametersUtil;

/**
 * 
 * @ClassName: Random_Dag
 * @Description: DAG����
 * @author YWJ
 * @date 2017-9-9 ����4:02:42
 */
public class RandomDag {
	public String dagId;
	public int dagLevel;// Dag�Ĳ���
	public int dagSize;// Dag�Ĵ�С
	public int levelCount;// ����ͳ��
	public int submitTime;// �ύʱ��
	public int deadlineTime;// �����Ҫ��ɵ�ʱ��
	public int[] levelNodeNumber;// �ӵڶ��㵽����ڶ�����ÿһ��������������
	public List<TaskNode> taskList;// ����ڵ��б�
	public List<DagEdge> edgeList;
	public List<TaskNode> lastLevelList;// ��һ��ڵ��б�
	public List<TaskNode> newLevelList;// ��һ��ڵ��б�
	public List<TaskNode> leafNodeList;// ��ǰҶ�ӽڵ�
	public boolean isSingle = false;
	public int[] color;

	/**
	 * 
	 * @Title: Random_Dag
	 * @Description: �������һ��DAG����ʱ����õĴ��롣���Ի�����һ������Ľڵ�
	 * 				 ֻ�����ɵ�һ��DAG��ҵ��ʱ������
	 * @param: @param dagId
	 * @throws
	 */
	public RandomDag(int dagId) {
		init(dagId);
		TaskNode root = new TaskNode("root_" + dagId, 0, 0, 0);
		taskList.add(root);
	//	System.out.println("�������ɵĵ�һ����ҵ������root��"+dagId+";��ǰ����������"+taskList.size());
		lastLevelList.add(root);
		leafNodeList.add(root);
		submitTime = 0;
	}

	public RandomDag(int dagId, TaskNode root, int lastDagTime) {
		init(dagId);
		taskList.add(root);
		//System.out.println("���Ǻ�����ҵ���ɵĵ�һ�����񡣺�����"+dagId+";��ǰ����������"+taskList.size());
		lastLevelList.add(root);
		leafNodeList.add(root);
		// ������ɵ�Dag���ύʱ��
		// ����һ��Dag�ύ��ʱ�䣬��ǰDag��ʼִ�е�ʱ�� ��֮���һ�����ֵ
		submitTime = DagBuilder.randomCreater.randomSubmitTime(lastDagTime,root.startTime);
	}

	/**
	 * 
	 * @Title: init
	 * @Description: ֻҪ����DAGͼ�ͻ���õĳ�ʼ�������ڹ��캯���е���
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

		// ���� ƽ��DAG������ �������ĳ DAGͼ��������
		// dagSize =
		// DagBuilder.randomCreater.randomDagSize(BuildParameters.dagAverageSize);

		/**
		 * ���õ�һ����ҵ��������������Ϊ1
		 */
		if(dagId==1){
			dagSize = DagBuilder.randomCreater.randomDagSize(CommonParametersUtil.dagAverageSize);
		}else{
			dagSize = DagBuilder.randomCreater.randomDagSizeWithSingle(CommonParametersUtil.dagAverageSize);
		}
		
		// �жϽڵ��Ƿ�Ϊ��һ�ڵ�
		if (dagSize == 1)
			isSingle = true;

		// ���ݴ��ж���������DAGͼ�Ĳ���
		dagLevel = DagBuilder.randomCreater.randomLevelNum(dagSize,CommonParametersUtil.dagLevelFlag);

		if(!isSingle){
			// ÿ����ж��ٸ�����,�±��Ǵ�0��ʼ�ģ�������������������������������
			levelNodeNumber = new int[dagLevel];
			// ������ɵڶ��㵽����ڶ����� ÿһ������������������ĿΪdagSize��
			// ��ʵ�������DAGͼ����Ϊ�������һ�������Կ����ǵڶ���͵����ڶ�
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

	//	System.out.println("dag:"+dagId+"dag�Ĳ���:"+dagLevel+" "+"���ڵĲ���:"+levelCount+";�����id��"+taskNode.nodeId);
		
		/**
		 * �����Ǳ����˵�
		 */
		if (newLevelList.size() == levelNodeNumber[levelCount - 1])// ����һ�������󣬱��һ��
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
	 * @Description: �����µĽڵ����List
	 * @return void
	 * @throws
	 */
	public void generateNode(TaskNode taskNode) {
		taskList.add(taskNode);
	}

	/**
	 * 
	 * @Title: generateEdge
	 * @Description:�����µı߼���List
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
	 * @Description: �ж�DAGͼ���Ƿ����ĳ����ڵ�
	 * @return boolean
	 * @throws
	 */
	public boolean containTaskNode(TaskNode taskNode) {
		return taskList.contains(taskNode);
	}

	/***
	 * 
	 * @Title: computeDeadLine
	 * @Description: ����DAGͼ�Ľ�ֹʱ�� ʱ���� �ύʱ��+����DAGͼִ��ʱ��*������������ǰĬ��Ϊ1.3��
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
	 * @Description: ��ӡDAGͼ
	 * @return void
	 * @throws
	 */
	public void printDag() {
		System.out.println(dagId + ":");
		for (TaskNode taskNode : taskList)
			System.out.print(taskNode.nodeId + " ");
		System.out.println();
		System.out.println("�ڵ���:" + taskList.size());
		System.out.println("��ʼ��������������"+dagSize+"----->�ڵ���:"+taskList.size());
//		for (DagEdge edge : edgeList)
//			edge.printEdge();
		System.out.println();
		System.out.println();
	}

}
