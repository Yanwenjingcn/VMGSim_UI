package org.generate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.generate.model.DagEdge;
import org.generate.model.Processor;
import org.generate.model.RandomDag;
import org.generate.model.TaskNode;
import org.generate.util.CommonParametersUtil;
import org.generate.util.RandomParametersUtil;
import org.generate.util.XMLOutputUtil;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.schedule.model.Task;
import org.schedule.model.DAG;
import org.schedule.model.DAGdepend;
import org.schedule.model.PEComputerability;

public class DagBuilder {

	private int endNodeNumber = 0;// DAGͼ��β�ڵ���

	public static int endTime = 100 * 10000;

	public static RandomParametersUtil randomCreater;

	public List<TaskNode> unCompleteTaskList;// δ��������б�

	public static List<RandomDag> dagList;// dag�б�

	public static List<RandomDag> finishDagList;// ������ɵ�Dag�б�

	public List<String> endNodeList;// �����ڵ��б�
	
	

	private String pathXML;

	
	//=================================
	private static ArrayList<Task> DAG_queue_personal;
	private static LinkedHashMap<Integer, Integer> DAGDependMap_personal;
	private static LinkedHashMap<String, Double> DAGDependValueMap_personal;
	private static ArrayList<DAG> DAGMapList;
	/**
	 * 
	 * @Title: createProcessor
	 * @Description: ��ʼ��������������Ĭ�ϵĴ���������ʱ��Ϊ ��ʱ�䴰��ʱ��/����������
	 * @return List<Processor>
	 * @throws
	 */
	
	
	
	public DagBuilder(String pathXML){
		DAG_queue_personal = new ArrayList<Task>();
		DAGMapList = new ArrayList<DAG>();

		this.pathXML = pathXML;
	}

	
	
	public List<Processor> createProcessor(int number, int endTime) {
		List<Processor> processorList = new ArrayList<Processor>();
		for (int i = 1; i <= number; i++) {
			Processor processor = new Processor(i, endTime);
			processorList.add(processor);
		}
		return processorList;
	}


	/**
	 * ��ӡ�ڵ���Ϣ
	 * 
	 * @param processorList
	 *            �������б�
	 */
	public void printNodes(List<Processor> processorList) {
		for (Processor processor : processorList) {
			System.out.print(processor.processorId + ":"+ processor.nodeList.size() + " ");
			System.out.println();
		}
	}

	/**
	 * 
	 * @Title: initList
	 * @Description: ��ʼ�� public List<String> endNodeList;//�����ڵ��б� ��ʼ�� public
	 *               List<TaskNode> unCompleteTaskList;//δ��������б�
	 * @return void
	 * @throws
	 */
	public void initList(List<Processor> processorList) {
		// ��ʼ��δ��ɵ��б�
		int maxsize = 0;
		// ��ȡ�������г�ʼ������������Ŀmaxsize
		for (Processor processor : processorList) {
			int size = processor.nodeList.size();
			if (size > maxsize)
				maxsize = size;
		}

		// �����д������ϵ����񶼼����� δ�����б���
		// ��ʼ�� public List<TaskNode> unCompleteTaskList;//δ��������б�
		for (int i = 0; i < maxsize; i++)
			for (Processor processor : processorList) {
				if (i < processor.nodeList.size())
					unCompleteTaskList.add(processor.nodeList.get(i));
			}

		/**
		 * ��ӡ��ʼʱ�����˶��ٸ�����
		 */
		// System.out.println("unCompleteTaskList�Ĵ�СΪ="+unCompleteTaskList.size());

		// ��ȡ���д������ϵ����һ�����
		// ��ʼ�� public List<String> endNodeList;//�����ڵ��б�
		for (Processor processor : processorList) {
			String endNodeId = processor.nodeList
					.get(processor.nodeList.size() - 1).nodeId;
			endNodeList.add(endNodeId);
		}
	}

	/**
	 * 
	 * @Title: isEndNode
	 * @Description: �ж�ĳ���ڵ��Ƿ��ǳ�ʼ����������ʱ���������սڵ�
	 * @return boolean
	 * @throws
	 */
	public boolean isEndNode(String taskId) {
		for (String endNodeId : endNodeList)
			if (taskId.equals(endNodeId))
				return true;
		return false;
	}

