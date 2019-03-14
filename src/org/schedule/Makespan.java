package org.schedule;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import org.jdom.xpath.XPath;
import org.schedule.model.Task;
import org.schedule.model.DAGdepend;
import org.schedule.model.PE;
import org.schedule.model.PEComputerability;
import org.comparator.DAGComparator;
import org.comparator.DAGoriComparator;
import org.generate.util.CommonParametersUtil;
import org.jdom.Attribute;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class Makespan {

	private static int CURRENT_TIME = 0;	//当前时间
	private static int task_num = 0;
	private static int clocktick = 1;
	private static double heft_deadline = 0;
	private static int islastnum = 0;	//所有DAG图中最后一个任务的数目
	private static double deadLineTimes = 1.3;// deadline的倍数值 （1.1，1.3，1.6，2.0）
	private static int pe_number = 8;	//处理器个数
	public static String[][] rate = new String[5][4];

	private static ArrayList<PE> PEList;	//处理器列表
	private static ArrayList<Task> DAG_queue;	//这个里面是所有DAG中的子任务的集合，
	private static ArrayList<Task> ready_queue;	//

	private static HashMap<Integer, Integer> DAG_deadline;
	private static HashMap<Integer, Integer> DAGDependMap;	//这个里面是所有DAG中的子任务的集合，
	private static HashMap<String, Double> DAGDependValueMap;	//这个里面是所有DAG中的子任务的集合，各个DAG子任务间依赖所传输的边的值

	private static ArrayList<Task> DAG_queue_personal;	//单个DAG中的子任务列表
	private static HashMap<Integer, Integer> DAGDependMap_personal;	//单个DAG中的子任务映射关系
	private static HashMap<String, Double> DAGDependValueMap_personal;	//单个DAG中的子任务的边值
	
	private static Map<Integer, int[]> ComputeCostMap;	//单个 DAG的某个子任务与在每个处理器上的执行时间的映射
	private static Map<Integer, Integer> AveComputeCostMap;	//单个DAG的某个子任务与在处理器上的平均执行时间的映射

	public static Map<Integer, double[]> DAGExeTimeMap;	//在HEFT计算中，当前DAG(单个)中每个子任务的的最早开始时间与最迟开始时间
	private static Map<Integer, Task> DAGIdToDAGMap;
	private static Map<Integer, Double> upRankValueMap;
	private static Map<Integer, double[]> vmComputeCostMap;
	private static Map<Integer, Double> vmAveComputeCostMap;
	private static Map<Integer, Integer[]> cloudletInVm;	//处理器ID==》其上运行得子任务ID集合 的映射
	private static Map<Integer, Integer> cloudletInVmId;	//当前DAG（单个）中任务ID==》所分配的处理器ID的映射

	
	//遍历循环
	private int caseCount=0;
	private String pathXML;
	//public static int mkDataCount=0;
	
	private static String interval="";
	
	private static String multiRes=System.getProperty("user.dir") + "\\MultiResult\\";
	
	/**
	 * 
	 * @Title: Makespan
	 * @Description: 构造方法，初始化整个 工程
	 * @param:
	 * @throws
	 */
	public Makespan() {
		ready_queue = new ArrayList<Task>();
		DAG_queue = new ArrayList<Task>();
		DAG_queue_personal = new ArrayList<Task>();
		PEList = new ArrayList<PE>();
		DAGDependMap = new HashMap<Integer, Integer>();
		DAGDependValueMap = new HashMap<String, Double>();
		deadLineTimes = CommonParametersUtil.deadLineTimes;
		pe_number = CommonParametersUtil.processorNumber;
	}

	/**
	 * 
	 * @Title: runMakespan_xml
	 * @Description: 输出FIFO，EDF，STF，EFTF，NEWEDF调度结果
	 * @param @throws Throwable
	 * @return void
	 * @throws
	 */
	public void runMakespan_xml(String inputXMLPath,String resultPath) throws Throwable {

		Makespan ms = new Makespan();
		DAGdepend dagdepend = new DAGdepend();
		PEComputerability vcc = new PEComputerability();
		
		// 初始化处理器
		initPE();
		int num = initdagmap(dagdepend, vcc,inputXMLPath);

		fiforesult(dagdepend, num);
		edfresult(dagdepend, num);
		stfresult(dagdepend, num);
		eftfresult(dagdepend, num);
		

	}

	/**
	 * 输出FIFO调度结果
	 * 
	 * @param dagdepend ，工作流依赖关系
	 * @param num,全部DAG的各个子任务总个数
	 */
	public static void fiforesult(DAGdepend dagdepend, int num)throws Throwable {
		
		Date a = new Date();
		
		FIFO fifo = new FIFO(pe_number);
		FIFO.dag_queue_ori = DAG_queue;
		FIFO.course_number = DAG_queue.size();
		FIFO.pe_number = pe_number;
		FIFO.pe_list = PEList;
		FIFO.dagdepend = dagdepend;
		int[] temp1 = new int[pe_number + 3];	
		temp1 = FIFO.makespan(pe_number);


		DecimalFormat df = new DecimalFormat("0.0000");
		double sum = 0;

		System.out.println("FIFO:");
		
		for (int j = 0; j < PEList.size(); j++) {
			sum = (float) temp1[j + 1] / temp1[0] + sum;
		}
		System.out.println("PE's use ratio is "+ df.format((float) sum / pe_number));
		System.out.println("effective PE's use ratio is "+ df.format((float) temp1[pe_number + 2]/ (temp1[0] * pe_number)));
		System.out.println("Task Completion Rates is "+ df.format((float) temp1[pe_number + 1] / num));
		System.out.println();
		
		rate[0][0] = df.format((float) sum / pe_number);
		rate[0][1] = df.format((float) temp1[pe_number + 1] / num);
		rate[0][2] = df.format((float) temp1[pe_number + 2]/ (temp1[0] * pe_number));
		//rate[0][3] = df.format(endTime-beginTime);
		
		Date b = new Date();
		//DecimalFormat df = new DecimalFormat("0.0000");
		//resultMap.put(algoName, df.format(((double) ER / lineCount)
		interval = df.format((b.getTime() - a.getTime()));
		printInfileFIFO();
	}
	
	/**
	 * 输出结果到文件
	 * @throws IOException
	 */
	protected static void printInfileFIFO() throws IOException {
        String path = multiRes+"fifo.txt";
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, true)));
            out.write(rate[0][0] + "\t" + rate[0][1]+"\t" + rate[0][2] +"\t"+interval+"\r\n");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

	/**
	 * 输出EDF调度结果
	 * 
	 * @param dagdepend
	 *            ，工作流依赖关系
	 * @param num
	 *            ,全部DAG的各个子任务总个数
	 */
	public static void edfresult(DAGdepend dagdepend, int num) throws Throwable {
		Date a = new Date();
		EDF edf = new EDF(pe_number);
		EDF.dag_queue_ori = DAG_queue;
		EDF.course_number = DAG_queue.size();
		EDF.pe_number = pe_number;
		EDF.pe_list = PEList;
		EDF.dagdepend = dagdepend;
		int[] temp2 = new int[pe_number + 3];
		temp2 = EDF.makespan(pe_number);
		

		DecimalFormat df = new DecimalFormat("0.0000");
		double sum = 0;

		System.out.println("EDF:");
		for (int j = 0; j < PEList.size(); j++) {
			sum = (float) temp2[j + 1] / temp2[0] + sum;
		}
		System.out.println("PE's use ratio is "+ df.format((float) sum / pe_number));
		System.out.println("effective PE's use ratio is "+ df.format((float) temp2[pe_number + 2]/ (temp2[0] * pe_number)));
		System.out.println("Task Completion Rates is "+ df.format((float) temp2[pe_number + 1] / num));
		System.out.println();
		rate[1][0] = df.format((float) sum / pe_number);
		rate[1][1] = df.format((float) temp2[pe_number + 1] / num);
		rate[1][2] = df.format((float) temp2[pe_number + 2]/ (temp2[0] * pe_number));
		//rate[1][3] = df.format(endTime-beginTime);
		
		Date b = new Date();
		interval = df.format((b.getTime() - a.getTime()));
		printInfileEDF();
	}
	
	/**
	 * 输出结果到文件
	 * @throws IOException
	 */
	protected static void printInfileEDF() throws IOException {
        String path = multiRes+"edf.txt";
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, true)));
            out.write(rate[1][0] + "\t" + rate[1][1]+"\t" + rate[1][2] +"\t"+interval+"\r\n");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

	/**
	 * 输出STF调度结果
	 * 
	 * @param dagdepend
	 *            ，工作流依赖关系
	 * @param num
	 *            ,全部DAG的各个子任务总个数
	 */
	public static void stfresult(DAGdepend dagdepend, int num) throws Throwable {
		Date a = new Date();
		STF stf = new STF(pe_number);
		STF.dag_queue_ori = DAG_queue;
		STF.course_number = DAG_queue.size();
		STF.pe_number = pe_number;
		STF.pe_list = PEList;
		STF.dagdepend = dagdepend;
		int[] temp3 = new int[pe_number + 3];
		temp3 = STF.makespan(pe_number);

		DecimalFormat df = new DecimalFormat("0.0000");
		double sum = 0;

		System.out.println("STF:");
		for (int j = 0; j < PEList.size(); j++) {
			sum = (float) temp3[j + 1] / temp3[0] + sum;
		}
		System.out.println("PE's use ratio is "+ df.format((float) sum / pe_number));
		System.out.println("effective PE's use ratio is "+ df.format((float) temp3[pe_number + 2]/ (temp3[0] * pe_number)));
		System.out.println("Task Completion Rates is "+ df.format((float) temp3[pe_number + 1] / num));
		System.out.println();
		rate[2][0] = df.format((float) sum / pe_number);
		rate[2][1] = df.format((float) temp3[pe_number + 1] / num);
		rate[2][2] = df.format((float) temp3[pe_number + 2]/ (temp3[0] * pe_number));
		//rate[2][3] = df.format(endTime-beginTime);
		
		Date b = new Date();
		interval = df.format((b.getTime() - a.getTime()));
		printInfileSTF();
	}
	
	/**
	 * 输出结果到文件
	 * @throws IOException
	 */
	protected static void printInfileSTF() throws IOException {
        String path = multiRes+"stf.txt";
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, true)));
            out.write(rate[2][0] + "\t" + rate[2][1]+"\t" + rate[2][2] +"\t"+interval+"\r\n");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

	/**
	 * 输出EFTF调度结果
	 * 
	 * @param dagdepend
	 *            ，工作流依赖关系
	 * @param num
	 *            ,全部DAG的各个子任务总个数
	 */
	public static void eftfresult(DAGdepend dagdepend, int num)
			throws Throwable {
		Date a = new Date();
		EFTF efff = new EFTF(pe_number);
		EFTF.dag_queue_ori = DAG_queue;
		EFTF.course_number = DAG_queue.size();
		EFTF.pe_number = pe_number;
		EFTF.pe_list = PEList;
		EFTF.dagdepend = dagdepend;
		int[] temp4 = new int[pe_number + 3];
		temp4 = EFTF.makespan(pe_number);
		
		DecimalFormat df = new DecimalFormat("0.0000");
		double sum = 0;

		System.out.println("EFTF:");
		for (int j = 0; j < PEList.size(); j++) {
			sum = (float) temp4[j + 1] / temp4[0] + sum;
		}
		System.out.println("PE's use ratio is "+ df.format((float) sum / pe_number));
		System.out.println("effective PE's use ratio is "+ df.format((float) temp4[pe_number + 2]/ (temp4[0] * pe_number)));
		System.out.println("Task Completion Rates is "+ df.format((float) temp4[pe_number + 1] / num));
		System.out.println();
		rate[3][0] = df.format((float) sum / pe_number);
		rate[3][1] = df.format((float) temp4[pe_number + 1] / num);
		rate[3][2] = df.format((float) temp4[pe_number + 2]/ (temp4[0] * pe_number));
		//rate[3][3] = df.format(endTime-beginTime);
		
		Date b = new Date();
		interval = df.format((b.getTime() - a.getTime()));
		//interval = (b.getTime() - a.getTime())/1000;
		printInfileEFTF();
	}
	
	/**
	 * 输出结果到文件
	 * @throws IOException
	 */
	protected static void printInfileEFTF() throws IOException {
        String path = multiRes+"eftf.txt";
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, true)));
            out.write(rate[3][0] + "\t" + rate[3][1]+"\t" + rate[3][2] +"\t"+interval+"\r\n");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

	
	/**
	 * 创建DAGMAP实例并初始化
	 * 
	 * @param dagdepend
	 *            ，工作流依赖关系
	 * @param vcc
	 *            ，计算能力
	 * @return num,全部DAG的各个子任务总个数
	 */
	public static int initdagmap(DAGdepend dagdepend, PEComputerability vcc,String inputXMLPath)
			throws Throwable {
		int pre_exist = 0;
		//File file = new File(System.getProperty("user.dir") + "\\DAG_XML\\"); 
		
		
		File file = new File(inputXMLPath);
		
		String[] fileNames = file.list(); // 
		int num = fileNames.length - 2;// 其中有一个文件是Deadline.txt

		BufferedReader bd = new BufferedReader(new FileReader(inputXMLPath+"Deadline.txt"));
		String buffered;

		for (int i = 0; i < num; i++) {

			boolean isSingle=false;
			DAGdepend dagdepend_persional = new DAGdepend();
			DAG_queue_personal.clear();// 初始化,ArrayList自带的clear()方法

			// 读取文件“Deadline.TXT”。获取DAG的arrivetime和deadline，task个数
			buffered = bd.readLine();
			String bufferedA[] = buffered.split(" ");
			int buff[] = new int[4];
			buff[0] = Integer.valueOf(bufferedA[0].split("dag")[1]).intValue();// dagID
			buff[1] = Integer.valueOf(bufferedA[1]).intValue();// tasknum
			buff[2] = Integer.valueOf(bufferedA[2]).intValue();// arrivetime
			buff[3] = Integer.valueOf(bufferedA[3]).intValue();// deadline
			int tasknum = buff[1];
			if(tasknum==1)
				isSingle=true;
			int arrivetime = buff[2];
			int deadline = buff[3];

			//构造各个DAG中子任务间的依赖关系
			pre_exist = initDAG_createDAGdepend_XML(i, pre_exist, tasknum,arrivetime,inputXMLPath);

			vcc.setComputeCostMap(ComputeCostMap);
			vcc.setAveComputeCostMap(AveComputeCostMap);
			
			dagdepend_persional.setDAGList(DAG_queue_personal);
			dagdepend_persional.setDAGDependMap(DAGDependMap_personal);
			dagdepend_persional.setDAGDependValueMap(DAGDependValueMap_personal);

			//为DAG_queue以及DAG_queue_personal中的内容设置deadline
			createDeadline_XML(deadline, dagdepend_persional);
			
			int number_1 = DAG_queue.size();
			int number_2 = DAG_queue_personal.size();
			for (int k = 0; k < number_2; k++) {
				DAG_queue.get(number_1 - number_2 + k).setdeadline(DAG_queue_personal.get(k).getdeadline());
			}

			double makespan = HEFT(DAG_queue_personal, dagdepend_persional);
			DAGoriComparator comparator = new DAGoriComparator();
			Collections.sort(DAG_queue_personal, comparator);

			int Criticalnumber = 0;
			if(isSingle){
				Criticalnumber=1;
			}else {
				Criticalnumber=CriticalPath(DAG_queue_personal,dagdepend_persional);
			}
			setNewDeadline(DAG_queue_personal, dagdepend_persional, arrivetime,
					deadline, makespan, Criticalnumber);
			for (int k = 0; k < number_2; k++) {
				DAG_queue.get(number_1 - number_2 + k).setnewdeadline(DAG_queue_personal.get(k).getnewdeadline());
			}
			for (int k = 0; k < number_2; k++) {
				//System.out.println("orideadline:"+ DAG_queue_personal.get(k).getdeadline()+ " newdeadline:"+ DAG_queue_personal.get(k).getnewdeadline());
			}

			clear();
		}

		dagdepend.setDAGList(DAG_queue);
		dagdepend.setDAGDependMap(DAGDependMap);
		dagdepend.setDAGDependValueMap(DAGDependValueMap);

		return num;
	}

	/**
	 * 清空PELIST
	 */
	private static void clear() throws Throwable {
		PEList.clear();
		initPE();
	}

	/**
	 * 计算调度结果中的关键路径
	 * 是自最后一个节点往前面找的，计算是以父节点的heft_eft+数据传输开销最大值为准
	 * 
	 * @param dagqueue_heft，DAG中各个子任务
	 * @param dagdepend_heft，DAG中任务间依赖关系
	 * @return Criticalnumber，关键路径上的子任务个数
	 */
	private static int CriticalPath(ArrayList<Task> dagqueue_heft,DAGdepend dagdepend_heft) {
		int Criticalnumber = 0;	//关键路径上任务数目
		int i = dagqueue_heft.size() - 1;
		//自后往前计算
		while (i >= 0) {
			if (i == (dagqueue_heft.size() - 1)) {//如果这个任务是整个DAG图中的最后一个任务
				dagqueue_heft.get(i).setinCriticalPath(true);
				Criticalnumber++;
			}

			int max = -1;
			int maxid = -1;
			Iterator<Integer> it = dagqueue_heft.get(i).getpre().iterator();
			while (it.hasNext()) {
				int pretempid = it.next();
				int temp = (int) ((int) dagqueue_heft.get(pretempid).getheftaft() 
						+ (double) dagdepend_heft.getDAGDependValueMap().get(pretempid + " " + i));
				if (temp > max) {
					max = temp;
					maxid = pretempid;
				}
			}
			dagqueue_heft.get(maxid).setinCriticalPath(true);
			Criticalnumber++;
			i = maxid;
			if (maxid == 0)
				i = -1;
		}
		
		return Criticalnumber;
	}

	/**
	 * 通过HEFT算法的调度结果，对每个子任务计算deadline
	 * 
	 * @param dagqueue_heft，DAG（一个）中各个子任务
	 * @param dagdepend_heft，DAG（一个）中任务间依赖关系
	 * @param arrivetime，DAG（当前，一个）到达时间
	 * @param deadline，DAG截止时间
	 * @param makespan，HEFT调度结果的makespan
	 * @param Criticalnumber，关键路径上的子任务个数
	 */
	private static void setNewDeadline(ArrayList<Task> dagqueue_heft,
			DAGdepend dagdepend_heft, int arrivetime, int deadline,
			double makespan, int Criticalnumber) {
		
		double redundancy = ((deadline - arrivetime) - makespan);
		double preredundancy = (redundancy / Criticalnumber);
		int cnum = Criticalnumber;

		for (int i = dagqueue_heft.size() - 1; i >= 0; i--) {
			if (dagqueue_heft.get(i).getinCriticalPath()) {//当前任务在关键路径上
				int newdeadline = (int) ((int) dagqueue_heft.get(i).getheftaft() 
						+ dagqueue_heft.get(i).getarrive() + preredundancy* cnum);
				dagqueue_heft.get(i).setnewdeadline(newdeadline);
				cnum--;
				
				//获取父节点
				Iterator<Integer> it = dagqueue_heft.get(i).getpre().iterator();
				while (it.hasNext()) {
					int pretempid = it.next();
					int dead = (int) (dagqueue_heft.get(i).getnewdeadline() 
							- (double) dagdepend_heft.getDAGDependValueMap().get(pretempid + " " + i));
					dagqueue_heft.get(pretempid).setnewdeadline(dead);
				}
			} else {//当前任务不在关键路径上
				if (dagqueue_heft.get(i).getnewdeadline() != 0) {
					Iterator<Integer> it = dagqueue_heft.get(i).getpre().iterator();
					while (it.hasNext()) {
						int pretempid = it.next();
						int dead = (int) (dagqueue_heft.get(i).getnewdeadline() 
								- (double) dagdepend_heft.getDAGDependValueMap().get(pretempid + " " + i));
						dagqueue_heft.get(pretempid).setnewdeadline(dead);
					}
				}
			}
			
			
		}

		
		
		for (int i = dagqueue_heft.size() - 1; i >= 0; i--) {
			if (dagqueue_heft.get(i).getnewdeadline() == 0) {
				dagqueue_heft.get(i).setnewdeadline(dagqueue_heft.get(dagqueue_heft.size() - 1).getnewdeadline());
			}
		}

	}

	/**
	 * 根据DAX文件为DAG添加相互依赖关系
	 * 
	 * @param i，DAGID，当前DAG
	 * @param preexist，将所有的工作流（一个工作流就是一个DAG）中子任务全部添加到一个队列，在本DAG前已有preexist个任务
	 * @param tasknumber，DAG中任务个数
	 * @param arrivetimes ，DAG到达时间
	 * @return back，将所有的工作流中子任务全部添加到一个队列，在本DAG全部添加后，有back个任务
	 */
	@SuppressWarnings("rawtypes")
	private static int initDAG_createDAGdepend_XML(int i, int preexist,int tasknumber, int arrivetimes,String pathXML) throws NumberFormatException,
			IOException, JDOMException {

		int back = 0;
		DAGDependMap_personal = new HashMap<Integer, Integer>();	//每个DAG自有的，里面是当前DAG的子任务下的最后一个output的对应关系
		DAGDependValueMap_personal = new HashMap<String, Double>();		//每个DAG自有的
		ComputeCostMap = new HashMap<Integer, int[]>();		//每个DAG自有的
		AveComputeCostMap = new HashMap<Integer, Integer>();	//每个DAG自有的

		// 获取XML解析器
		SAXBuilder builder = new SAXBuilder();
		
		// 获取document对象
		Document doc = builder.build(pathXML+"/dag" + (i + 1) + ".xml");
		
		// 获取根节点
		Element adag = doc.getRootElement();

		for (int j = 0; j < tasknumber; j++) {
			Task dag = new Task();	//添加到 所有工作流中子任务全部所在的一个队列（在本DAG前已有preexist个任务）的DAG
									//其实它本身不应该看作为一个DAG，而是DAG中的各个子任务
			Task dag_persional = new Task();	//

			dag.setid(Integer.valueOf(preexist + j).intValue());//编号设置为（preexist+子任务编号）
			dag.setarrive(arrivetimes);
			dag.setdagid((i + 1));//该子任务所属DAG的输入时原始编号
			dag_persional.setid(Integer.valueOf(j).intValue());
			dag_persional.setarrive(arrivetimes);
			dag_persional.setdagid((i + 1));//该子任务所属DAG的输入时原始编号

			XPath path = XPath.newInstance("//job[@id='" + j + "']/@tasklength");
			List list = path.selectNodes(doc);
			Attribute attribute = (Attribute) list.get(0);
			int x = Integer.valueOf(attribute.getValue()).intValue();	//得到任务长度
			dag.setlength(x);	//该子任务的长度
			dag_persional.setlength(x);		//该子任务的长度

			//标记本DAG中的最后一个任务
			if (j == tasknumber - 1) {
				dag.setislast(true);
				islastnum++;	//总共有多少个最终任务
			}

			DAG_queue.add(dag);
			DAG_queue_personal.add(dag_persional);

			int sum = 0;
			int[] bufferedDouble = new int[PEList.size()];
			for (int k = 0; k < PEList.size(); k++) {
				bufferedDouble[k] = Integer.valueOf(x/ PEList.get(k).getability());
				sum = sum + Integer.valueOf(x / PEList.get(k).getability());
			}
			ComputeCostMap.put(j, bufferedDouble);//DAG的某个子任务与在每个处理器上的执行时间的映射
			AveComputeCostMap.put(j, (sum / PEList.size()));//DAG的某个子任务在每个处理器上的执行时间的平均值
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
			Attribute attribute2 = (Attribute) list2.get(0);	//任务间数据传输的花销，也就是DAG中子任务链接的边的值
			int datasize = Integer.valueOf(attribute2.getValue()).intValue();

			DAGDependMap.put(presuc[0], presuc[1]);		//构建DAG中子任务之间的链接MAP
			DAGDependValueMap.put((presuc[0] + " " + presuc[1]),(double) datasize); 	//构建DAG中子任务之间的链接MAP的边的值

			//添加DAG的子任务之间的关联
			DAG_queue.get(presuc[0]).addToSuc(presuc[1]);
			DAG_queue.get(presuc[1]).addToPre(presuc[0]);
			DAGDependMap_personal.put(Integer.valueOf(pre_suc[0]).intValue(),Integer.valueOf(pre_suc[1]).intValue());
			DAGDependValueMap_personal.put((pre_suc[0] + " " + pre_suc[1]),(double) datasize);

			int tem0 = Integer.parseInt(pre_suc[0]);
			int tem1 = Integer.parseInt(pre_suc[1]);
			DAG_queue_personal.get(tem0).addToSuc(tem1);
			DAG_queue_personal.get(tem1).addToPre(tem0);

		}

		back = preexist + tasknumber;
		return back;//返回当前的preexist的值
	}

	/**
	 * 为DAG根据deadline，给每个子任务计算相应的最迟截止时间
	 * 选择当前任务的所有子任务中（子任务结束时间-子任务执行时间）中最小的那么值作为当前任务的deadline
	 * @param dead_line，DAG的deadline           
	 * @param dagdepend_persion，DAG中相互依赖关系    ，其实没有用到
	 */
	private static void createDeadline_XML(int dead_line,DAGdepend dagdepend_persion) throws Throwable {
		int maxability = 1;
		int max = 10000;

		//计算截止时间是从后往前计算
		for (int k = DAG_queue_personal.size() - 1; k >= 0; k--) {
			ArrayList<Task> suc_queue = new ArrayList<Task>();
			ArrayList<Integer> suc = new ArrayList<Integer>();
			suc = DAG_queue_personal.get(k).getsuc();
			//选择当前任务的所有子任务中（子任务结束时间-子任务执行时间）中最小的那么值作为当前任务的deadline
			if (suc.size() > 0) {
				for (int j = 0; j < suc.size(); j++) {
					int tem = 0;
					Task buf3 = new Task();
					buf3 = getDAGById(suc.get(j));
					suc_queue.add(buf3);
					tem = (int) (buf3.getdeadline() - (buf3.getlength() / maxability));
					if (max > tem)
						max = tem;
				}
				DAG_queue_personal.get(k).setdeadline(max);
			} else {//当前的任务是所属DAG的叶子任务或者是结束任务
				DAG_queue_personal.get(k).setdeadline(dead_line);
			}
		}
	}

	/**
	 * 
	 * @Title: initPE
	 * @Description: 创建PE实例并初始化
	 * @param @throws Throwable
	 * @return void
	 * @throws
	 */
	private static void initPE() throws Throwable {

		for (int i = 0; i < pe_number; i++) {
			PE pe = new PE();
			pe.setID(i);
			pe.setability(1);
			pe.setfree(true);
			pe.setAvail(0);
			PEList.add(pe);
		}
	}

	/**
	 * 根据TASKID返回该TASK实例
	 * 
	 * @param dagId
	 *            ，TASKID
	 * @return DAG，TASK实例
	 */
	private static Task getDAGById(int dagId) {
		for (Task dag : DAG_queue_personal) {
			if (dag.getid() == dagId)
				return dag;
		}
		return null;
	}

	/**
	 * 用HEFT对每个DAG预先进行调度
	 * 
	 * @param dagqueue_heft，DAG中各个子任务===》DAG_queue_personal
	 * @param dagdepend_heft，DAG中任务间依赖关系====》dagdepend_persional
	 * @return makespan，HEFT调度结果的makespan
	 */
	public static double HEFT(ArrayList<Task> dagqueue_heft,DAGdepend dagdepend_heft) throws Throwable {
		
		DAGExeTimeMap = new HashMap<Integer, double[]>();
		DAGIdToDAGMap = new HashMap<Integer, Task>(); 	//由DAG的ID到其DAG对象的对应
		upRankValueMap = new HashMap<Integer, Double>();	//计算出的RANK值
		
		//为HEFT计算做准备，求取当前DAG的子任务在各个处理器上的平均执行时间
		createVmComputeCost(dagqueue_heft);
		
		//构建id--》DAG(其实是其子任务)的映射
		for (int i = 0; i < dagqueue_heft.size(); i++) {
			DAGIdToDAGMap.put(i, dagqueue_heft.get(i));   
		}
		
		//计算各个rank值
		computeUpRankValue(dagqueue_heft, dagdepend_heft);
		
		//按照rank值从大到小排列
		DAGComparator comparator = new DAGComparator();
		Collections.sort(dagqueue_heft, comparator);
		
		double makespan = assignVm_(dagqueue_heft, dagdepend_heft);
		return makespan;
	}

	/**
	 * HEFT初始化
	 * @param dagqueue_heft，DAG中各个子任务           
	 */
	private static void createVmComputeCost(ArrayList<Task> dagqueue_heft1)throws IOException {
		vmComputeCostMap = new HashMap<Integer, double[]>();
		vmAveComputeCostMap = new HashMap<Integer, Double>();
		int num = 0;
		for (int i = 0; i < dagqueue_heft1.size(); i++) {
			double sum = 0;
			double ComputeCost[] = new double[pe_number];
			for (int j = 0; j < pe_number; j++) {
				ComputeCost[j] = dagqueue_heft1.get(i).getlength();
				sum += ComputeCost[j];
			}
			vmComputeCostMap.put(num, ComputeCost);	//其实就是当前DAG每个子任务的长度
			vmAveComputeCostMap.put(num, sum / pe_number);	//其实就是当前DAG每个子任务的长度
			num++;
		}
	}

	/**
	 * HEFT计算优先级，就是论文上的公式
	 * 不考虑父任务和子任务是否在同一处理器上
	 * 
	 * @param dagqueue_heft，DAG中各个子任务（独自）
	 * @param dagdepend_heft，DAG中任务间依赖关系（独自）
	 */
	public static void computeUpRankValue(ArrayList<Task> dagqueue_heft2,DAGdepend dagdepend_heft2) {
		//rank的计算是自下往上进行的
		for (int i = dagqueue_heft2.size() - 1; i >= 0; i--) {
			//设置任务i在各个处理器上的平均处理时间   computerability.getAveComputeCost(i)默认返回1
			dagqueue_heft2.get(i).setUpRankValue(dagqueue_heft2.get(i).getlength()/ PEComputerability.getAveComputeCost(i));
			
			double tem = 0;
			Iterator<Integer> it = dagqueue_heft2.get(i).getsuc().iterator();
			while (it.hasNext()) {
				int sucCloudletIdTem = it.next();
				double valuetemp = dagqueue_heft2.get(sucCloudletIdTem).getUpRankValue()+ (double) dagdepend_heft2.getDAGDependValueMap().get(String.valueOf(i) + " "+ String.valueOf(sucCloudletIdTem))
						/ PEComputerability.getAveComputeCost(i);//computerability.getAveComputeCost(i);这里以这个值作为人物间传输速度，
																//此时没有考虑两个任务是否在同一个处理器上
				if (valuetemp > tem) {
					tem = valuetemp;
				}
			}
			
			tem += dagqueue_heft2.get(i).getUpRankValue();
			tem = (int) (tem * 1000) / 1000.0;	//转化为浮点数
			dagqueue_heft2.get(i).setUpRankValue(tem);
		}
	}

	/**
	 * 用HEFT对每个DAG预先进行调度
	 * 
	 * @param dagqueue_heft，当前DAG中各个子任务（独自）
	 * @param dagdepend_heft，当前DAG中各任务间依赖关系（独自）
	 * @return makespan，HEFT调度结果的makespan
	 */
	public static double assignVm_(ArrayList<Task> dagqueue_heft3,DAGdepend dagdepend_heft3) {

		double makespan = 0;
		
		/* 分配第一个任务 */
		cloudletInVm = new HashMap<Integer, Integer[]>();
		cloudletInVmId = new HashMap<Integer, Integer>();
		DecimalFormat df = new DecimalFormat("#.##");
		double temp = Integer.MAX_VALUE;
		int vmIdTem = -1;
		int[] num = new int[pe_number];
		Integer[][] cloudletinvm = new Integer[pe_number][100];// 前一个代表有多少个处理器，后一个代表一个DAG中有多少个子任务
		double exetime = 0;
		double[] time = new double[2];
		
		
		for (int firTem = 0; firTem < pe_number; firTem++) {
			if (vmComputeCostMap.get(0)[firTem] < temp) {
				temp = vmComputeCostMap.get(0)[firTem];
				vmIdTem = firTem;
			}
		}
		
		time[0] = PEList.get(vmIdTem).getAvail();
		time[1] = time[0] + vmComputeCostMap.get(0)[vmIdTem];
		DAGExeTimeMap.put(0, time);
		PEList.get(vmIdTem).setast(num[vmIdTem], time[0]);
		PEList.get(vmIdTem).setaft(num[vmIdTem], time[1]);
		num[vmIdTem]++;
		PEList.get(vmIdTem).setAvail(time[1]);
		cloudletinvm[vmIdTem][0] = 0;

		cloudletInVmId.put(0, vmIdTem);
		exetime = DAGExeTimeMap.get(0)[1] - DAGExeTimeMap.get(0)[0];
		//System.out.println("cloudlet 0" + "	ast:" + time[0] + "		aft:"+ time[1] + "		processor:" + (vmIdTem + 1) + "	pes:"+ dagqueue_heft3.get(0).getpeid() + "	exeTime:" + exetime);
		dagqueue_heft3.get(0).setheftast(time[0]);
		dagqueue_heft3.get(0).setheftaft(time[1]);

		
		/* 分配其他的任务 */
		for (int iAssignTem = 1; iAssignTem < dagqueue_heft3.size(); iAssignTem++) {

			int cloudletIdCurrent = dagqueue_heft3.get(iAssignTem).getid();
			double[] timeTemp = new double[2];

			int vmIdTemp = -1;
			boolean success = false;
			timeTemp[1] = Integer.MAX_VALUE;

			/* 基于插入 */
			for (int Assigntemp = 0; Assigntemp < PEList.size(); Assigntemp++) {
				for (int i = 0; i < num[Assigntemp]; i++) {
					Iterator<Integer> it = DAGIdToDAGMap.get(cloudletIdCurrent)
							.getpre().iterator();
					double temp_1 = 0;
					double sum = 0;
					int cloudletIdTemp = 0;
					while (it.hasNext()) {
						/* 取出该任务前驱任务id */
						int pretempid = it.next();
						double pretemp;
						// System.out.println(cloudletIdCurrent+" "+pretempid+" "+cloudletInVmId.get(pretempid)+" "+Assigntemp);
						if (cloudletInVmId.get(pretempid) == Assigntemp) {
							pretemp = DAGExeTimeMap.get(pretempid)[1];
						} else {
							pretemp = DAGExeTimeMap.get(pretempid)[1]
									+ (double) dagdepend_heft3
											.getDAGDependValueMap()
											.get(String.valueOf(pretempid)
													+ " "
													+ String.valueOf(cloudletIdCurrent));
						}

						if (pretemp > temp_1) {
							temp_1 = pretemp;
							cloudletIdTemp = pretempid;
						}
					}
					sum += temp_1;
					sum += vmComputeCostMap.get(cloudletIdCurrent)[Assigntemp];
					if (PEList.get(Assigntemp).getast(i) != 0 && i == 0) {
						if (temp_1 >= 0
								&& PEList.get(Assigntemp).getast(i) >= sum) {
							vmIdTemp = Assigntemp;
							timeTemp[0] = temp_1;
							timeTemp[1] = sum;
							success = true;
							break;
						} else {
							continue;
						}
					} else if (PEList.get(Assigntemp).getast(i) == 0 && i == 0) {
						continue;
					} else {
						if (PEList.get(Assigntemp).getast(i) >= sum
								&& (PEList.get(Assigntemp).getast(i) - PEList
										.get(Assigntemp).getaft(i - 1)) >= vmComputeCostMap
										.get(cloudletIdCurrent)[Assigntemp]) {
							if (temp_1 < PEList.get(Assigntemp).getaft(i - 1)) {
								timeTemp[0] = PEList.get(Assigntemp).getaft(
										i - 1);
								timeTemp[1] = PEList.get(Assigntemp).getaft(
										i - 1)
										+ vmComputeCostMap
												.get(cloudletIdCurrent)[Assigntemp];
							} else {
								timeTemp[0] = temp_1;
								timeTemp[1] = sum;
							}
							vmIdTemp = Assigntemp;
							success = true;
							break;
						} else {
							continue;
						}
					}
				}
				if (success) {
					break;
				}
			}

			if (success) {
				DAGExeTimeMap.put(cloudletIdCurrent, timeTemp);
				PEList.get(vmIdTemp).setast(num[vmIdTemp], timeTemp[0]);
				PEList.get(vmIdTemp).setaft(num[vmIdTemp], timeTemp[1]);

				DAGIdToDAGMap.get(cloudletIdCurrent).setinserte(true);
				cloudletinvm[vmIdTemp][num[vmIdTemp]] = cloudletIdCurrent;
				int q = 0;
				int n = num[vmIdTemp];
				for (int p = num[vmIdTemp] - 1; p >= 0; p--) {
					if (DAGExeTimeMap.get(cloudletinvm[vmIdTemp][p])[0] > timeTemp[0]) {
						q = cloudletinvm[vmIdTemp][p];
						cloudletinvm[vmIdTemp][p] = cloudletIdCurrent;
						cloudletinvm[vmIdTemp][n] = q;
						n = p;
					}
				}
				dagqueue_heft3.get(iAssignTem).setinserte(true);
				num[vmIdTemp]++;
				PEList.get(vmIdTemp).setAvail(timeTemp[1]);
				cloudletInVmId.put(cloudletIdCurrent, vmIdTemp);
				exetime = DAGExeTimeMap.get(cloudletIdCurrent)[1]
						- DAGExeTimeMap.get(cloudletIdCurrent)[0];
				//System.out.println("cloudlet " + (cloudletIdCurrent) + "	ast:"+ timeTemp[0] + "		aft:" + timeTemp[1] + "	processor:"+ (vmIdTemp + 1) + "	pes:"+ dagqueue_heft3.get(iAssignTem).getpeid()+ "	exeTime:" + exetime);

				dagqueue_heft3.get(iAssignTem).setheftast(timeTemp[0]);
				dagqueue_heft3.get(iAssignTem).setheftaft(timeTemp[1]);
				// System.out.println(dagqueue_heft3.get(iAssignTem).getid()+" "+dagqueue_heft3.get(iAssignTem).getheftast()+" "+dagqueue_heft3.get(iAssignTem).getheftaft());

				continue;
			}

			for (int jAssignTem = 0; jAssignTem < PEList.size(); jAssignTem++) {
				/* 当前任务在每个处理器上暂定的est记为temEST */
				double temEST = PEList.get(jAssignTem).getAvail();
				/* 当前任务的每个前驱任务传输数据完毕时刻的最大值为tem */
				double tem = 0;
				/* 选定前驱任务id */
				int cloudletIdTemp = 0;

				Iterator<Integer> it = DAGIdToDAGMap.get(cloudletIdCurrent)
						.getpre().iterator();

				while (it.hasNext()) {
					/* 取出该任务前驱任务id */
					int preTempId = it.next();
					/* 当前任务的每个前驱任务传输数据完毕时间 */
					double preTem;

					if (cloudletInVmId.get(preTempId) == jAssignTem) {
						preTem = DAGExeTimeMap.get(preTempId)[1];
					} else {
						preTem = DAGExeTimeMap.get(preTempId)[1]
								+ (double) dagdepend_heft3
										.getDAGDependValueMap()
										.get(String.valueOf(preTempId)
												+ " "
												+ String.valueOf(cloudletIdCurrent));
					}

					if (preTem > tem) {
						tem = preTem;
						cloudletIdTemp = preTempId;
					}
				}
				temEST = (temEST > tem) ? temEST : tem;
				if ((temEST + vmComputeCostMap.get(cloudletIdCurrent)[jAssignTem]) < timeTemp[1]) {
					timeTemp[0] = temEST;
					timeTemp[1] = temEST
							+ vmComputeCostMap.get(cloudletIdCurrent)[jAssignTem];
					vmIdTemp = jAssignTem;
				}
			}
			DAGExeTimeMap.put(cloudletIdCurrent, timeTemp);
			PEList.get(vmIdTemp).setast(num[vmIdTemp], timeTemp[0]);
			PEList.get(vmIdTemp).setaft(num[vmIdTemp], timeTemp[1]);

			cloudletinvm[vmIdTemp][num[vmIdTemp]] = cloudletIdCurrent;
			num[vmIdTemp]++;
			PEList.get(vmIdTemp).setAvail(timeTemp[1]);
			cloudletInVmId.put(cloudletIdCurrent, vmIdTemp);
			exetime = DAGExeTimeMap.get(cloudletIdCurrent)[1]
					- DAGExeTimeMap.get(cloudletIdCurrent)[0];
			//System.out.println("cloudlet " + (cloudletIdCurrent) + "	ast:"+ timeTemp[0] + "		aft:" + timeTemp[1] + "	processor:"+ (vmIdTemp + 1) + "	pes:"+ dagqueue_heft3.get(iAssignTem).getpeid() + "	exeTime:"+ exetime);

			dagqueue_heft3.get(iAssignTem).setheftast(timeTemp[0]);
			dagqueue_heft3.get(iAssignTem).setheftaft(timeTemp[1]);
			// System.out.println(dagqueue_heft3.get(iAssignTem).getid()+" "+dagqueue_heft3.get(iAssignTem).getheftast()+" "+dagqueue_heft3.get(iAssignTem).getheftaft());

			makespan = timeTemp[1];
		}

		for (int i = 0; i < PEList.size(); i++) {
			cloudletInVm.put(i, cloudletinvm[i]);
		}

		return makespan;

	}

}
