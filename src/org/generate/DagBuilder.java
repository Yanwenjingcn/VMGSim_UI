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

	private int endNodeNumber = 0;// DAG图的尾节点数

	public static int endTime = 100 * 10000;

	public static RandomParametersUtil randomCreater;

	public List<TaskNode> unCompleteTaskList;// 未完成任务列表

	public static List<RandomDag> dagList;// dag列表

	public static List<RandomDag> finishDagList;// 构造完成的Dag列表

	public List<String> endNodeList;// 结束节点列表
	
	

	private String pathXML;

	
	//=================================
	private static ArrayList<Task> DAG_queue_personal;
	private static LinkedHashMap<Integer, Integer> DAGDependMap_personal;
	private static LinkedHashMap<String, Double> DAGDependValueMap_personal;
	private static ArrayList<DAG> DAGMapList;
	/**
	 * 
	 * @Title: createProcessor
	 * @Description: 初始化各个处理器，默认的处理器工作时长为 总时间窗口时间/处理器个数
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
	 * 打印节点信息
	 * 
	 * @param processorList
	 *            处理器列表
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
	 * @Description: 初始化 public List<String> endNodeList;//结束节点列表 初始化 public
	 *               List<TaskNode> unCompleteTaskList;//未完成任务列表
	 * @return void
	 * @throws
	 */
	public void initList(List<Processor> processorList) {
		// 初始化未完成的列表
		int maxsize = 0;
		// 获取处理器中初始的最大的任务数目maxsize
		for (Processor processor : processorList) {
			int size = processor.nodeList.size();
			if (size > maxsize)
				maxsize = size;
		}

		// 将所有处理器上的任务都加载在 未处理列表中
		// 初始化 public List<TaskNode> unCompleteTaskList;//未完成任务列表
		for (int i = 0; i < maxsize; i++)
			for (Processor processor : processorList) {
				if (i < processor.nodeList.size())
					unCompleteTaskList.add(processor.nodeList.get(i));
			}

		/**
		 * 打印初始时生成了多少个任务。
		 */
		// System.out.println("unCompleteTaskList的大小为="+unCompleteTaskList.size());

		// 获取所有处理器上的最后一个结点
		// 初始化 public List<String> endNodeList;//结束节点列表
		for (Processor processor : processorList) {
			String endNodeId = processor.nodeList
					.get(processor.nodeList.size() - 1).nodeId;
			endNodeList.add(endNodeId);
		}
	}

	/**
	 * 
	 * @Title: isEndNode
	 * @Description: 判断某个节点是否是初始处理器生成时产生的最终节点
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
	 * @Description: 尝试判断某个节点能否成为某DAG图的最终节点，可以返回TRUE
	 * @return boolean
	 * @throws
	 */
	public boolean tryFinishDag(TaskNode taskNode, RandomDag dag) {
		int m = 0;
		for (TaskNode leafTask : dag.leafNodeList)
			if (taskNode.startTime >= leafTask.endTime) {
				if (taskNode.startTime == leafTask.endTime)// 如果时间差为0,判断是否在同一个处理器上
				{
					if (taskNode.getProcessorId() != leafTask.getProcessorId())
						continue;
				}
				m++;
			}

		// 是否该节点能做为这个DAG图的最终节点？
		if (m == dag.leafNodeList.size())// 可以构成完整的Dag图
		{
			dag.generateNode(taskNode);
			//System.out.println("当前节点"+taskNode.nodeId+";能够结束作业DAG："+dag.dagId+"；任务数："+dag.taskList.size());
			for (TaskNode leafTask : dag.leafNodeList)
				dag.generateEdge(leafTask, taskNode);
			dagList.remove(dag);
			finishDagList.add(dag);// 放到完成列表中去
			return true;
		}

		return false;
	}

	/**
	 * 
	 * @Title: searchParentNode
	 * @Description: 为 unCompleteTaskList 中的每一个节点找寻匹配的父节点
	 * @return void
	 * @throws
	 */
	public void searchParentNode(List<TaskNode> unCompleteTaskList,List<RandomDag> dagList) {

		// 对未分配的列表中的任务都执行一次下述操作
		for (int i = 0; i < unCompleteTaskList.size(); i++) {
			TaskNode taskNode = unCompleteTaskList.get(i);

			// Collections.shuffle（）就是随机打乱原来的顺序，和洗牌一样
			Collections.shuffle(dagList);// 让node去随机匹配一个dag

			boolean match = false;

			//如果DAG到了倒数第二层，很有可能倒数第二层中有很多的任务，所以还需要归一化之类的。
			//判断某个未匹配的节点能否成为某个DAG图的最终节点
			for (int n = 0; n < dagList.size(); n++) // 完成Dag匹配
			{
				RandomDag dag = dagList.get(n);
				//如果匹配成功，会将DAG作业移出
				if (dag.levelCount == dag.dagLevel + 1){
					match = tryFinishDag(taskNode, dag);
//					System.out.println("匹配成功，本DAG的最后一个节点："+dag.dagId+";当前任务数："
//					+dag.taskList.size()+";这个任务的id是："+dag.taskList.get((dag.taskList.size()-1)).nodeId
//					+";levelCount="+dag.levelCount+";dagLevel="+dag.dagLevel);
				}
				if (match)
					break;
			}		
			// 跳过本次匹配，该节点已经匹配成功。进行下一个任务节点的匹配
			if (match)
				continue;

			// ==============如果当前节点是初始处理器时产生的终止节点中的一员，判断它能否成为某个DAG图的终止节点======
			if (isEndNode(taskNode.nodeId))// 结束节点匹配
			{
				for (int k = 0; k < dagList.size(); k++) {
					RandomDag dag = dagList.get(k);
					match = tryFinishDag(taskNode, dag);			
					//System.out.println("匹配成功，处理器的最后一个结点作为某个DAG的完结:"+dag.dagId);
					
					if (match)
						break;
				}
			}
			
			if (match)
				continue;// 跳过本次匹配，进行下一个任务节点的匹配

			// ================================================================================

			for (int k = 0; k < dagList.size(); k++) {

				RandomDag dag = dagList.get(k);
				// 如果该DAG图层数已经到达上限，则跳过对该DAG图的搜索
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
				int edgeNum = 0;// 和当前节点所连接的边的条数

				// 在当前DAG图中找寻当前节点可匹配的父节点并建立连接
				for (int j = 0; j < dag.lastLevelList.size(); j++) {

					TaskNode leafNode = dag.lastLevelList.get(j);
					if (taskNode.startTime >= leafNode.endTime)// 是否和当前Dag匹配
					{
						if (taskNode.startTime == leafNode.endTime)// 如果时间差为0,判断是否在同一个处理器上
						{
							// 如果在同一处理器上，则放弃该上层节点为当前节点父节点的可能性
							if (taskNode.getProcessorId() != leafNode
									.getProcessorId()
									&& leafNode.getProcessorId() != 0)
								continue;
						}

						
						// 当当前节点已经和本DAG图中其它上层节点建立父子关系后。
						// 即使当前上层节点和当前节点满足要求，该上层节点能够成为当前节点的父节点，但是也只有1/2的概率它们之间能够建立连接
						if (edgeNum > 1)// 如果已经和上一层匹配过了
						{
							if (Math.random() > 0.5)
								continue;
						}
						dag.generateEdge(leafNode, taskNode);
						
						
						//----------------------如果能够建立连接----------执行下面的内容----
						
						edgeNum++;
						matchFlag = true;
						match = true;

						// 匹配后就不是叶子节点了
						dag.leafNodeList.remove(leafNode);

						// 如果该DAG图中本身是不包含当前任务节点的
						if (!dag.containTaskNode(taskNode)) {
							// 创建新的一层
							dag.addToNewLevel(taskNode);
							dag.generateNode(taskNode);
							//System.out.println("匹配的时候添加的任务节点,DAG作业是："+dag.dagId+"；任务数："+dag.taskList.size());
							// 当前节点变成叶子节点
							dag.leafNodeList.add(taskNode);
						}
		
					}

				}

				// 当 当前DAG图所有上层节点都遍历完毕后，如果当前节点和当前Dag匹配，则跳出，不进行剩下的DAG图的遍历
				if (matchFlag)
					break;
			}
			
			
			// ============如果当前节点与目前所有的DAG图都不匹配，则为新Dag的root==========
			if (!match) {
				// 上一DAG图的提交时间
				int foreDagTime;
				//判断，要么是dagList中最后一个DAG图的提交时间，要么是finishDagList中最后一个DAG的提交时间
				if (dagList.size() > 0)
					foreDagTime = dagList.get(dagList.size() - 1).submitTime;
				else
					foreDagTime = finishDagList.get(finishDagList.size() - 1).submitTime;
				
				RandomDag dag = new RandomDag(dagList.size()+ finishDagList.size() + 1, taskNode, foreDagTime);

				// 如果这个DAG图是单一节点的
				if (dag.isSingle) {
					finishDagList.add(dag);// 放到完成列表中去
				} else {
					dagList.add(dag);
				}

			}
		}

	}

	/**
	 * 
	 * @Title: generateDags
	 * @Description: 生成DAG图,貌似就是生成第一个DAG会调用这个代码？？？？？
	 * @return void
	 * @throws
	 */
	public void generateDags(int number) {

		for (int i = 1; i <= number; i++) {
			// 创建总体的第一个节点
			RandomDag dag = new RandomDag(i);
			dagList.add(dag);
		}
		// 为第一个DAG找寻合适的节点
		searchParentNode(unCompleteTaskList, dagList);
	}

	/**
	 * 
	 * @Title: fillDags
	 * @Description: 为最后留在DAL列表中的未完结的dag们添加一个指定的结束任务。
	 * 				   添加一个结束节点 foot节点 用于规整化整个DAG图的输出汇总为一个结点
	 * @return void
	 * @throws
	 */
	public void fillDags() {
		
		//System.out.println("需要归一化的dagList.size=" + dagList.size());
		
		for (int k = 0; k < dagList.size(); k++) {
			RandomDag dag = dagList.get(k);
			TaskNode footNode = new TaskNode("foot_" + (k + 1), 0, endTime,endTime);
			dag.generateNode(footNode);
			//System.out.println("归一化的时候添加的节点，DAG作业是:"+dag.dagId+"；任务数："+dag.taskList.size());
			endNodeNumber++;// 尾节点数加一
			for (TaskNode leafTask : dag.leafNodeList)
				dag.generateEdge(leafTask, footNode);
			finishDagList.add(dag);
		}
	}

	/**
	 * 
	 * @Title: finishDags
	 * @Description: 为每一个新生成的DAG图计算截止时间
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
	public void checkDags()// 检查生成的dag是否正确
	{
		// 检查节点数量
		int nodeSum = 0;
		for (RandomDag dag : finishDagList) {
			nodeSum += dag.taskList.size();
			// 判断unCompleteTaskList中是否包含当前节点
			for (TaskNode node : dag.taskList)
				if (!unCompleteTaskList.contains(node)) {
					System.err.print("不包含节点：" + node.nodeId + " ");
				}
			// 判断边是否存在，edgeList中是否包含当前边
			for (DagEdge edge : dag.edgeList)
				if (edge.tail.startTime < edge.head.endTime)
					System.err.print("边错误：" + edge.head + "——>" + edge.tail
							+ "　");
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
	 * @Description: 输出成txt文件改为xml文件
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
			System.setOut(ps); // 重定向输出流
			
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
	 * @Description: 初始化DAG图基本信息
	 * @return void
	 * @throws
	 */
	public void initDags() {
		unCompleteTaskList = new ArrayList<TaskNode>();
		dagList = new ArrayList<RandomDag>();
		finishDagList = new ArrayList<RandomDag>();
		// 存放每个处理器中生成的最后一个任务
		endNodeList = new ArrayList<String>();
		randomCreater = new RandomParametersUtil();

		// 初始化处理器，并生成其上 初始的随机任务
		// 设置的窗口时长是总的时长
		List<Processor> processorList = createProcessor(
				CommonParametersUtil.processorNumber, CommonParametersUtil.timeWindow
						/ CommonParametersUtil.processorNumber);

		// 打印初始处理器List中每个处理器上的的总任务数
		printNodes(processorList);

		// 初始化 public List<String> endNodeList;//结束节点列表
		// 初始化 public List<TaskNode> unCompleteTaskList;//未完成任务列表
		initList(processorList);

		/**
		 * 里面是自动循环生成新的DAG
		 * 如果一个节点不能终止一个DAG或者不能作为某个DAG的任务，就会作为一个新的DAG的入口任务
		 */
		generateDags(1);// 刚开始生成一个带root的DAG

		// 为没有顺利完结的DGA作业们添加一个统一的结束节点 foot节点.用于规整化整个DAG图的输出汇总为一个结点
		// 类似于将森林组成了树
		fillDags();
		// 计算deadline和打印信息
		finishDags();

		checkDags();// 检查生成Dag的正确性

		// 输出DAG信息至TXT以及XML
		writeDags();
		
		/**
		 * 根据已生成的状态，构造出新的DAG，然后求出关键路径，得到新的deadline
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
	* @Description: 改变其deadline，依据关键路径来计算
	* @throws
	 */
	public static void repaireDeadline(String pathXML) {

		//String basePath = System.getProperty("user.dir") + "\\DAG_XML\\";
		try {
			
			String filePathxml = pathXML + "Deadline.txt";

			PrintStream out = System.out;
			PrintStream ps = new PrintStream(new FileOutputStream(filePathxml));
			System.setOut(ps); // 重定向输出流
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
	 * 外部接口，调用生成DAG图
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
			// 每个DAG有一个dagmap
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
			// 其中也是包含这些以前加上去的0.0.0的节点
			int tasknum = buff[1];
			// 标记本作业是否是单任务作业
			if (tasknum == 1)
				dagmap.setSingle(true);

			int arrivetime = buff[2];

			// 对每个DAG创建其任务间的相关依赖以及基本信息
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

		// 获取XML解析器
		SAXBuilder builder = new SAXBuilder();
		// 获取document对象
		Document doc = builder.build(pathXML + "/dag" + (i + 1) + ".xml");
		// 获取根节点
		Element adag = doc.getRootElement();

		for (int j = 0; j < tasknumber; j++) {
			Task dag_persional = new Task();

			dag_persional.setid(Integer.valueOf(j).intValue());
			dag_persional.setarrive(arrivetimes);
			dag_persional.setdagid(i);

			XPath path = XPath.newInstance("//job[@id='" + j + "']/@tasklength");
			List list = path.selectNodes(doc);
			Attribute attribute = (Attribute) list.get(0);
			// x：任务的长度
			int x = Integer.valueOf(attribute.getValue()).intValue();

			dag_persional.setlength(x);
			dag_persional.setts(x);
			DAG_queue_personal.add(dag_persional); // 当前DAG（一个）的自有任务列表

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
			DAG_queue_personal.get(tem0).addToSuc(tem1);//添加子任务
			DAG_queue_personal.get(tem1).addToPre(tem0);//添加父任务

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

		//计算关键路径，求得其每层分得的均值
		int newDeadline=createDeadlineCriticalPath(dead_line,dagdepend_persion,dagmap);	
		if(newDeadline<dagmap.getDAGdeadline()){
			newDeadline=dagmap.getDAGdeadline();
		}
		dagmap.setDAGdeadline(newDeadline);
	
	}
	
	//============================求关键路径
	/**
	 * @return 
	 * @param dagmap 
	 * 
	* @Title: createDeadlineCriticalPath
	* @Description: 通过关键路径，构建新的deadline
	* @param dead_line
	* @param dagdepend_persion
	* @throws Throwable:
	* @throws
	 */
	private static int createDeadlineCriticalPath(int dead_line,DAGdepend dagdepend, DAG dagmap) throws Throwable {
		int taskSize = dagmap.gettasknumber();
		ArrayList<Task> taskList=dagmap.gettasklist();
		Stack<Integer> topo = new Stack<Integer>(); // 拓扑排序的顶点栈
		int[] ve = null; //	各顶点的最早发生时间
		int[] vl = null; // 各顶点的最迟发生时间

		//求得图的拓扑排序压入栈中，并得到每个任务的最早开始时间
		ve=topologicalSort(dagmap,ve,topo,dagdepend);
		
		ArrayList<Integer> topoList=new ArrayList<>();
		while (!topo.isEmpty()) {
			topoList.add(topo.pop());
		}
		//将列表反序，任务id从小到大排列
		Collections.reverse(topoList);	
		//得到关键路径
		criticalPath(topoList,ve,dagmap,dagdepend);
		
		//求得每个任务的平均分配冗余
		int endIndex=topoList.get(topoList.size()-1);
		Task tempDag=taskList.get(endIndex);
		int oldDeadline=ve[topoList.get(topoList.size()-1)]+tempDag.getlength()+dagmap.getsubmittime();
		return computeDeadLine(dagmap,oldDeadline);
		
	}
	
	/**
	 * 
	* @Title: computeDeadLine
	* @Description: 计算新的deadline
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
	* @Description: 获得其拓扑排序并亚茹栈中，返回的是其各个任务的最早开始执行时间
	* @param dagmap
	* @param ve
	* @param topo
	* @param dagdepend
	* @return:
	* @throws
	 */
	private static int[] topologicalSort(DAG dagmap, int[] ve, Stack topo, DAGdepend dagdepend) {
			int count = 0; //输出顶点计数
	        int[] inDegree = findInDegree(dagmap); //求各个顶点的入度
	        int taskSize=dagmap.gettasknumber();
	        Stack<Integer> noInputTask = new Stack<Integer>();  //零入度的 顶点栈
	        
	        ArrayList<Task> taskList=new ArrayList<>();
			taskList=dagmap.gettasklist();
			
	        //找到第一个入度为0的节点
	        for(int i = 0; i < taskSize; i++){
	        	if(inDegree[i] == 0){
	        		noInputTask.push(i);  //入度为0的进栈
	        	}	
	        }
	        ve = new int[taskSize]; //初始化,里面存储的是各个节点的最早开始执行时间

	        while( !noInputTask.isEmpty()){
	        	//当前的任务
	            int currentTask = (Integer) noInputTask.pop();
	            Task curDag=taskList.get(currentTask);
	            topo.push(currentTask); //i号顶点入T栈并计数 
	            
	            for(int m=0;m<taskSize;m++){
	            	Task tempDag=taskList.get(m);
	            	ArrayList<Integer> parents=tempDag.getpre();
	            	//当前删除的任务是这个任务的父任务
	            	if(parents.contains(currentTask)){
	            		inDegree[m]--;
	            		if(inDegree[m]==0){
	            			noInputTask.push(m);//当前的任务没有入度
	            		}
	            		Map<String,Double> valueMap=dagdepend.getDAGDependValueMap();
	            		String key=currentTask+" "+m;
	            		Double value=valueMap.get(key)+curDag.getlength();//得加上执行时间
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
	* @Description: 计算其关键路径，返回关键路径上任务个数
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
         // 初始化各顶点事件的最迟发生时间为最后一个任务的完成时间
         for(int i = 0; i < taskSize; i++){
             vl[i] = ve[taskSize - 1]; 
         }
		Map<String, Double> valueMap = dagdepend.getDAGDependValueMap();
         for(int k=topoList.size()-1;k>=0;k--){
			int currentTask = (int) topoList.get(k);// 得到当前任务的编号

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
            	 //System.out.println("关键路径："+i);
            	 criticalNum++;
             }
         } 
        // System.out.println("++++++++++++++++++++"+dagmap.getDAGId());
         return criticalNum;
    } 

	

	/**
	 * 
	* @Title: findInDegree
	* @Description: 获得各个任务相应的入度
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