	/**
	 * 
	 * @Title: tryFinishDag
	 * @Description: �����ж�ĳ���ڵ��ܷ��ΪĳDAGͼ�����սڵ㣬���Է���TRUE
	 * @return boolean
	 * @throws
	 */
	public boolean tryFinishDag(TaskNode taskNode, RandomDag dag) {
		int m = 0;
		for (TaskNode leafTask : dag.leafNodeList)
			if (taskNode.startTime >= leafTask.endTime) {
				if (taskNode.startTime == leafTask.endTime)// ���ʱ���Ϊ0,�ж��Ƿ���ͬһ����������
				{
					if (taskNode.getProcessorId() != leafTask.getProcessorId())
						continue;
				}
				m++;
			}

		// �Ƿ�ýڵ�����Ϊ���DAGͼ�����սڵ㣿
		if (m == dag.leafNodeList.size())// ���Թ���������Dagͼ
		{
			dag.generateNode(taskNode);
			//System.out.println("��ǰ�ڵ�"+taskNode.nodeId+";�ܹ�������ҵDAG��"+dag.dagId+"����������"+dag.taskList.size());
			for (TaskNode leafTask : dag.leafNodeList)
				dag.generateEdge(leafTask, taskNode);
			dagList.remove(dag);
			finishDagList.add(dag);// �ŵ�����б���ȥ
			return true;
		}

		return false;
	}

	/**
	 * 
	 * @Title: searchParentNode
	 * @Description: Ϊ unCompleteTaskList �е�ÿһ���ڵ���Ѱƥ��ĸ��ڵ�
	 * @return void
	 * @throws
	 */
	public void searchParentNode(List<TaskNode> unCompleteTaskList,List<RandomDag> dagList) {

		// ��δ������б��е�����ִ��һ����������
		for (int i = 0; i < unCompleteTaskList.size(); i++) {
			TaskNode taskNode = unCompleteTaskList.get(i);

			// Collections.shuffle���������������ԭ����˳�򣬺�ϴ��һ��
			Collections.shuffle(dagList);// ��nodeȥ���ƥ��һ��dag

			boolean match = false;

			//���DAG���˵����ڶ��㣬���п��ܵ����ڶ������кܶ���������Ի���Ҫ��һ��֮��ġ�
			//�ж�ĳ��δƥ��Ľڵ��ܷ��Ϊĳ��DAGͼ�����սڵ�
			for (int n = 0; n < dagList.size(); n++) // ���Dagƥ��
			{
				RandomDag dag = dagList.get(n);
				//���ƥ��ɹ����ὫDAG��ҵ�Ƴ�
				if (dag.levelCount == dag.dagLevel + 1){
					match = tryFinishDag(taskNode, dag);
//					System.out.println("ƥ��ɹ�����DAG�����һ���ڵ㣺"+dag.dagId+";��ǰ��������"
//					+dag.taskList.size()+";��������id�ǣ�"+dag.taskList.get((dag.taskList.size()-1)).nodeId
//					+";levelCount="+dag.levelCount+";dagLevel="+dag.dagLevel);
				}
				if (match)
					break;
			}		
			// ��������ƥ�䣬�ýڵ��Ѿ�ƥ��ɹ���������һ������ڵ��ƥ��
			if (match)
				continue;

			// ==============�����ǰ�ڵ��ǳ�ʼ������ʱ��������ֹ�ڵ��е�һԱ���ж����ܷ��Ϊĳ��DAGͼ����ֹ�ڵ�======
			if (isEndNode(taskNode.nodeId))// �����ڵ�ƥ��
			{
				for (int k = 0; k < dagList.size(); k++) {
					RandomDag dag = dagList.get(k);
					match = tryFinishDag(taskNode, dag);			
					//System.out.println("ƥ��ɹ��������������һ�������Ϊĳ��DAG�����:"+dag.dagId);
					
					if (match)
						break;
				}
			}
			
			if (match)
				continue;// ��������ƥ�䣬������һ������ڵ��ƥ��

			// ================================================================================

			for (int k = 0; k < dagList.size(); k++) {

				RandomDag dag = dagList.get(k);
				// �����DAGͼ�����Ѿ��������ޣ��������Ը�DAGͼ������
				if (dag.levelCount == dag.dagLevel + 1)
					continue;
				/**
				 * 
				 */
				if(dag.taskList.size()>dag.dagSize)
					continue;
				/**
				 * 
				 */
				boolean matchFlag = false;
				int edgeNum = 0;// �͵�ǰ�ڵ������ӵıߵ�����

				// �ڵ�ǰDAGͼ����Ѱ��ǰ�ڵ��ƥ��ĸ��ڵ㲢��������
				for (int j = 0; j < dag.lastLevelList.size(); j++) {

					TaskNode leafNode = dag.lastLevelList.get(j);
					if (taskNode.startTime >= leafNode.endTime)// �Ƿ�͵�ǰDagƥ��
					{
						if (taskNode.startTime == leafNode.endTime)// ���ʱ���Ϊ0,�ж��Ƿ���ͬһ����������
						{
							// �����ͬһ�������ϣ���������ϲ�ڵ�Ϊ��ǰ�ڵ㸸�ڵ�Ŀ�����
							if (taskNode.getProcessorId() != leafNode
									.getProcessorId()
									&& leafNode.getProcessorId() != 0)
								continue;
						}

						
						// ����ǰ�ڵ��Ѿ��ͱ�DAGͼ�������ϲ�ڵ㽨�����ӹ�ϵ��
						// ��ʹ��ǰ�ϲ�ڵ�͵�ǰ�ڵ�����Ҫ�󣬸��ϲ�ڵ��ܹ���Ϊ��ǰ�ڵ�ĸ��ڵ㣬����Ҳֻ��1/2�ĸ�������֮���ܹ���������
						if (edgeNum > 1)// ����Ѿ�����һ��ƥ�����
						{
							if (Math.random() > 0.5)
								continue;
						}
						dag.generateEdge(leafNode, taskNode);
						
						
						//----------------------����ܹ���������----------ִ�����������----
						
						edgeNum++;
						matchFlag = true;
						match = true;

						// ƥ���Ͳ���Ҷ�ӽڵ���
						dag.leafNodeList.remove(leafNode);

						// �����DAGͼ�б����ǲ�������ǰ����ڵ��
						if (!dag.containTaskNode(taskNode)) {
							// �����µ�һ��
							dag.addToNewLevel(taskNode);
							dag.generateNode(taskNode);
							//System.out.println("ƥ���ʱ����ӵ�����ڵ�,DAG��ҵ�ǣ�"+dag.dagId+"����������"+dag.taskList.size());
							// ��ǰ�ڵ���Ҷ�ӽڵ�
							dag.leafNodeList.add(taskNode);
						}
		
					}

				}

				// �� ��ǰDAGͼ�����ϲ�ڵ㶼������Ϻ������ǰ�ڵ�͵�ǰDagƥ�䣬��������������ʣ�µ�DAGͼ�ı���
				if (matchFlag)
					break;
			}
			
			
			// ============�����ǰ�ڵ���Ŀǰ���е�DAGͼ����ƥ�䣬��Ϊ��Dag��root==========
			if (!match) {
				// ��һDAGͼ���ύʱ��
				int foreDagTime;
				//�жϣ�Ҫô��dagList�����һ��DAGͼ���ύʱ�䣬Ҫô��finishDagList�����һ��DAG���ύʱ��
				if (dagList.size() > 0)
					foreDagTime = dagList.get(dagList.size() - 1).submitTime;
				else
					foreDagTime = finishDagList.get(finishDagList.size() - 1).submitTime;
				
				RandomDag dag = new RandomDag(dagList.size()+ finishDagList.size() + 1, taskNode, foreDagTime);

				// ������DAGͼ�ǵ�һ�ڵ��
				if (dag.isSingle) {
					finishDagList.add(dag);// �ŵ�����б���ȥ
				} else {
					dagList.add(dag);
				}

			}
		}

	}

	/**
	 * 
	 * @Title: generateDags
	 * @Description: ����DAGͼ,ò�ƾ������ɵ�һ��DAG�����������룿��������
	 * @return void
	 * @throws
	 */
	public void generateDags(int number) {

		for (int i = 1; i <= number; i++) {
			// ��������ĵ�һ���ڵ�
			RandomDag dag = new RandomDag(i);
			dagList.add(dag);
		}
		// Ϊ��һ��DAG��Ѱ���ʵĽڵ�
		searchParentNode(unCompleteTaskList, dagList);
	}

	/**
	 * 
	 * @Title: fillDags
	 * @Description: Ϊ�������DAL�б��е�δ����dag�����һ��ָ���Ľ�������
	 * 				   ���һ�������ڵ� foot�ڵ� ���ڹ���������DAGͼ���������Ϊһ�����
	 * @return void
	 * @throws
	 */
	public void fillDags() {
		
		//System.out.println("��Ҫ��һ����dagList.size=" + dagList.size());
		
		for (int k = 0; k < dagList.size(); k++) {
			RandomDag dag = dagList.get(k);
			TaskNode footNode = new TaskNode("foot_" + (k + 1), 0, endTime,endTime);
			dag.generateNode(footNode);
			//System.out.println("��һ����ʱ����ӵĽڵ㣬DAG��ҵ��:"+dag.dagId+"����������"+dag.taskList.size());
			endNodeNumber++;// β�ڵ�����һ
			for (TaskNode leafTask : dag.leafNodeList)
				dag.generateEdge(leafTask, footNode);
			finishDagList.add(dag);
		}
	}

	/**
	 * 
	 * @Title: finishDags
	 * @Description: Ϊÿһ�������ɵ�DAGͼ�����ֹʱ��
	 * @return void
	 * @throws
	 */
	public void finishDags() {
		for (RandomDag dag : finishDagList) {
			dag.computeDeadLine();
		}
	}

	/**
	 * 
	 * @Title: checkDags
	 * @Description: TODO
	 * @return void
	 * @throws
	 */
	public void checkDags()// ������ɵ�dag�Ƿ���ȷ
	{
		// ���ڵ�����
		int nodeSum = 0;
		for (RandomDag dag : finishDagList) {
			nodeSum += dag.taskList.size();
			// �ж�unCompleteTaskList���Ƿ������ǰ�ڵ�
			for (TaskNode node : dag.taskList)
				if (!unCompleteTaskList.contains(node)) {
					System.err.print("�������ڵ㣺" + node.nodeId + " ");
				}
			// �жϱ��Ƿ���ڣ�edgeList���Ƿ������ǰ��
			for (DagEdge edge : dag.edgeList)
				if (edge.tail.startTime < edge.head.endTime)
					System.err.print("�ߴ���" + edge.head + "����>" + edge.tail
							+ "��");
		}

		System.err.println();

		int number = 0;
		for (TaskNode taskNode : unCompleteTaskList) {

			boolean containflag = false;
			for (RandomDag dag : finishDagList)
				if (dag.taskList.contains(taskNode)) {
					containflag = true;
					break;
				}
			if (!containflag) {
				System.err.print(taskNode.nodeId + ":" + taskNode.startTime+ " ");
				number++;
			}

		}

		if (nodeSum == unCompleteTaskList.size() + 1 + endNodeNumber)
			System.out.println("Success");
		else
			System.out.println("check dags and fix bugs");

	}

	/**
	 * 
	 * @Title: writeDags
	 * @Description: �����txt�ļ���Ϊxml�ļ�
	 * @return void
	 * @throws
	 */
	public void writeDags() {

		//FileDag fileDag = new FileDag(caseCount,pathTXT);
		XMLOutputUtil xmldag = new XMLOutputUtil(pathXML);

		//fileDag.clearDir();
		//xmldag.clearDir();

		try {
			//String basePath = System.getProperty("user.dir") + "\\DAG_XML\\";
			String filePathxml = pathXML + "DeadlineOld.txt";

			PrintStream out = System.out;
			PrintStream ps = new PrintStream(new FileOutputStream(filePathxml));
			System.setOut(ps); // �ض��������
			
			for (int i = 1; i <= finishDagList.size(); i++) {
				for (RandomDag dag : finishDagList) {
					String[] number = dag.dagId.split("dag");
					if (i == Integer.valueOf(number[1]).intValue()) {
						System.out.println(dag.dagId + " "+ dag.taskList.size() + " " + dag.submitTime+ " " + dag.deadlineTime);
						break;
					}
				}
			}
			
			ps.close();
			System.setOut(out);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (RandomDag dag : finishDagList) {
		//	fileDag.writeData(dag);
			xmldag.writeDataToXML(dag);
		}	

		
	}

	/**
	 * 
	 * @Title: initDags
	 * @Description: ��ʼ��DAGͼ������Ϣ
	 * @return void
	 * @throws
	 */
	public void initDags() {
		unCompleteTaskList = new ArrayList<TaskNode>();
		dagList = new ArrayList<RandomDag>();
		finishDagList = new ArrayList<RandomDag>();
		// ���ÿ�������������ɵ����һ������
		endNodeList = new ArrayList<String>();
		randomCreater = new RandomParametersUtil();

		// ��ʼ�������������������� ��ʼ���������
		// ���õĴ���ʱ�����ܵ�ʱ��
		List<Processor> processorList = createProcessor(
				CommonParametersUtil.processorNumber, CommonParametersUtil.timeWindow
						/ CommonParametersUtil.processorNumber);

		// ��ӡ��ʼ������List��ÿ���������ϵĵ���������
		printNodes(processorList);

		// ��ʼ�� public List<String> endNodeList;//�����ڵ��б�
		// ��ʼ�� public List<TaskNode> unCompleteTaskList;//δ��������б�
		initList(processorList);

		/**
		 * �������Զ�ѭ�������µ�DAG
		 * ���һ���ڵ㲻����ֹһ��DAG���߲�����Ϊĳ��DAG�����񣬾ͻ���Ϊһ���µ�DAG���������
		 */
		generateDags(1);// �տ�ʼ����һ����root��DAG

		// Ϊû��˳������DGA��ҵ�����һ��ͳһ�Ľ����ڵ� foot�ڵ�.���ڹ���������DAGͼ���������Ϊһ�����
		// �����ڽ�ɭ���������
		fillDags();
		// ����deadline�ʹ�ӡ��Ϣ
		finishDags();

		checkDags();// �������Dag����ȷ��

		// ���DAG��Ϣ��TXT�Լ�XML
		writeDags();
		
		/**
		 * ���������ɵ�״̬��������µ�DAG��Ȼ������ؼ�·�����õ��µ�deadline
		 */
		try {
			initdagmap(pathXML);
			repaireDeadline(pathXML);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}


	/**
	 * 
	* @Title: repaireDeadline
	* @Description: �ı���deadline�����ݹؼ�·��������
	* @throws
	 */
	public static void repaireDeadline(String pathXML) {

		//String basePath = System.getProperty("user.dir") + "\\DAG_XML\\";
		try {
			
			String filePathxml = pathXML + "Deadline.txt";

			PrintStream out = System.out;
			PrintStream ps = new PrintStream(new FileOutputStream(filePathxml));
			System.setOut(ps); // �ض��������
			for (int i = 0; i <= DAGMapList.size(); i++) {
				for (DAG dag : DAGMapList) {
					if (i == dag.getDAGId()) {
						System.out.println("dag"+(dag.getDAGId()+1) + " "+ dag.gettasklist().size() + " " + dag.getsubmittime()+ " " + dag.getDAGdeadline());
						break;
					}
				}
			}
			ps.close();
			System.setOut(out);		
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}

	
	/**
	 * �ⲿ�ӿڣ���������DAGͼ
	 * 
	 * @Title: BuildDAG
	 * @Description: TODO
	 * @return void
	 * @throws
	 */
	public void BuildDAG(int caseCount, String pathXML) {
		DagBuilder dagBuilder = new DagBuilder(pathXML);
		dagBuilder.initDags();

	}
	
	
	
	public static void initdagmap(String pathXML) throws Throwable {
		int pre_exist = 0;
		
		File file = new File(pathXML);
		String[] fileNames = file.list();
		int num = fileNames.length - 1;

		BufferedReader bd = new BufferedReader(new FileReader(pathXML+ "DeadlineOld.txt"));
		String buffered;

		for (int i = 0; i < num; i++) {
			// ÿ��DAG��һ��dagmap
			DAG dagmap = new DAG();
			DAGdepend dagdepend_persional = new DAGdepend();
			
			DAG_queue_personal.clear();

			buffered = bd.readLine();
			String bufferedA[] = buffered.split(" ");
			int buff[] = new int[4];
			buff[0] = Integer.valueOf(bufferedA[0].split("dag")[1]).intValue();// dagID
			buff[1] = Integer.valueOf(bufferedA[1]).intValue();// tasknum
			buff[2] = Integer.valueOf(bufferedA[2]).intValue();// arrivetime
			buff[3] = Integer.valueOf(bufferedA[3]).intValue();// deadline
			int deadline = buff[3];
			// ����Ҳ�ǰ�����Щ��ǰ����ȥ��0.0.0�Ľڵ�
			int tasknum = buff[1];
			// ��Ǳ���ҵ�Ƿ��ǵ�������ҵ
			if (tasknum == 1)
				dagmap.setSingle(true);

			int arrivetime = buff[2];

			// ��ÿ��DAG��������������������Լ�������Ϣ
			pre_exist = initDAG_createDAGdepend_XML(i, pre_exist, tasknum,arrivetime, pathXML);


			dagdepend_persional.setDAGList(DAG_queue_personal);
			dagdepend_persional.setDAGDependMap(DAGDependMap_personal);
			dagdepend_persional.setDAGDependValueMap(DAGDependValueMap_personal);
			

			dagmap.settasknumber(tasknum);
			dagmap.setDAGId(i);
			dagmap.setDAGdeadline(deadline);
			dagmap.setsubmittime(arrivetime);
			dagmap.settasklist(DAG_queue_personal);
			dagmap.setdepandmap(DAGDependMap_personal);
			dagmap.setdependvalue(DAGDependValueMap_personal);
			
			createDeadlineWithCri(deadline, dagdepend_persional, dagmap);
			DAGMapList.add(dagmap);
			
		}
	}

	
	private static int initDAG_createDAGdepend_XML(int i, int preexist,int tasknumber, int arrivetimes, String pathXML)throws NumberFormatException, IOException, JDOMException {

		int back = 0;
		DAGDependMap_personal = new LinkedHashMap<Integer, Integer>();
		DAGDependValueMap_personal = new LinkedHashMap<String, Double>();

		// ��ȡXML������
		SAXBuilder builder = new SAXBuilder();
		// ��ȡdocument����
		Document doc = builder.build(pathXML + "/dag" + (i + 1) + ".xml");
		// ��ȡ���ڵ�
		Element adag = doc.getRootElement();

		for (int j = 0; j < tasknumber; j++) {
			Task dag_persional = new Task();

			dag_persional.setid(Integer.valueOf(j).intValue());
			dag_persional.setarrive(arrivetimes);
			dag_persional.setdagid(i);

			XPath path = XPath.newInstance("//job[@id='" + j + "']/@tasklength");
			List list = path.selectNodes(doc);
			Attribute attribute = (Attribute) list.get(0);
			// x������ĳ���
			int x = Integer.valueOf(attribute.getValue()).intValue();

			dag_persional.setlength(x);
			dag_persional.setts(x);
			DAG_queue_personal.add(dag_persional); // ��ǰDAG��һ���������������б�

		}

		XPath path1 = XPath.newInstance("//uses[@link='output']/@file");
		List list1 = path1.selectNodes(doc);
		for (int k = 0; k < list1.size(); k++) {
			Attribute attribute1 = (Attribute) list1.get(k);
			String[] pre_suc = attribute1.getValue().split("_");

			int[] presuc = new int[2];
			presuc[0] = Integer.valueOf(pre_suc[0]).intValue() + preexist;
			presuc[1] = Integer.valueOf(pre_suc[1]).intValue() + preexist;

			XPath path2 = XPath.newInstance("//uses[@file='"+ attribute1.getValue() + "']/@size");
			List list2 = path2.selectNodes(doc);
			Attribute attribute2 = (Attribute) list2.get(0);
			int datasize = Integer.valueOf(attribute2.getValue()).intValue();

			DAGDependMap_personal.put(Integer.valueOf(pre_suc[0]).intValue(),Integer.valueOf(pre_suc[1]).intValue());
			DAGDependValueMap_personal.put((pre_suc[0] + " " + pre_suc[1]),(double) datasize);

			int tem0 = Integer.parseInt(pre_suc[0]);
			int tem1 = Integer.parseInt(pre_suc[1]);
			DAG_queue_personal.get(tem0).addToSuc(tem1);//���������
			DAG_queue_personal.get(tem1).addToPre(tem0);//��Ӹ�����

		}

		// for(Entry<Integer, Integer> map:DAGDependMap_personal.entrySet()){
		// System.out.println("======>dag:"+i+"\t"+map.getKey()+":"+map.getValue());
		// }
		// System.out.println("DAGDependValueMap_personal="+DAGDependValueMap_personal.size());
		//
		back = preexist + tasknumber;
		return back;
	}
	
	
	private static void createDeadlineWithCri(int dead_line,DAGdepend dagdepend_persion, DAG dagmap) throws Throwable {

		//����ؼ�·���������ÿ��ֵõľ�ֵ
		int newDeadline=createDeadlineCriticalPath(dead_line,dagdepend_persion,dagmap);	
		if(newDeadline<dagmap.getDAGdeadline()){
			newDeadline=dagmap.getDAGdeadline();
		}
		dagmap.setDAGdeadline(newDeadline);
	
	}
	
	//============================��ؼ�·��
	/**
	 * @return 
	 * @param dagmap 
	 * 
	* @Title: createDeadlineCriticalPath
	* @Description: ͨ���ؼ�·���������µ�deadline
	* @param dead_line
	* @param dagdepend_persion
	* @throws Throwable:
	* @throws
	 */
	private static int createDeadlineCriticalPath(int dead_line,DAGdepend dagdepend, DAG dagmap) throws Throwable {
		int taskSize = dagmap.gettasknumber();
		ArrayList<Task> taskList=dagmap.gettasklist();
		Stack<Integer> topo = new Stack<Integer>(); // ��������Ķ���ջ
		int[] ve = null; //	����������緢��ʱ��
		int[] vl = null; // ���������ٷ���ʱ��

		//���ͼ����������ѹ��ջ�У����õ�ÿ����������翪ʼʱ��
		ve=topologicalSort(dagmap,ve,topo,dagdepend);
		
		ArrayList<Integer> topoList=new ArrayList<>();
		while (!topo.isEmpty()) {
			topoList.add(topo.pop());
		}
		//���б�������id��С��������
		Collections.reverse(topoList);	
		//�õ��ؼ�·��
		criticalPath(topoList,ve,dagmap,dagdepend);
		
		//���ÿ�������ƽ����������
		int endIndex=topoList.get(topoList.size()-1);
		Task tempDag=taskList.get(endIndex);
		int oldDeadline=ve[topoList.get(topoList.size()-1)]+tempDag.getlength()+dagmap.getsubmittime();
		return computeDeadLine(dagmap,oldDeadline);
		
	}
	
	/**
	 * 
	* @Title: computeDeadLine
	* @Description: �����µ�deadline
	* @param dagmap
	* @param oldDeadline
	* @return:
	* @throws
	 */
	public static int computeDeadLine(DAG dagmap, int oldDeadline) {
		int proceesorEndTime = CommonParametersUtil.timeWindow/ CommonParametersUtil.processorNumber;
		int submitTime=dagmap.getsubmittime();
		int newDeadline = (int) (submitTime + (oldDeadline - submitTime)* CommonParametersUtil.deadLineTimes);
		if (newDeadline > proceesorEndTime)
			newDeadline = proceesorEndTime;
		//System.out.println(dagmap.getDAGId()+"\toldDeadline"+oldDeadline+"\tnewDeadline"+newDeadline);
		return newDeadline;
	}


	/**
	 * 
	* @Title: topologicalSort
	* @Description: �����������������ջ�У����ص����������������翪ʼִ��ʱ��
	* @param dagmap
	* @param ve
	* @param topo
	* @param dagdepend
	* @return:
	* @throws
	 */
	private static int[] topologicalSort(DAG dagmap, int[] ve, Stack topo, DAGdepend dagdepend) {
			int count = 0; //����������
	        int[] inDegree = findInDegree(dagmap); //�������������
	        int taskSize=dagmap.gettasknumber();
	        Stack<Integer> noInputTask = new Stack<Integer>();  //����ȵ� ����ջ
	        
	        ArrayList<Task> taskList=new ArrayList<>();
			taskList=dagmap.gettasklist();
			
	        //�ҵ���һ�����Ϊ0�Ľڵ�
	        for(int i = 0; i < taskSize; i++){
	        	if(inDegree[i] == 0){
	        		noInputTask.push(i);  //���Ϊ0�Ľ�ջ
	        	}	
	        }
	        ve = new int[taskSize]; //��ʼ��,����洢���Ǹ����ڵ�����翪ʼִ��ʱ��

	        while( !noInputTask.isEmpty()){
	        	//��ǰ������
	            int currentTask = (Integer) noInputTask.pop();
	            Task curDag=taskList.get(currentTask);
	            topo.push(currentTask); //i�Ŷ�����Tջ������ 
	            
	            for(int m=0;m<taskSize;m++){
	            	Task tempDag=taskList.get(m);
	            	ArrayList<Integer> parents=tempDag.getpre();
	            	//��ǰɾ�����������������ĸ�����
	            	if(parents.contains(currentTask)){
	            		inDegree[m]--;
	            		if(inDegree[m]==0){
	            			noInputTask.push(m);//��ǰ������û�����
	            		}
	            		Map<String,Double> valueMap=dagdepend.getDAGDependValueMap();
	            		String key=currentTask+" "+m;
	            		Double value=valueMap.get(key)+curDag.getlength();//�ü���ִ��ʱ��
	            		 if(ve[currentTask] + value> ve[m])
	 	                    ve[m] = (int) (ve[currentTask] + value);
	            	}
	            }
	        }    
	        return ve;
	}

	/**
	 * 
	* @Title: criticalPath
	* @Description: ������ؼ�·�������عؼ�·�����������
	* @param topoList
	* @param ve
	* @param dagmap
	* @param dagdepend
	* @return
	* @throws Exception:
	* @throws
	 */
	public static int criticalPath(ArrayList<Integer> topoList, int[] ve, DAG dagmap, DAGdepend dagdepend) throws Exception{  
		int taskSize=dagmap.gettasknumber();
		ArrayList<Task> taskList=new ArrayList<>();
		taskList=dagmap.gettasklist();
		
         int[] vl = new int[taskSize];
         // ��ʼ���������¼�����ٷ���ʱ��Ϊ���һ����������ʱ��
         for(int i = 0; i < taskSize; i++){
             vl[i] = ve[taskSize - 1]; 
         }
		Map<String, Double> valueMap = dagdepend.getDAGDependValueMap();
         for(int k=topoList.size()-1;k>=0;k--){
			int currentTask = (int) topoList.get(k);// �õ���ǰ����ı��

			Task tempDag = taskList.get(currentTask);
			ArrayList<Integer> childs = tempDag.getsuc();
			if (childs == null)
				continue;
			
			for (int p = 0; p < childs.size(); p++) {
				int childNo=childs.get(p);
				String key = currentTask + " " + childNo;
				Double value = valueMap.get(key);			
				int diff = (int) (vl[childNo] - value - tempDag.getlength());
				if ((diff) < vl[currentTask]) {
					vl[currentTask] = diff;
				}
			}
		} 
         
         int criticalNum=0;
         ArrayList<Integer> criticalNodeList=new ArrayList<>();
         for(int i = 0; i < taskSize; i++){  
        	 int ee= ve[i];
        	 int el=vl[i];
             if(ee==el){
            	 criticalNodeList.add(i);
            	 //System.out.println("�ؼ�·����"+i);
            	 criticalNum++;
             }
         } 
        // System.out.println("++++++++++++++++++++"+dagmap.getDAGId());
         return criticalNum;
    } 

	

	/**
	 * 
	* @Title: findInDegree
	* @Description: ��ø���������Ӧ�����
	* @param dagmap
	* @return:
	* @throws
	 */
	private static int[] findInDegree(DAG dagmap) {
		int taskSize=dagmap.gettasknumber();
		int[] indegree  = new int[taskSize];
		ArrayList<Task> taskList=new ArrayList<>();
		taskList=dagmap.gettasklist();
        for(int i = 0; i < taskSize; i++){
        	Task tempDag=taskList.get(i);
        	ArrayList<Integer> parents=tempDag.getpre();
        	indegree[i]=parents.size();	
        }
        return indegree; 
	}
	//========================================================================

	private static Task getDAGById_task(int dagId) {
		for (Task dag : DAG_queue_personal) {
			if (dag.getid() == dagId)
				return dag;
		}
		return null;
	}
}
