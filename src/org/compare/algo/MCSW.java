package org.compare.algo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.Map.Entry;

import org.jdom.xpath.XPath;
import org.jdom.Attribute;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.schedule.model.Task;
import org.schedule.model.DAG;
import org.schedule.model.DAGdepend;
import org.schedule.model.PE;
import org.schedule.model.PEComputerability;
import org.schedule.model.Slot;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.poi.hslf.exceptions.HSLFException;
import org.apache.poi.hssf.record.cont.ContinuableRecord;
import org.generate.util.CommonParametersUtil;
import org.generate.util.RandomParametersUtil;

/**
 * 
 * @ClassName: MergeDAG
 * @Description: 
 * 1、合并起始几个相同时间提交的任务为一个大的dag图合并成一个DAG，当其调度失败的时候取出其中成功的任务作为调度内容
 * 				
 * @author YanWenjing
 * @date 2017-10-21 下午7:45:26
 */
public class MCSW {

	public static int finishTaskCount=0;
	private static ArrayList<PE> PEList;
	private static ArrayList<DAG> DAGMapList;
	private static ArrayList<DAG> tempDAGMapList;

	private static ArrayList<Task> DAG_queue;
	private static ArrayList<Task> readyqueue;

	private static HashMap<Integer, Integer> DAGDependMap;
	private static HashMap<String, Double> DAGDependValueMap;

	private static ArrayList<Task> DAG_queue_personal;
	private static HashMap<Integer, Integer> DAGDependMap_personal;
	private static HashMap<String, Double> DAGDependValueMap_personal;
	private static Map<Integer, int[]> ComputeCostMap;
	private static Map<Integer, Integer> AveComputeCostMap;

//	private static Map<Integer, DAG> DAGIdToDAGMap;
//	public static Map<Integer, double[]> DAGExeTimeMap;
//	private static Map<Integer, Double> upRankValueMap;
	private static Map<Integer, double[]> vmComputeCostMap;
	private static Map<Integer, Double> vmAveComputeCostMap;
//	private static Map<Integer, Integer[]> cloudletInVm;
//	private static Map<Integer, Integer> cloudletInVmId;

	// 写入文件的结果
	public static String[][] rateResult = new String[1][4];

	private static int islastnum = 0;
	private static double deadLineTimes = 1.3;// deadline的倍数值 （1.1，1.3，1.6，2.0）
	private static int pe_number = 8;

	public static String[][] rate = new String[5][2];

	public static int current_time;
	public static int proceesorEndTime = CommonParametersUtil.timeWindow;// 时间窗
	public static int timeWindow;
	public static int T = 1;

	public static int fillbacktasknum = 10;
	public static int[][] message;
	public static int dagnummax = 10000;
	public static int timewindowmax = 9000000;
	public static int mesnum = 5;
	private static HashMap<Integer, ArrayList> SlotListInPes;
	private static HashMap<Integer, HashMap> TASKListInPes;

	// 处理器上的后推标记
	private static int[] pushFlag;
	// 需要后推的任务数
	private static int pushCount = 0;
	// 后推成功总次数
	private static int pushSuccessCount = 0;

	// 总任务数目
	private static int taskTotal = 0;
	private static int[][] dagResultMap = null;
	
	//合并后的总长度
	static List<Integer> mergeId;//被合并的作业的原始id集合
	static ArrayList<Integer> mergeDAGEndNode;//合并的大作业中每个小作业的最后一个任务的集合
	static ArrayList<Integer> successMergeJob;//合并的大作业中，成功调度的原始作业id


	public MCSW() {
		readyqueue = new ArrayList<Task>();
		DAG_queue = new ArrayList<Task>();
		DAG_queue_personal = new ArrayList<Task>();
		PEList = new ArrayList<PE>();
		DAGMapList = new ArrayList<DAG>();
		tempDAGMapList=new ArrayList<DAG>();
		DAGDependMap = new HashMap<Integer, Integer>();
		DAGDependValueMap = new HashMap<String, Double>();
		deadLineTimes = CommonParametersUtil.deadLineTimes;
		pe_number = CommonParametersUtil.processorNumber;
		current_time = 0;
		timeWindow = proceesorEndTime / pe_number;

		pushFlag = new int[pe_number];
		dagResultMap = new int[1000][dagnummax];

		message = new int[dagnummax][mesnum];
		SlotListInPes = new HashMap<Integer, ArrayList>(); // 各个处理器上的空闲段信息

		TASKListInPes = new HashMap<Integer, HashMap>();
		for (int i = 0; i < pe_number; i++) {
			HashMap<Integer, Integer[]> TASKInPe = new HashMap<Integer, Integer[]>();
			TASKListInPes.put(i, TASKInPe);
			// 数组0代表task开始时间，1代表task结束时间，2代表dagid，3代表id
		}
		successMergeJob=new ArrayList<>();
		
	}

	/**
	 * 
	 * @Title: runMakespan
	 * @Description: 开始fillback算法
	 * @param @throws Throwable
	 * @return void
	 * @throws
	 */
	public void runMakespan(String pathXML, String resultPath) throws Throwable {

		// init dagmap
		MCSW fb = new MCSW();
		DAGdepend dagdepend = new DAGdepend();
		PEComputerability vcc = new PEComputerability();

		// 初始化处理器
		initPE();

		// 初始化输入xml
		initdagmap(dagdepend, vcc, pathXML);

		Date begin = new Date();
		Long beginTime = begin.getTime();
		// 调度第一个DAG，初始化各个处理器上的任务分布、空闲段、各个任务的松弛情况

		// 设置当前时间是第一个DAG 的到达时间
		current_time = DAGMapList.get(0).getsubmittime();

		// 开始调度后续的作业
		for (int i = 0; i < DAGMapList.size(); i++) {

			HashMap<Integer, ArrayList> SlotListInPestemp = new HashMap<Integer, ArrayList>();
			HashMap<Integer, HashMap> TASKListInPestemp = new HashMap<Integer, HashMap>();

			// 获得适应本作业范围内的空闲块内容
			computeSlot(DAGMapList.get(i).getsubmittime(), DAGMapList.get(i).getDAGdeadline());
			//printTaskAndSlot();
			SlotListInPestemp = copySlot();
			TASKListInPestemp = copyTASK();

			scheduleOtherDAG(i, SlotListInPestemp, TASKListInPestemp);
			

		}
//		printTaskAndSlot();

		
//		for(int j=0;j<DAGMapList.size();j++){
//			System.out.println("当前作业："+j+"\t是否调度成功："+DAGMapList.get(j).getfillbackdone());
//		}
//		
		
		
		
		Date end = new Date();
		Long endTime = end.getTime();
		Long diff = endTime - beginTime;

		outputresult(diff, resultPath);

		storeresultShow();

	}
		

	/**
	 * 输出处理器资源利用率和任务完成率
	 */
	public static void outputresult(Long diff, String resultPath) {
		int suc = 0;
		int fault = 0;
		int effective = 0;
		int notEffective=0;
		int tempp = timeWindow;

		//System.out.println("第一个会不会成功+"+DAGMapList.get(0).getfillbackdone());
		int successCount=0;
		int faultCount=0;
		
		for (int j = 0; j < DAGMapList.size(); j++) {
			ArrayList<Task> DAGTaskList = new ArrayList<Task>();
			for (int i = 0; i < DAGMapList.get(j).gettasklist().size(); i++) {
				Task dag_temp = (Task) DAGMapList.get(j).gettasklist().get(i);
				DAGTaskList.add(dag_temp);
			}
			
			
			if(j==0){	
				if(DAGMapList.get(0).getfillbackdone()==true){
					if(DAGMapList.get(0).isMerge()){//判断第一个任务是否是合并而来的，是的话里面不一定每个任务都是成功了的
						int successMergeCount=0;
						for(int i=0;i<DAGTaskList.size();i++){
							if(DAGTaskList.get(i).getfillbackdone()==true){
								effective = effective + DAGTaskList.get(i).getts();
								successMergeCount++;
							}else{
								notEffective = notEffective +DAGTaskList.get(i).getts();
							}
						}
					//	System.out.println("第一个作业中成功的任务个数："+successMergeCount);			
					}else{//不是合并出来的，就是单个的，证明的确每一个都是成功了的
						for(int i=0;i<DAGTaskList.size();i++){
							if(DAGTaskList.get(i).getfillbackdone()==true)
								effective = effective + DAGTaskList.get(i).getts();	
						}
					}			
					successCount=successMergeJob.size();
					faultCount=mergeId.size()-successCount;
					//System.out.println("第一个任务里面完成的个数"+successCount+"\t失败的个数："+faultCount);
				}else{	
					for(int i=0;i<DAGTaskList.size();i++){
						notEffective = notEffective +DAGTaskList.get(i).getts();
					}
					faultCount=mergeId.size();
				//	System.out.println("第一个任务里面完成的个数"+successCount+"\t失败的个数："+faultCount);
					continue;
				}	
			//	System.out.println("effective="+effective);
		
			}else{//不是第一个作业的值
				if (DAGMapList.get(j).getfillbackdone()) {
					successCount++;
					System.out.println("dag" + DAGMapList.get(j).getDAGId()+ "调度成功，成功执行");
					suc++;
					for (int i = 0; i < DAGTaskList.size(); i++) {
						if(DAGTaskList.get(i).getfillbackdone()){
							effective = effective + DAGTaskList.get(i).getts();
						}else {	
							notEffective = notEffective +DAGTaskList.get(i).getts();
						}
						
					}
				}else{
					faultCount++;
					for (int i = 0; i < DAGMapList.get(j).gettasklist().size(); i++) 
						notEffective = notEffective + DAGTaskList.get(i).getts();
				}
			}				
		}
		
		

		System.out.println("MCSW有" + successCount + "个任务调度完成");
		System.out.println("MCSW有" + faultCount + "个任务调度失败》》》");


		DecimalFormat df = new DecimalFormat("0.0000");
		System.out.println("MCSW:");
		System.out.println("PE's use ratio is "+ df.format((float) effective / (pe_number * tempp))+"\teffective="+effective);
		System.out.println("PE's no use ratio is "+ df.format((float) notEffective / (pe_number * tempp))+"\tnotEffective="+notEffective);
		System.out.println("effective PE's use ratio is "+ df.format((float) effective / (tempp * pe_number)));
		System.out.println("Task Completion Rates is "+ df.format((float) successCount / tempDAGMapList.size()));
		System.out.println();

		rateResult[0][0] = df.format((float) effective / (pe_number * tempp));
		rateResult[0][1] = df.format((float) effective / (tempp * pe_number));
		rateResult[0][2] = df.format((float) successCount / tempDAGMapList.size());

		rateResult[0][3] = df.format(diff);

//		System.out.println("任务后推成功计数=" + pushSuccessCount);
//		System.out.println("总任务数=" + taskTotal);
		
		
		printInfile(rateResult, resultPath);
		

//		int count = 0;
//		for (int k = 0; k < dagResultMap.length; k++) {
//			for (int l = 0; l < dagResultMap[k].length; l++) {
//				if (dagResultMap[k][l] == 1) {
//					System.out.println("所属dagid=" + k + "；任务的标号=" + l + "调度失败");
//					count++;
//				}
//			}
//		}
	}
	
	
	private static void printInfile(String[][] rateResult2, String resultPath) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultPath, true)));
            out.write(rateResult[0][0] + "\t" + rateResult[0][1]+"\t" + rateResult[0][2] +"\t"+rateResult[0][3] +"\r\n");
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
	 * 
	 * @Title: storeresultShow
	 * @Description: 保存本算法的各个任务的开始结束时间,这里面只有调度成功的
	 * @param
	 * @return void
	 * @throws
	 */
	public static void storeresultShow() {
		int dagcount = 0;
		for (DAG dagmap : DAGMapList) {

			if (dagmap.fillbackdone) {
				ArrayList<Task> DAGTaskList = new ArrayList<Task>();

				for (int i = 0; i < dagmap.gettasklist().size(); i++) {
					Task dag = (Task) dagmap.gettasklist().get(i);
					DAGTaskList.add(dag);
					if(dag.getfillbackdone()){
						message[dagcount][0] = dag.getOriDagId();
						message[dagcount][1] = dag.getOriID();
						message[dagcount][2] = dag.getfillbackpeid();
						message[dagcount][3] = dag.getfillbackstarttime();
						message[dagcount][4] = dag.getfillbackfinishtime();
						dagcount++;
					}
				}
			}
		}
		finishTaskCount=dagcount;

	}

	/**
	 * 
	 * @Title: computeSlot
	 * @Description: 根据relax后结果重新计算空闲时间段SlotListInPes。其中SlotListInPes.put(i,
	 *               slotListinpe)==》slotListinpe的内容是筛选过的，时间上能匹配submit----
	 *               deadline时间段的slot的集合
	 * @param @param submit，DAG提交时间
	 * @param @param deadline，DAG截止时间
	 * @return void
	 * @throws
	 */
	public static void computeSlot(int submit, int deadline) {
		
		SlotListInPes.clear();
		
		for (int i = 0; i < pe_number; i++) {
			// 当前处理器上空闲片段总数计数
			int Slotcount = 0;

			// 获取某处理器上的任务内容
			HashMap<Integer, Integer[]> TASKInPe = new HashMap<Integer, Integer[]>();
			TASKInPe = TASKListInPes.get(i);
			// 数组0代表task开始时间，1代表task结束时间，2代表dagid，3代表id

			ArrayList<Slot> slotListinpe = new ArrayList<Slot>();
			ArrayList<Slot> slotListinpe_ori = new ArrayList<Slot>();

			if (TASKInPe.size() == 0) {// 当前处理器上没有执行过任务
				Slot tem = new Slot();
				tem.setPEId(i);
				tem.setslotId(Slotcount);
				tem.setslotstarttime(submit);
				tem.setslotfinishtime(deadline);
				slotListinpe.add(tem);
				Slotcount++;
			} else if (TASKInPe.size() == 1) {// 该处理器上只有一个任务运行

				if (TASKInPe.get(0)[0] > submit) {
					if (deadline <= TASKInPe.get(0)[0]) {
						Slot tem = new Slot();
						ArrayList<String> below_ = new ArrayList<String>();
						// 数组0代表task开始时间，1代表task结束时间，2代表dagid，3代表id
						// 任务所属dag 任务编号
						below_.add(TASKInPe.get(0)[2] + " "+ TASKInPe.get(0)[3] + " " + 0);
						tem.setPEId(i);
						tem.setslotId(Slotcount);
						tem.setslotstarttime(submit);
						tem.setslotfinishtime(deadline);
						tem.setbelow(below_);
						slotListinpe.add(tem);
						Slotcount++;
					} else if (deadline <= TASKInPe.get(0)[1]) {
						Slot tem = new Slot();
						ArrayList<String> below_ = new ArrayList<String>();
						// 数组0代表task开始时间，1代表task结束时间，2代表dagid，3代表id
						// 任务所属dag 任务编号
						below_.add(TASKInPe.get(0)[2] + " "+ TASKInPe.get(0)[3] + " " + 0);
						tem.setPEId(i);
						tem.setslotId(Slotcount);
						tem.setslotstarttime(submit);
						tem.setslotfinishtime(TASKInPe.get(0)[0]);
						tem.setbelow(below_);
						slotListinpe.add(tem);
						Slotcount++;
					} else {
						Slot tem = new Slot();
						ArrayList<String> below_ = new ArrayList<String>();
						// 数组0代表task开始时间，1代表task结束时间，2代表dagid，3代表id
						// 任务所属dag 任务编号
						below_.add(TASKInPe.get(0)[2] + " "+ TASKInPe.get(0)[3] + " " + 0);
						tem.setPEId(i);
						tem.setslotId(Slotcount);
						tem.setslotstarttime(submit);
						tem.setslotfinishtime(TASKInPe.get(0)[0]);
						tem.setbelow(below_);
						slotListinpe.add(tem);
						Slotcount++;
						// ===================================================
						Slot temp = new Slot();
						// 数组0代表task开始时间，1代表task结束时间，2代表dagid，3代表id
						temp.setPEId(i);
						temp.setslotId(Slotcount);
						temp.setslotstarttime(TASKInPe.get(0)[1]);
						temp.setslotfinishtime(deadline);
						slotListinpe.add(temp);
						Slotcount++;

					}
				} else if (submit <= TASKInPe.get(0)[1]&& deadline > TASKInPe.get(0)[1]) {
					Slot tem = new Slot();
					// 数组0代表task开始时间，1代表task结束时间，2代表dagid，3代表id
					tem.setPEId(i);
					tem.setslotId(Slotcount);
					tem.setslotstarttime(TASKInPe.get(0)[1]);
					tem.setslotfinishtime(deadline);
					slotListinpe.add(tem);
					Slotcount++;
				} else if (submit > TASKInPe.get(0)[1]
						&& deadline > TASKInPe.get(0)[1]) {
					Slot tem = new Slot();
					// 数组0代表task开始时间，1代表task结束时间，2代表dagid，3代表id
					tem.setPEId(i);
					tem.setslotId(Slotcount);
					tem.setslotstarttime(submit);
					tem.setslotfinishtime(deadline);
					slotListinpe.add(tem);
					Slotcount++;
				}
			} else {// 该处理器上有多个任务运行

				// 获取处理器上原本所在各任务间的执行空闲片段
				// 这里是处理上所有的空闲，还没有算上submit和deadline的限制

				// 如果处理器上第一个任务的开始时间不是为0，最开头有一段空隙
				if (TASKInPe.get(0)[0] >= 0) {
					Slot tem = new Slot();
					ArrayList<String> below_ = new ArrayList<String>();
					for (int k = 0; k < TASKInPe.size(); k++) {
						// 数组0代表task开始时间，1代表task结束时间，2代表dagid，3代表id
						// 任务所属的业务id 任务标号
						below_.add(TASKInPe.get(k)[2] + " "+ TASKInPe.get(k)[3] + " " + 0);
					}
					tem.setPEId(i);
					tem.setslotId(Slotcount);
					// System.out.println("这里应该是0才对"+Slotcount);
					tem.setslotstarttime(0);
					tem.setslotfinishtime(TASKInPe.get(0)[0]);
					tem.setbelow(below_);
					slotListinpe_ori.add(tem);
					Slotcount++;
				}

				/**
				 * 在第一个的FIFO调度算法中会出现问题，但是只会在第一个中出现，可以忽略不计较
				 */
				for (int j = 1; j < TASKInPe.size(); j++) {
					if (TASKInPe.get(j - 1)[1] <= TASKInPe.get(j)[0]) {
						Slot tem = new Slot();
						ArrayList<String> below_ = new ArrayList<String>();
						for (int k = j; k < TASKInPe.size(); k++) {
							// 数组0代表task开始时间，1代表task结束时间，2代表dagid，3代表id
							// 任务所属的业务id 任务标号 在哪个空闲块的后面
							below_.add(TASKInPe.get(k)[2] + " "+ TASKInPe.get(k)[3] + " " + j);
						}
						tem.setPEId(i);
						tem.setslotId(Slotcount);
						tem.setslotstarttime(TASKInPe.get(j - 1)[1]);
						tem.setslotfinishtime(TASKInPe.get(j)[0]);
						tem.setbelow(below_);
						slotListinpe_ori.add(tem);
						Slotcount++;
					}

				}

				// 计算在当前dag开始时间到截止时间之间 的slot。
				// 计算能容纳当前DAG的起始slot的编号
				int startslot = 0;
				for (int j = 0; j < slotListinpe_ori.size(); j++) {
					Slot tem = new Slot();
					tem = slotListinpe_ori.get(j);
					/**
					 * 判断处理器上第一个空闲块，就是最开始的头部,有问题？？？？,
					 * 
					 */
					if (j == 0 && (tem.slotstarttime != tem.slotfinishtime)) {
						if (submit >= 0 && submit < tem.slotfinishtime) {
							startslot = 0;
							tem.setslotstarttime(submit);
							break;
						}
					} else if (j > 0 && j <= (slotListinpe_ori.size() - 1)) {
						if (tem.getslotstarttime() <= submit // --slotstarttime--submit--slotfinishtime--
								&& tem.getslotfinishtime() > submit) {
							tem.setslotstarttime(submit);
							startslot = j;
							break;
						} else if (tem.getslotstarttime() > submit // slotfinishtime(上一个slot)--submit---slotstarttime
								&& slotListinpe_ori.get(j - 1).getslotfinishtime() <= submit) {
							startslot = j;
							break;
						}
					}

					// 如果到达时间在前面的slot都没办法匹配插入，则放在处理器最后.
					if (j == (slotListinpe_ori.size() - 1))
						startslot = slotListinpe_ori.size();
				}

				// 设置slotListinpe内容，这里面是筛选过的，时间上能匹配submit----deadline时间段的slot的集合
				/**
				 * 讲道理这里应该没有问题，因为会计算每个处理器上的空闲时间
				 * 但讲道理应该是只要计算一轮就好了，但是为什么会计算好几轮，可能是因为在调度算法中就绪列表里面没有用到这个作业
				 * ，等下一次计算的时候又计算了一轮
				 */
				int count = 0;
				for (int j = startslot; j < slotListinpe_ori.size(); j++) {
					Slot tem = new Slot();
					tem = slotListinpe_ori.get(j);

					if (tem.getslotfinishtime() <= deadline) {
						tem.setslotId(count);
						slotListinpe.add(tem);
						count++;
					} else if (tem.getslotfinishtime() > deadline
							&& tem.getslotstarttime() < deadline) {// ---slotstarttime---deadline---slotfinishtime---
						tem.setslotId(count);
						tem.setslotfinishtime(deadline);
						// System.out.println("============提交时间："+submit+";现有匹配的空闲块："+count+"；空闲开始时间:"+tem.getslotstarttime()+"；空闲结束时间:"+tem.getslotfinishtime()+";在处理器上:"+tem.PEId);
						slotListinpe.add(tem);
						break;
					}
				}

				// 设置最后一个空闲块
				if (TASKInPe.get(TASKInPe.size() - 1)[1] <= submit) {
					Slot tem = new Slot();
					tem.setPEId(i);
					tem.setslotId(count);
					tem.setslotstarttime(submit);
					tem.setslotfinishtime(deadline);
					slotListinpe.add(tem);
					// System.out.println("应该是只有一个空闲块的"+slotListinpe.size());
				} else if (TASKInPe.get(TASKInPe.size() - 1)[1] < deadline
						&& TASKInPe.get(TASKInPe.size() - 1)[1] > submit) {
					Slot tem = new Slot();
					tem.setPEId(i);
					tem.setslotId(count);
					tem.setslotstarttime(TASKInPe.get(TASKInPe.size() - 1)[1]);
					tem.setslotfinishtime(deadline);
					slotListinpe.add(tem);
				}

			}

			SlotListInPes.put(i, slotListinpe);
		}
		
		
	}
	
	/**
	 * 
	 * @Title: changeinpe
	 * @Description: 在调度之后修改slotlistinpe后的below
	 * @param @param slotlistinpe
	 * @param @param inpe
	 * @return void
	 * @throws
	 */
	public static void changeinpe(ArrayList<Slot> slotlistinpe, int inpe) {
		ArrayList<String> below = new ArrayList<String>();

		for (int i = 0; i < slotlistinpe.size(); i++) {
			ArrayList<String> belowte = new ArrayList<String>();
			Slot slottem = slotlistinpe.get(i);
			for (int j = 0; j < slottem.getbelow().size(); j++) {
				below.add(slottem.getbelow().get(j));
			}
			String belowbuf[] = below.get(0).split(" ");
			// 在某个空闲块后面，这个空闲块的编号
			int buffer = Integer.valueOf(belowbuf[2]).intValue();
			if (buffer >= inpe) {
				buffer += 1;
				for (int j = 0; j < below.size(); j++) {
					String belowbuff = belowbuf[0] + " " + belowbuf[1] + " "+ buffer;
					belowte.add(belowbuff);
				}
				slottem.getbelow().clear();
				slottem.setbelow(belowte);
			}
		}
	}

	/**
	 * 
	 * @Title: changetasklistinpe
	 * @Description: 在一次调度且relax后，依据结果修改TASKListInPes的值
	 * @param @param dagmap,DAG包括DAG中各个子任务，以及DAG中任务间依赖关系
	 * @return void
	 * @throws
	 */
	private static void changetasklistinpe(DAG dagmap) {

		for (int i = 0; i < pe_number; i++) {
			HashMap<Integer, Integer[]> TASKInPe = new HashMap<Integer, Integer[]>();
			TASKInPe = TASKListInPes.get(i);

			for (int j = 0; j < TASKInPe.size(); j++) {
				if (TASKInPe.get(j)[2] == dagmap.getDAGId()) {
					Task temp = new Task();
					temp = getDAGById(TASKInPe.get(j)[2], TASKInPe.get(j)[3]);
					TASKInPe.get(j)[0] = temp.getfillbackstarttime();
					TASKInPe.get(j)[1] = temp.getfillbackfinishtime();
				}
			}

			TASKListInPes.put(i, TASKInPe);
		}
	}


	/**
	 * @Description: 计算每层中的各个子任务可以后移的距离，设置relax字段，松弛距离
	 * 
	 * @param dagmap
	 *            ，DAG包括DAG中各个子任务，以及DAG中任务间依赖关系
	 * @param DAGTaskList
	 *            ，DAG中各个子任务
	 * @param canrelaxDAGTaskList
	 *            ，可以后移的子任务列表
	 * @param DAGTaskDependValue
	 *            ，依赖关系
	 * @param levelnumber
	 *            ，层数
	 * @param totalrelax
	 *            ，总冗余值
	 */
	public static void calculateweight(DAG dagmap,
			ArrayList<Task> DAGTaskList, ArrayList<Task> canrelaxDAGTaskList,
			Map<String, Double> DAGTaskDependValue, int levelnumber,
			int totalrelax) {

		int startlevelnumber = canrelaxDAGTaskList.get(0).getnewlevel();
		int[] weight = new int[levelnumber];
		int[] relax = new int[DAGTaskList.size()];
		int[] maxlength = new int[levelnumber + 1];
		int weightsum = 0;

		for (int i = startlevelnumber; i <= levelnumber; i++) {
			int max = 0, maxid = 0;
			for (int j = 0; j < dagmap.taskinlevel.get(i).size(); j++) {
				Task dagtem = new Task();
				dagtem = getDAGById(dagmap.getDAGId(), (int) dagmap.taskinlevel
						.get(i).get(j));

				if (canrelaxDAGTaskList.contains(dagtem)) {
					if (i == levelnumber) {
						max = dagtem.getts();
						maxid = i;
					} else {
						int value = dagtem.getts();
						for (int k = 0; k < dagmap.taskinlevel.get(i + 1)
								.size(); k++) {
							Task dagsuc = new Task();
							dagsuc = getDAGById(dagmap.getDAGId(),
									(int) dagmap.taskinlevel.get(i + 1).get(k));
							if (dagmap.isDepend(String.valueOf(dagtem.getid()),
									String.valueOf(dagsuc.getid()))) {
								if (dagtem.getfillbackpeid() != dagsuc
										.getfillbackpeid()) {
									int tempp = dagtem.getts()
											+ (int) (double) DAGTaskDependValue
													.get(dagtem.getid() + " "
															+ dagsuc.getid());
									if (value < tempp) {
										value = tempp;
										maxid = dagtem.getid();
									}
								}
							}
						}

						if (max < value) {
							max = value;
							maxid = dagtem.getid();
						}
					}
				}
			}
			weight[i - 1] = max;
			maxlength[i - 1] = maxid;
		}

		for (int i = startlevelnumber - 1; i < levelnumber; i++) {
			weightsum = weight[i] + weightsum;
		}

		for (int i = startlevelnumber; i <= levelnumber; i++) {
			for (int j = 0; j < dagmap.taskinlevel.get(i).size(); j++) {
				Task dagtem = new Task();
				dagtem = getDAGById(dagmap.getDAGId(), (int) dagmap.taskinlevel
						.get(i).get(j));
				if (canrelaxDAGTaskList.contains(dagtem)) {
					int tem = weight[i - 1] * totalrelax / weightsum;
					dagtem.setrelax(tem);
				}
			}
		}

		// for (int i = startlevelnumber; i <= levelnumber; i++) {
		// for (int j = 0; j < dagmap.taskinlevel.get(i).size(); j++) {
		// DAG dagtem = new DAG();
		// dagtem = getDAGById(dagmap.getDAGId(), (int)
		// dagmap.taskinlevel.get(i).get(j));
		// if (canrelaxDAGTaskList.contains(dagtem)) {
		//
		// System.out.println("dag编号是："+dagtem.getdagid()+":"+dagtem.getid()+"\t层级是："+dagtem.getnewlevel()+":"+dagtem.getrelax());
		// }
		// }
		// }

		// for (int j = 0; j < canrelaxDAGTaskList.size(); j++) {
		// DAG dagtem = new DAG();
		// dagtem = getDAGById(dagmap.getDAGId(),
		// canrelaxDAGTaskList.get(j).getid());
		// if (canrelaxDAGTaskList.contains(dagtem)) {
		// System.out.println("dag编号是："+dagtem.getdagid()+":"+dagtem.getid()+"\t层级是："+dagtem.getnewlevel()+":"+dagtem.getrelax());
		// }
		//
		// }

	}

	/**
	 * @Description: 将相同层数的子任务放在一个列表里。里面存放的是原列表中的标号值
	 * 
	 * @param dagmap
	 *            ，DAG包括DAG中各个子任务，以及DAG中任务间依赖关系
	 * @param DAGTaskList
	 *            ，DAG中各个子任务
	 * @param deadline
	 *            ，DAG的截止时间
	 * @return levelnumber,现有层数
	 */
	public static int putsameleveltogether(DAG dagmap,
			ArrayList<Task> DAGTaskList, int deadline) {
		int levelnumber = DAGTaskList.get(DAGTaskList.size() - 1).getnewlevel();
		int finishtime = DAGTaskList.get(DAGTaskList.size() - 1)
				.getfillbackfinishtime();
		int totalrelax = deadline - finishtime;

		// 层级是从1开始的
		for (int j = 1; j <= levelnumber; j++) {
			ArrayList<Integer> samelevel = new ArrayList<Integer>();

			for (int i = 0; i < dagmap.gettasklist().size(); i++) {
				if (DAGTaskList.get(i).getnewlevel() == j)
					samelevel.add(i);
			}
			dagmap.taskinlevel.put(j, samelevel);
		}
		return levelnumber;
	}

	/**
	 * @throws IOException
	 * 
	 * @Title: calculatenewlevel
	 * @Description: 根据调度结果，重新计算DAG中各个子任务的层数。设置newlevel、orderbystarttime参数
	 * @param @param dagmap，DAG包括DAG中各个子任务，以及DAG中任务间依赖关系
	 * @param @param DAGTaskList，DAG中各个子任务
	 * @param @param DAGTaskListtemp
	 * @param @param setorderbystarttime 传进去的时候还是一个空的
	 * @return void
	 * @throws
	 */
	public static void calculateNewLevel(DAG dagmap,
			ArrayList<Task> DAGTaskList, ArrayList<Task> DAGTaskListtemp,
			ArrayList<Task> setorderbystarttime) throws IOException {

		Task min = new Task();
		Task temp = new Task();

		// 将DAGTaskListtemp按照fillbackstarttime从小到大排列
		for (int k = 0; k < DAGTaskListtemp.size(); k++) {
			int tag = k;
			min = DAGTaskListtemp.get(k);
			temp = DAGTaskListtemp.get(k);
			for (int p = k + 1; p < DAGTaskListtemp.size(); p++) {
				if (DAGTaskListtemp.get(p).getfillbackstarttime() < min
						.getfillbackstarttime()) {
					min = DAGTaskListtemp.get(p);
					tag = p;
				}
			}
			if (tag != k) {
				DAGTaskListtemp.set(k, min);
				DAGTaskListtemp.set(tag, temp);
			}
		}

		// 设置本作业中按照调度后的结果开始时间升序排序的结果列表
		dagmap.setorderbystarttime(DAGTaskListtemp);

		for (int i = 0; i < dagmap.gettasklist().size(); i++) {
			setorderbystarttime.add((Task) dagmap.getorderbystarttime().get(i));
		}

		// 依据starttime为遍历顺序
		for (int i = 0; i < setorderbystarttime.size(); i++) {
			Task dag = new Task();
			dag = setorderbystarttime.get(i);

			if (i == 0) {
				dag.setnewlevel(1);
			} else {
				int max = 0;
				// 找寻当前任务在相同处理器上的相邻的上一个任务，找到了就跳出循环
				for (int j = i - 1; j >= 0; j--) {
					if (setorderbystarttime.get(j).getfillbackpeid() == dag
							.getfillbackpeid()) {
						max = setorderbystarttime.get(j).getnewlevel() + 1;
						break;
					}
				}

				/**
				 * 
				 */
				// 这个pre里面放的是原本的id。原本的id是setorderbystarttime.get(i).getid()
				ArrayList<Integer> pre = dag.getpre();
				int id = dag.getdagid();
				// 当前这个dag的开始时间
				int childStart = dag.getfillbackstarttime();
				// 这里面是当前的位置
				int[] current = new int[pre.size()];
				int co = 0;
				for (int p : pre) {
					for (int c = 0; c < setorderbystarttime.size(); c++) {
						if (p == setorderbystarttime.get(c).getid()) {
							current[co] = c;
							co++;
						}
					}
				}

				for (int cc : current) {
					Task purePre = setorderbystarttime.get(cc);

//					if (purePre.getnewlevel() == -1)
//						System.out.println("为什么又是-1！！！！！！！！！！！！！！~~~~~~~~~~");

					int leveltemp = purePre.getnewlevel() + 1;
					if (leveltemp > max)
						max = leveltemp;

				}
				Task NOW = getDAGById(dag.getdagid(), dag.getid());
				NOW.setnewlevel(max);
			}
		}
	}

	/**
	 * 
	 * @Title: calculateoriginallevel
	 * @Description: 计算DAG中原始各个子任务的层数，设置：level
	 * @param @param DAGTaskList，DAG中各个子任务
	 * @return void
	 * @throws
	 */
	public static void calculateOriginalLevel(ArrayList<Task> DAGTaskList) {
		for (int i = 0; i < DAGTaskList.size(); i++) {
			// 如果当前任务是起始任务，设置层级为1
			if (i == 0)
				DAGTaskList.get(i).setlevel(1);
			else {// 如果当前任务不是起始任务，设置层级为其父任务层级最大值+1
				int max = 0;
				Iterator<Integer> it = DAGTaskList.get(i).getpre().iterator();
				while (it.hasNext()) {
					int pretempid = it.next();
					int leveltemp = DAGTaskList.get(pretempid).getlevel() + 1;
					if (leveltemp > max)
						max = leveltemp;
				}
				DAGTaskList.get(i).setlevel(max);
			}
		}
	}

	/**
	 * @throws IOException
	 * 
	 * @Title: wholerelax
	 * @Description: 根据调度结果进行levelrelaxing操作，
	 * @param @param dagmap，DAG包括DAG中各个子任务，以及DAG中任务间依赖关系
	 * @return void
	 * @throws
	 */
	public static void wholerelax(DAG dagmap) throws IOException {
		int Criticalnum = 0;
		if (!dagmap.isSingle) {
			Criticalnum = CriticalPath(dagmap);
		} else {
			Criticalnum = 1;
		}
		int submit = dagmap.getsubmittime();
		int deadline = dagmap.getDAGdeadline();

		ArrayList<Task> canrelaxDAGTaskList = new ArrayList<Task>();
		ArrayList<Task> norelaxDAGTaskList = new ArrayList<Task>();
		ArrayList<Task> DAGTaskList = new ArrayList<Task>();
		ArrayList<Task> DAGTaskListtemp = new ArrayList<Task>();
		ArrayList<Task> setorderbystarttime = new ArrayList<Task>();
		Map<String, Double> DAGTaskDependValue = new HashMap<String, Double>();
		DAGTaskDependValue = dagmap.getdependvalue();

		for (int i = 0; i < dagmap.gettasklist().size(); i++) {
			DAGTaskList.add((Task) dagmap.gettasklist().get(i));
			DAGTaskListtemp.add((Task) dagmap.gettasklist().get(i));
		}

		// 计算本DAG的原本level
		calculateOriginalLevel(DAGTaskList);

		/**
		 * 10.22：这个无敌bug终于是解决了，生气哦
		 * 
		 * 10.23：结果发现学姐其实已经解决过这个bug，更生气
		 */
		calculateNewLevel(dagmap, DAGTaskList, DAGTaskListtemp,setorderbystarttime);

		// 相同层级的任务放在同一个列表中
		int levelnumber = putsameleveltogether(dagmap, DAGTaskList, deadline);

		int finishtime = DAGTaskList.get(DAGTaskList.size() - 1).getfillbackfinishtime();

		int totalrelax = deadline - finishtime;

		// 只要有一个任务没能回填成功，就是失败
		boolean finishsearch = true;

		for (int i = levelnumber; i >= 1; i--) {
			for (int j = 0; j < dagmap.taskinlevel.get(i).size(); j++) {
				Task dagtem = new Task();
				dagtem = getDAGById(dagmap.getDAGId(), (int) dagmap.taskinlevel.get(i).get(j));
				//System.out.println("+++++++++此时需要提取的DAG是："+dagmap.getDAGId()+":"+(int) dagmap.taskinlevel.get(i).get(j)+"\t原始DAG信息是：");
				/**
				 * 插在中间的任务不参与松弛
				 */
				if (dagtem.getisfillback() == false) {
					canrelaxDAGTaskList.add(dagtem);
					// finishsearch=true;
				} else {
					norelaxDAGTaskList.add(dagtem);
				}
			}
		}
		/**
		 * 不参与松弛的任务，相关值都设置为自己。 如果要更新算法就要修改这里
		 */
		for (int j = 0; j < norelaxDAGTaskList.size(); j++) {
			Task dagtem = new Task();
			dagtem = getDAGById(dagmap.getDAGId(), (int) norelaxDAGTaskList.get(j).getid());
			dagtem.setslidefinishdeadline(dagtem.getfillbackfinishtime());
			dagtem.setslidedeadline(dagtem.getfillbackstarttime());
			dagtem.setslidelength(0);
		}

		// 开始松弛操作
		if (canrelaxDAGTaskList.size() > 0) {
			Task mindag = new Task();
			Task tempdag = new Task();

			/**
			 * 按照开始时间排序
			 */
			for (int k = 0; k < canrelaxDAGTaskList.size(); k++) {
				int tag = k;
				mindag = canrelaxDAGTaskList.get(k);
				tempdag = canrelaxDAGTaskList.get(k);
				for (int p = k + 1; p < canrelaxDAGTaskList.size(); p++) {
					if (canrelaxDAGTaskList.get(p).getfillbackstarttime() <= mindag.getfillbackstarttime()) {
						mindag = canrelaxDAGTaskList.get(p);
						tag = p;
					}
				}
				if (tag != k) {
					canrelaxDAGTaskList.set(k, mindag);
					canrelaxDAGTaskList.set(tag, tempdag);
				}
			}

			/**
			 * 
			 */

			// 参与的不一定是从第一层开始的
			int startlevelnumber = canrelaxDAGTaskList.get(0).getnewlevel();

			/**
			 * 计算每个任务的冗余值并设置对应对象字段
			 */
			// calculateweight(dagmap, DAGTaskList,canrelaxDAGTaskList,DAGTaskDependValue, levelnumber, totalrelax);

			/**
			 * 整体角度计算冗余值
			 */
			calculateweight(dagmap, DAGTaskList, DAGTaskList,DAGTaskDependValue, levelnumber, totalrelax);

			// ============================ 找到本作业中的任务总执行时间最长的处理器===========
			int startinpe[] = new int[pe_number];
			int finishinpe[] = new int[pe_number];
			int length = -1;
			int maxpeid = -1;
			for (int k = 0; k < pe_number; k++)
				startinpe[k] = timewindowmax;
			for (int k = 0; k < DAGTaskList.size(); k++) {
				Task dagtem = new Task();
				dagtem = DAGTaskList.get(k);
				if (startinpe[dagtem.getfillbackpeid()] > dagtem.getfillbackstarttime())
					startinpe[dagtem.getfillbackpeid()] = dagtem.getfillbackstarttime();
				if (finishinpe[dagtem.getfillbackpeid()] < dagtem.getfillbackfinishtime())
					finishinpe[dagtem.getfillbackpeid()] = dagtem.getfillbackfinishtime();
			}
			for (int k = 0; k < pe_number; k++) {
				if (length < (finishinpe[k] - startinpe[k])) {
					length = finishinpe[k] - startinpe[k];
					maxpeid = k;
				}
			}

			// 设置该处理器上的任务为关键任务
			for (int k = 0; k < DAGTaskList.size(); k++) {
				Task dagtem = new Task();
				dagtem = getDAGById(dagmap.getDAGId(), DAGTaskList.get(k).getid());
				if (dagtem.getfillbackpeid() == maxpeid) {
					dagtem.setiscriticalnode(true);
				}
			}

			// 设置需要更新的列表中的起始层的任务时间
			for (int j = 0; j < dagmap.taskinlevel.get(startlevelnumber).size(); j++) {
				Task dagtem = new Task();
				// taskinlevel中放的是原列表中的编号。dagmap.gettasklist()
				dagtem = getDAGById(dagmap.getDAGId(), (int) dagmap.taskinlevel.get(startlevelnumber).get(j));

				if (canrelaxDAGTaskList.contains(dagtem)) {
					// 设置fd
					dagtem.setslidefinishdeadline(dagtem.getfillbackfinishtime() + dagtem.getrelax());
					// 设置sd
					dagtem.setslidedeadline(dagtem.getrelax()+ dagtem.getfillbackstarttime());
					// 设置可滑动的长度
					dagtem.setslidelength(dagtem.getrelax());
				}
			}

			// 从下一层开始更新
			for (int i = startlevelnumber + 1; i <= levelnumber; i++) {
				Task dagtemLast = new Task();
				dagtemLast = getDAGById(dagmap.getDAGId(),
						(int) dagmap.taskinlevel.get(i - 1).get(0));
				int starttime = dagtemLast.getslidefinishdeadline();
				int finishdeadline = -1;

				for (int j = 0; j < dagmap.taskinlevel.get(i).size(); j++) {
					Task dagtem = new Task();
					dagtem = getDAGById(dagmap.getDAGId(),
							(int) dagmap.taskinlevel.get(i).get(j));
					// 设置s

					dagtem.setfillbackstarttime(starttime);
					dagtem.setfillbackfinishtime(dagtem.getfillbackstarttime()+ dagtem.getts());

				}

				// 如果本层没有在关键路径上的任务，则选取所有任务中最大的fd为本层的fd
				/**
				 * 有可能本层的所有任务都不在执行时间最长的那个处理器上 所以本层没有在关键位置的任务
				 */
				if (finishdeadline == -1) {
					for (int j = 0; j < dagmap.taskinlevel.get(i).size(); j++) {
						Task dagtem = new Task();
						dagtem = getDAGById(dagmap.getDAGId(),(int) dagmap.taskinlevel.get(i).get(j));
						if (finishdeadline < dagtem.getfillbackfinishtime())
							finishdeadline = dagtem.getfillbackfinishtime();
					}
				}

				/**
				 * 有不符合时间的任务，但是他们都不在需要松弛的列表中
				 */

				// 更新本层所有任务的fd为在关键路径上的任务的fd
				for (int j = 0; j < dagmap.taskinlevel.get(i).size(); j++) {
					Task dagtem = new Task();
					dagtem = getDAGById(dagmap.getDAGId(),
							(int) dagmap.taskinlevel.get(i).get(j));
					if (canrelaxDAGTaskList.contains(dagtem)) {
						dagtem.setslidefinishdeadline(finishdeadline);
						dagtem.setslidedeadline(finishdeadline - dagtem.getts());
						dagtem.setslidelength(dagtem.getslidedeadline()
								- dagtem.getfillbackstarttime());
						// System.out.println("dag="+dagtem.getdagid()+":"+dagtem.getid()+";松弛开始时间："+dagtem.getslidedeadline());
					}
				}
			}

		}// if (canrelaxDAGTaskList.size() > 0)

		// printRelax(dagmap);
	}

	private static void printRelax(DAG dagmap) throws IOException {

		FileWriter writer = new FileWriter("G:\\relax.txt", true);
		DAG tempTestJob = dagmap;
		int dagid = tempTestJob.DAGId;
		int num = tempTestJob.tasknumber;

		for (int o = 0; o < num; o++) {
			Task tempDag = getDAGById(dagid, o);
			writer.write("层级是：" + tempDag.getnewlevel() + "\t作业："
					+ tempDag.getdagid() + "\t任务：" + tempDag.getid()
					+ "\t调度开始：" + tempDag.getfillbackstarttime() + "\t松弛开始："
					+ tempDag.getslidedeadline() + "\t调度结束："
					+ tempDag.getfillbackfinishtime() + "\t松弛结束："
					+ tempDag.getslidefinishdeadline() + "\t可后推长度："
					+ tempDag.getslidelength() + "\n");

		}
		if (writer != null) {
			writer.close();
		}

	}

	/**
	 * 
	 * @Title: printDagMap
	 * @Description: 打印作业的调度结果至文件中
	 * @param dagmap
	 * @throws IOException
	 *             :
	 * @throws
	 */
	private static void printDagMap(DAG dagmap) throws IOException {
		FileWriter writer = new FileWriter("G:\\dagmap.txt", true);
		DAG tempTestJob = dagmap;
		int dagid = tempTestJob.DAGId;
		int num = tempTestJob.tasknumber;

		for (int o = 0; o < num; o++) {
			Task tempDag = getDAGById(dagid, o);
			ArrayList<Integer> pre = tempDag.getpre();

			for (int p : pre) {
				Task tempPre = getDAGById(dagid, p);
				if (tempPre.getfillbackstarttime() > tempDag
						.getfillbackstarttime())
					writer.write("DAG的id=" + dagid + "；父任务id=" + p + ";开始时间："
							+ tempPre.getfillbackstarttime() + ";子任务id=" + o
							+ ";子任务开始时间：" + tempDag.getfillbackstarttime()
							+ "\n");
			}
		}
		if (writer != null) {
			writer.close();
		}

	}

	/**
	 * @Description: 判断后移空闲块后的负载能否使子任务成功放入空闲块
	 * 
	 * @param dagmap
	 *            ，DAG包括DAG中各个子任务，以及DAG中任务间依赖关系
	 * @param readylist
	 *            ，readylist就绪队列
	 * 
	 * @return isslide，能否放入
	 */
	public static boolean scheduling(DAG dagmap, ArrayList<Task> readylist) {
		boolean findsuc = true;// 本DAG能否回填成功，只要一个任务失败就是全部失败
		
		while (readylist.size() > 0) {
			int finimintime = timewindowmax;
			int mindag = -1;
			int message[][] = new int[readylist.size()][6];
			// 0 is if success 1 means success 0 means fail,
			// 1 is earliest starttime
			// 2 is peid
			// 3 is slotid
			// 4 is if need slide
			// 5 is slide length

			int[] finish = new int[readylist.size()];// 该任务的执行结束时间

			// 为DAG中除第一个任务外的任务找寻可插入的slot并返回信息
			for (int i = 0; i < readylist.size(); i++) {
				Task dag = new Task();
				dag = readylist.get(i);
				// 不管怎样，先全部都找一遍，里面只是找了一遍，没有设置相应的参数
				message[i] = findslot(dagmap, dag);
				finish[i] = message[i][1] + dag.getts();
			}
			
			
			int dagId = dagmap.getDAGId();
			// 只要其中有一个任务没能回填成功，那么整个DAG失败
			for (int i = 0; i < readylist.size(); i++) {
				Task tempDagResult = readylist.get(i);
				if (message[i][0] == 0) {
					//System.out.println("找不到位置插入这个任务"+tempDagResult.getid());
					dagResultMap[dagId][tempDagResult.getid()] = 1;
					findsuc = false;
				}
			}

			if (findsuc == false) {
				return findsuc;
			}

			// 就绪队列中所有任务都找到了合适的位置插入
			// 找到所有任务中执行结束时间最早的，mindag为其任务
			for (int i = 0; i < readylist.size(); i++) {
				if (finimintime > finish[i]) {
					finimintime = finish[i];
					mindag = i;
				}
			}
			
			// 找到这个执行结束时间最早的任务
			ArrayList<Task> DAGTaskList = new ArrayList<Task>();
			Task dagtemp = new Task();
			for (int i = 0; i < dagmap.gettasklist().size(); i++) {
				Task dag = new Task();
				DAGTaskList.add((Task) dagmap.gettasklist().get(i));
				dag = (Task) dagmap.gettasklist().get(i);
				int tempTaskId=readylist.get(mindag).getid();
				if (dag.getid() == tempTaskId) {
					dagtemp = (Task) dagmap.gettasklist().get(i);
				}
			}

			// 设置这个任务fillbackstarttime等信息
			// int startmin = finimin - readylist.get(mindag).getts();
			int startmin = message[mindag][1];
			int pemin = message[mindag][2];
			int slotid = message[mindag][3];
			dagtemp.setfillbackstarttime(startmin);
			dagtemp.setfillbackpeid(pemin);
			dagtemp.setpeid(pemin);
			dagtemp.setfillbackready(true);
			dagtemp.setprefillbackdone(true);
			dagtemp.setprefillbackdone(true);

			HashMap<Integer, Integer[]> TASKInPe = new HashMap<Integer, Integer[]>();
			TASKInPe = TASKListInPes.get(pemin);
			// ==================修改处理器上的调度结果，插入这个任务
			// 在要操作的空闲位置中插入任务，并后移原本就在处理器上的任务
			if (TASKInPe.size() > 0) {// 该处理器上原本有任务
				ArrayList<Slot> slotlistinpe = new ArrayList<Slot>();
				for (int j = 0; j < SlotListInPes.get(pemin).size(); j++)
					slotlistinpe.add((Slot) SlotListInPes.get(pemin).get(j));

				ArrayList<String> below = new ArrayList<String>();

				Slot slottem = new Slot();
				for (int i = 0; i < slotlistinpe.size(); i++) {
					if (slotlistinpe.get(i).getslotId() == slotid) {
						slottem = slotlistinpe.get(i);
						break;
					}
				}

				for (int i = 0; i < slottem.getbelow().size(); i++) {
					below.add(slottem.getbelow().get(i));
				}

				if (below.size() > 0) {// 如果这个slot后面原本有任务
					String buf[] = below.get(0).split(" ");
					//
					int inpe = Integer.valueOf(buf[2]).intValue();
					// 后移后续的任务
					for (int i = TASKInPe.size(); i > inpe; i--) {
						Integer[] st_fitemp = new Integer[4];
						st_fitemp[0] = TASKInPe.get(i - 1)[0];
						st_fitemp[1] = TASKInPe.get(i - 1)[1];
						st_fitemp[2] = TASKInPe.get(i - 1)[2];
						st_fitemp[3] = TASKInPe.get(i - 1)[3];
						TASKInPe.put(i, st_fitemp);
					}

					Integer[] st_fi = new Integer[4];
					st_fi[0] = startmin;
					st_fi[1] = startmin+dagtemp.getlength();
					st_fi[2] = dagtemp.getdagid();
					st_fi[3] = dagtemp.getid();
					TASKInPe.put(inpe, st_fi);

					/**
					 * 设置isfillback 使这个任务跳过松弛这一步骤
					 * 证明这个任务是插在其它作业之间的
					 */
					dagtemp.setisfillback(true);

					// 改变空闲块的的below
					changeinpe(slotlistinpe, inpe);

				} else {// 如果这个slot后面原本没有任务
					Integer[] st_fi = new Integer[4];
					st_fi[0] = startmin;
					st_fi[1] = startmin+dagtemp.getlength();;
					st_fi[2] = dagtemp.getdagid();
					st_fi[3] = dagtemp.getid();
					TASKInPe.put(TASKInPe.size(), st_fi);
				}
			} else {// 该处理器上原本没有任务
				Integer[] st_fi = new Integer[4];
				st_fi[0] = startmin;
				st_fi[1] = startmin+dagtemp.getlength();;
				st_fi[2] = dagtemp.getdagid();
				st_fi[3] = dagtemp.getid();
				TASKInPe.put(TASKInPe.size(), st_fi);
			}

			TASKListInPes.put(pemin, TASKInPe);
			// 重新计算空闲块列表
			
			computeSlot(dagmap.getsubmittime(), dagmap.getDAGdeadline());
			
			
			
			// mindag是个就绪队列中的标记
			readylist.remove(mindag);

		}

		return findsuc;
	}

	/**
	 * @Description: 判断DAG中其余节点能否找到空闲时间段放入，如果能则返回相应的信息
	 * 
	 * @param dagmap
	 *            ，DAG包括DAG中各个子任务，以及DAG中任务间依赖关系
	 * @param dagtemp
	 *            ，DAG中其余TASK中的一个
	 * @return message，0 is if success(1 means success 0 means fail), 1 is
	 *         earliest start time, 2 is peid, 3 is slotid
	 */
	public static int[] findslot(DAG dagmap, Task dagtemp) {
		int message[] = new int[6];

		boolean findsuc = false;
		int startmin = timewindowmax;
		int finishmin = timewindowmax;
		int diffmin=timewindowmax;
		int pemin = -1;
		int slide;
		int[] startinpe = new int[pe_number]; // 在处理器i上开始执行的时间
		int[] slotid = new int[pe_number]; // 空闲块在处理器i上的编号
		int[] isneedslide = new int[pe_number]; // 0 means don't need 1 means
												// need slide
		int[] slidelength = new int[pe_number];// 在处理器i上需要滑动的长度
		int[] diff = new int[pe_number];
		

		/**
		 * 归0
		 */
		for (int k = 0; k < pe_number; k++) {
			pushFlag[k] = 0;
		}

		Map<String, Double> DAGTaskDependValue = new HashMap<String, Double>();
		DAGTaskDependValue = dagmap.getdependvalue();

		// 1、获取父任务集合
		ArrayList<Task> pre_queue = new ArrayList<Task>();
		ArrayList<Integer> pre = new ArrayList<Integer>();

		pre = dagtemp.getpre();
		if (pre.size() > 0) {
			for (int j = 0; j < pre.size(); j++) {
				Task buf = new Task();
				buf = getDAGById(dagtemp.getdagid(), pre.get(j));
				if (!buf.fillbackdone && !buf.fillbackready) {
					message[0] = 0;
					System.out.println("他的父节点没能完成");
					return message;
				}
				pre_queue.add(buf);
			}
		}

		int faDone=0;
		// 2、计算在各个处理器上可调度位置
		for (int i = 0; i < pe_number; i++) {
			//2.1：计算在当前处理器上的最早开始时间，也就是父任务的截止时间
			int predone = 0;// 在当前处理器上当前任务最早开始执行时间
			if (pre_queue.size() == 1) {// 如果该任务只有一个父任务
				if (pre_queue.get(0).getfillbackpeid() == i) {// 与父任务在同一个处理器上
					predone = pre_queue.get(0).getfillbackfinishtime();
				} else {// 与父任务不在同一个处理器上
					int value = (int) (double) DAGTaskDependValue.get(String.valueOf(pre_queue.get(0).getid())+ " "+ String.valueOf(dagtemp.getid()));
					predone = pre_queue.get(0).getfillbackfinishtime() + value;
				}
			} else if (pre_queue.size() >= 1) {// 有多个父任务
				for (int j = 0; j < pre_queue.size(); j++) {
					if (pre_queue.get(j).getfillbackpeid() == i) {// 与父任务在同一个处理器上
						if (predone < pre_queue.get(j).getfillbackfinishtime()) {
							predone = pre_queue.get(j).getfillbackfinishtime();
						}
					} else {// 与父任务不在同一个处理器上
						int valu = (int) (double) DAGTaskDependValue.get(String.valueOf(pre_queue.get(j).getid())+ " "+ String.valueOf(dagtemp.getid()));
						int value = pre_queue.get(j).getfillbackfinishtime()+ valu;
						if (predone < value)
							predone = value;
					}
				}
			}

			faDone=predone;
		
			startinpe[i] = -1;
			diff[i] = -1;

			ArrayList<Slot> slotlistinpe = new ArrayList<Slot>();
			 //i:处理器编号
			for (int j = 0; j < SlotListInPes.get(i).size(); j++)
				slotlistinpe.add((Slot) SlotListInPes.get(i).get(j));

			HashMap<Integer, Integer[]> tempSlotInfo=new LinkedHashMap<>();
			int countSlot=0;
			//2.2： 找寻本任务在当前处理器上插入的最早开始的空余块的相关信息。
			//找到一个就立马跳出循环
			for (int j = 0; j < SlotListInPes.get(i).size(); j++) {
				int slst = slotlistinpe.get(j).getslotstarttime();
				int slfi = slotlistinpe.get(j).getslotfinishtime();
				
				if (predone < slst) {
					if ((slst + dagtemp.getts()) <= slfi&& (slst + dagtemp.getts()) <= dagtemp.getdeadline()) {
						startinpe[i] = slst;
						diff[i]=0;
						slotid[i] = slotlistinpe.get(j).getslotId();
						isneedslide[i] = 0;
						break;
					}
				} else if (predone >= slst && predone < slfi) {
					if ((predone + dagtemp.getts()) <= slfi&& (predone + dagtemp.getts()) <= dagtemp.getdeadline()) {
						startinpe[i] = predone;
						diff[i]=predone-slst;
						slotid[i] = slotlistinpe.get(j).getslotId();
						isneedslide[i] = 0;
						break;
					}
				}
			}
		}
		//3、从所有处理器结果中选择最小空隙
		for (int i = 0; i < pe_number; i++) {
			if (startinpe[i] != -1) {
				findsuc = true;
				if (startinpe[i] < startmin) {
					startmin = startinpe[i];
					pemin = i;
				}
			}
		}
		
		
	//	System.out.println("pemin="+pemin);
		// 0 is if success 1 means success 0 means fail,
		// 1 is earliest starttime
		// 2 is peid
		// 3 is slotid
		// 4 is if need slide
		// 5 is slide length
		if (findsuc) {
			message[0] = 1;
			message[1] = startinpe[pemin];
			message[2] = pemin;
			message[3] = slotid[pemin];
			message[4] = isneedslide[pemin];
			if (isneedslide[pemin] == 1)
				message[5] = slidelength[pemin];
			else
				message[5] = -1;
		} else {
			message[0] = 0;
		}

		return message;
	}

	/**
	 * @Description: 判断DAG中起始节点能否找到空闲时间段放入
	 * 
	 * @param dagmap
	 *            ，DAG包括DAG中各个子任务，以及DAG中任务间依赖关系
	 * @param dagtemp
	 *            ，起始节点
	 * @return findsuc，能否放入
	 */
	public static boolean findfirsttaskslot(DAG dagmap, Task dagtemp) {
		// perfinish is the earliest finish time minus task'ts time, the
		// earliest start time

		boolean findsuc = false;
		int startmin = timewindowmax;
		int finishmin = 0;
		int pemin = -1;
		int slide;
		int[] startinpe = new int[pe_number];// 在处理器i上开始执行的最早时间
		int[] slotid = new int[pe_number];// 如果在处理器i上执行，本任务插入的slot的id
		// int[] slidinpe = new int[pe_number];//如果在处理器i上执行，本任务插入的slot的id

		// 遍历所有处理器
		for (int i = 0; i < pe_number; i++) {
			// 起始赋予初始值-1。其中保存的是可以最早开始的时间
			startinpe[i] = -1;
			ArrayList<Slot> slotlistinpe = new ArrayList<Slot>();
			for (int j = 0; j < SlotListInPes.get(i).size(); j++)
				slotlistinpe.add((Slot) SlotListInPes.get(i).get(j));

			// 遍历本处理器上所有满足时间要求的空闲段
			for (int j = 0; j < SlotListInPes.get(i).size(); j++) {
				int slst = slotlistinpe.get(j).getslotstarttime();
				int slfi = slotlistinpe.get(j).getslotfinishtime();

				// 每个任务的到达时间初始是整个业务的提交时间。
				// 这里第一个任务的到达时间就是作业提交的时间。dagtemp.getarrive()
				if (dagtemp.getarrive() <= slst) {// predone<=slst
					if ((slst + dagtemp.getts()) <= slfi && // s1+c<f1
							(slst + dagtemp.getts()) <= dagtemp.getdeadline()) {
						startinpe[i] = slst;
						slotid[i] = slotlistinpe.get(j).getslotId();
						break;
					} else if ((slst + dagtemp.getts()) > slfi
							&& (slst + dagtemp.getts()) <= dagtemp
									.getdeadline()) {
						continue;

					}
				} else {// predone>slst
					if ((dagtemp.getarrive() + dagtemp.getts()) <= slfi // predone+c<f1
							&& (dagtemp.getarrive() + dagtemp.getts()) <= dagtemp
									.getdeadline()) {
						startinpe[i] = dagtemp.getarrive();
						slotid[i] = slotlistinpe.get(j).getslotId();
						break;
					} else if ((dagtemp.getarrive() + dagtemp.getts()) > slfi
							&& (dagtemp.getarrive() + dagtemp.getts()) <= dagtemp
									.getdeadline()) {
						continue;
					}
				}
			}

		}

		// 找到开始时间最早的处理器
		for (int i = 0; i < pe_number; i++) {
			if (startinpe[i] != -1) {
				// 存在可以插入这第一个任务的空闲块
				findsuc = true;
				// 选在开始时间最早的空闲块
				if (startinpe[i] < startmin) {
					startmin = startinpe[i];
					pemin = i;
				}
			}
		}

		/**
		 * 
		 */
		if (findsuc == false) {
			System.out.println("任务的第一个节点没有找到合适处理器的地方插入");
		}

		/**
		 * 
		 */
		// 如果至少有一个处理器上能够插入这个任务
		if (findsuc) {
			// startmin:可最早处理的时间
			// finishmin:可最早执行完毕的时间
			finishmin = startmin + dagtemp.getts();
			dagtemp.setfillbackstarttime(startmin);
			dagtemp.setfillbackpeid(pemin);
			dagtemp.setfillbackready(true);

			HashMap<Integer, Integer[]> TASKInPe = new HashMap<Integer, Integer[]>();
			TASKInPe = TASKListInPes.get(pemin);

			if (TASKInPe.size() > 0) {// 原本的处理器上有多个任务存在

				ArrayList<Slot> slotlistinpe = new ArrayList<Slot>();
				for (int j = 0; j < SlotListInPes.get(pemin).size(); j++)
					slotlistinpe.add((Slot) SlotListInPes.get(pemin).get(j));

				ArrayList<String> below = new ArrayList<String>();

				Slot slottem = new Slot();
				// 找到slotlistinpe中要插入的那个slot对象
				for (int i = 0; i < slotlistinpe.size(); i++) {
					if (slotlistinpe.get(i).getslotId() == slotid[pemin]) {
						slottem = slotlistinpe.get(i);
						break;
					}
				}

				// 得到below
				for (int i = 0; i < slottem.getbelow().size(); i++) {
					below.add(slottem.getbelow().get(i));
				}

				if (below.size() > 0) {// 要插入的位置后面有多个任务
					// 将本任务插入到对应的位置
					String buf[] = below.get(0).split(" ");
					int inpe = Integer.valueOf(buf[2]).intValue();

					// 将空闲块后的任务在处理器上的执行次序都后移一个位置
					for (int i = TASKInPe.size(); i > inpe; i--) {
						Integer[] st_fitemp = new Integer[4];
						st_fitemp[0] = TASKInPe.get(i - 1)[0];
						st_fitemp[1] = TASKInPe.get(i - 1)[1];
						st_fitemp[2] = TASKInPe.get(i - 1)[2];
						st_fitemp[3] = TASKInPe.get(i - 1)[3];
						TASKInPe.put(i, st_fitemp);
					}
					Integer[] st_fi = new Integer[4];
					st_fi[0] = startmin;
					st_fi[1] = finishmin;
					st_fi[2] = dagtemp.getdagid();
					st_fi[3] = dagtemp.getid();
					TASKInPe.put(inpe, st_fi);
					dagtemp.setisfillback(true);
				} else {// 要插入的位置后面没有任务，也就不需要移动
					Integer[] st_fi = new Integer[4];
					st_fi[0] = startmin;
					st_fi[1] = finishmin;
					st_fi[2] = dagtemp.getdagid();
					st_fi[3] = dagtemp.getid();
					TASKInPe.put(TASKInPe.size(), st_fi);
				}

			} else {// 如果该处理器上原本没有任务
				Integer[] st_fi = new Integer[4];
				st_fi[0] = startmin;
				st_fi[1] = finishmin;
				st_fi[2] = dagtemp.getdagid();
				st_fi[3] = dagtemp.getid();
				TASKInPe.put(TASKInPe.size(), st_fi);
			}

			// 计算新的空闲块列表
			computeSlot(dagmap.getsubmittime(), dagmap.getDAGdeadline());
		} else {
			return false;
		}

		return findsuc;// 返回是否找到位置插入任务

	}

	/**
	 * @Description: 判断backfilling操作能否成功, 从第二个提交任务开始操作
	 * 
	 * @param dagmap
	 *            ，DAG包括DAG中各个子任务，以及DAG中任务间依赖关系
	 * @return fillbacksuc，backfilling操作的成功与否
	 */
	public static boolean fillback(DAG dagmap) {
	
		int runtime = dagmap.getsubmittime();
		boolean fillbacksuc = true; // 只要有一个任务失败就是全部失败

		boolean notfini = true; //并不是全部的任务都执行成功

		ArrayList<Task> readylist = new ArrayList<Task>();
		ArrayList<Task> DAGTaskList = new ArrayList<Task>();
		Map<String, Double> DAGTaskDependValue = new HashMap<String, Double>();
		DAGTaskDependValue = dagmap.getdependvalue();

		int DAGID = dagmap.getDAGId();

		for (int i = 0; i < dagmap.gettasklist().size(); i++) {
			DAGTaskList.add((Task) dagmap.gettasklist().get(i));
		}

		while (runtime <= dagmap.getDAGdeadline() && notfini && fillbacksuc) {

			// 遍历所有任务，为当前时刻执行完毕的任务设置参数
			for (Task dag : DAGTaskList) {
				if ((dag.getfillbackstarttime() + dag.getts()) == runtime
						&& dag.getfillbackready()
						&& dag.getfillbackdone() == false
						&& dag.getprefillbackdone() == true
						&& dag.getprefillbackready() == true) {
					dag.setfillbackfinishtime(runtime);
					dag.setfillbackdone(true);
				}
			}

			/**
			 * 
			 * 从此分为两部分 1、第一部分用于判断调度成功的的任务现在是否执行结束，若结束则设置结束时间
			 * 2、当前时刻是否有可以去调度的任务（就绪任务，有就去调度没有就略过）
			 * 
			 */
			for (Task dag : DAGTaskList) {
				// =====================设置本作业的第一个任务============
				if (dag.getid() == 0 && dag.getfillbackready() == false) {
					if (findfirsttaskslot(dagmap, DAGTaskList.get(0))) {// 当前DAG中起始节点能找到空闲时间段放入
						DAGTaskList.get(0).setprefillbackready(true);//
						DAGTaskList.get(0).setprefillbackdone(true);

						if (dag.getts() == 0) {// 如果这个任务是为了归一化而添加进去的起始节点
							dag.setfillbackfinishtime(dag.getfillbackstarttime());		
						} else {
							dag.setfillbackfinishtime(dag.getfillbackstarttime() + dag.getts());
						}
						dag.setfillbackdone(true);
						dag.setfillbackready(true);
						// 如果是单节点任务，那么就调度成功了呀~~
						if (dagmap.isSingle)
							return true;
					} else {// 起始任务插入失败
						fillbacksuc = false;
						System.out.println("DAG" + DAGID + "的第一个任务设置失败，从而整体失败");
						return fillbacksuc;
					}
				}


				// ================查询当前任务的所有父任务是否都以完成，若就绪就将当前任务加入就绪队列
				// ================查看当前时刻有没有就绪的任务加入就绪队列============
				if (dag.getfillbackdone() == false&& dag.getfillbackready() == false) {
					ArrayList<Task> pre_queue = new ArrayList<Task>();
					ArrayList<Integer> pre = new ArrayList<Integer>();
					pre = dag.getpre();
					if (pre.size() > 0) {
						boolean ready = true;
						// 获取当前任务的所有父任务
						for (int j = 0; j < pre.size(); j++) {
							Task buf = new Task();
							buf = getDAGById(dag.getdagid(), pre.get(j));
							if (buf.getfillbackdone() && buf.getfillbackready()) {
								continue;
							} else {
								ready = false;
								break;
							}
						}
						// 所有的父任务都完成了，就可以加入就绪队列
						if (ready) {
							// 当前任务加入就绪队列
							readylist.add(dag);
							// 设置本任务的父任务已经完成回填操作
							dag.setprefillbackready(true);
							dag.setprefillbackdone(true);
							dag.setfillbackready(true);
						}
					}
				}
			}

			// ===========对就绪队列进行调度================

			/**
			 * 这里没有设置fillbackdone的标记
			 */
			if (readylist.size() > 0) {
				// 就绪队列中有调度失败的，就直接设置整个任务失败
				if (!scheduling(dagmap, readylist)) {
					fillbacksuc = false;
					//System.out.println("调度当前就绪队列失败");
					return fillbacksuc;
				}
			}

			//用于合并的那个DAG作业
			for (Task dag : DAGTaskList) {
				if ((dag.getfillbackstarttime() + dag.getts()) == runtime
						&& dag.getfillbackready()
						&& dag.getfillbackdone() == false
						&& dag.getprefillbackdone() == true
						&& dag.getprefillbackready() == true) {
					dag.setfillbackfinishtime(runtime);
					dag.setfillbackdone(true);
				}
			}
			
			// 本作业中所有的任务都调度成功，则未完成设置为false，可以跳出循环
			notfini = false;
			// 查询当前时刻本DAG中所有的任务是否都是已经执行成功，若所有任务都成功则跳出时间循环
			// 本作业中有没调度成功的任务。
			for (Task dag : DAGTaskList) {
				if (dag.getfillbackdone() == false) {
					notfini = true;
					break;
				}
			}

			runtime = runtime + T;

		}

		//完整的设置整个作业的回填结束时间
		if (!notfini) {
			for (Task dag : DAGTaskList) {
				dag.setfillbackfinishtime(dag.getfillbackstarttime()+ dag.getts());
			}
		} else {
			fillbacksuc = false;
			System.out.println("本DAG为" + dagmap.getDAGId()+ ",这里退出的理由是：时间已经过期了，但是作业中还有没有被调度完成的任务");
		}
		return fillbacksuc;
	}

	/**
	 * 
	 * @Title: restoreSlotandTASK
	 * @Description: 还原SlotListInPes和TASKListInPes
	 * @param SlotListInPestemp
	 *            ，用于还原的SlotListInPes
	 * @param TASKListInPestemp
	 *            ，用于还原的TASKListInPes
	 * @return void
	 * @throws
	 */
	public static void restoreSlotandTASK(
			HashMap<Integer, ArrayList> SlotListInPestemp,
			HashMap<Integer, HashMap> TASKListInPestemp) {

		SlotListInPes.clear();
		TASKListInPes.clear();

		for (int k = 0; k < SlotListInPestemp.size(); k++) {
			ArrayList<Slot> slotListinpe = new ArrayList<Slot>();
			for (int j = 0; j < SlotListInPestemp.get(k).size(); j++) {
				Slot slottemp = new Slot();
				slottemp = (Slot) SlotListInPestemp.get(k).get(j);
				slotListinpe.add(slottemp);
			}
			SlotListInPes.put(k, slotListinpe);
		}
		for (int k = 0; k < TASKListInPestemp.size(); k++) {
			HashMap<Integer, Integer[]> TASKInPe = new HashMap<Integer, Integer[]>();
			for (int j = 0; j < TASKListInPestemp.get(k).size(); j++) {
				Integer[] temp = new Integer[4];
				temp = (Integer[]) TASKListInPestemp.get(k).get(j);
				TASKInPe.put(j, temp);
			}

			TASKListInPes.put(k, TASKInPe);
		}
		//repairTaskList();
	}

	/**
	 * @throws IOException
	 * 
	 * @Title: scheduleOtherDAG
	 * @Description: 使用Backfilling调度第i个DAG，若调度成功，进行LevelRelaxing操作，
	 *               并且修改TASKListInPes中各个TASK的开始结束时间，若调度不成功，取消该DAG的执行
	 * @param @param i，DAG的ID
	 * @param @param SlotListInPestemp，用于还原的SlotListInPes
	 * @param @param TASKListInPestemp，用于还原的TASKListInPes
	 * @return void
	 * @throws
	 */
	public static void scheduleOtherDAG(int i,HashMap<Integer, ArrayList> SlotListInPestemp,HashMap<Integer, HashMap> TASKListInPestemp) throws IOException {

		int arrive = DAGMapList.get(i).getsubmittime();
		if (arrive > current_time)
			current_time = arrive;
		// 判断本DAG的backfilling操作能否成功
		boolean fillbacksuc = fillback(DAGMapList.get(i));
		//System.out.println("===============当前作业+" +i+"是否调度成功："+fillbacksuc);
		
		// 如果不成功
		if (!fillbacksuc) {

			if(i==0){
				if(!DAGMapList.get(0).isMerge){
					// 修复调度之前的样貌
					restoreSlotandTASK(SlotListInPestemp, TASKListInPestemp);
					DAGMapList.get(0).setfillbackdone(false);
					// 整个作业忽略
					DAGMapList.get(0).setfillbackpass(true);
					// 设置作业中所有任务都为忽略（pass）
					ArrayList<Task> DAGTaskList = new ArrayList<Task>();
					for (int j = 0; j < DAGMapList.get(0).gettasklist().size(); j++) {
						DAGTaskList.add((Task) DAGMapList.get(0).gettasklist().get(j));
						DAGTaskList.get(j).setfillbackpass(true);
					}
					return ;		
				}
				
				//调度的是第一个做作业
				DAG tempDagMap=new DAG();
				tempDagMap = DAGMapList.get(0);
				ArrayList<Task> taskList=tempDagMap.gettasklist();
				//判断这个合并的作业中里面有哪些初始作业是成功的
				for(Task dag:taskList){
					if(mergeDAGEndNode.contains(dag.getid())){
						if(dag.getfillbackdone()){
							successMergeJob.add(dag.getOriDagId());
						}
					}
				}
				//System.out.println("原任务中调度成功的作业有"+successMergeJob.size());

				//如果原始作业中没有处理成功的，就按照以前的方式做
				if(successMergeJob.size()<=0){
					restoreSlotandTASK(SlotListInPestemp, TASKListInPestemp);
					DAGMapList.get(0).setfillbackdone(false);
					// 整个作业忽略
					DAGMapList.get(0).setfillbackpass(true);
					// 设置作业中所有任务都为忽略（pass）
					ArrayList<Task> DAGTaskList = new ArrayList<Task>();
					for (int j = 0; j < DAGMapList.get(0).gettasklist().size(); j++) {
						DAGTaskList.add((Task) DAGMapList.get(0).gettasklist().get(j));
						DAGTaskList.get(j).setfillbackpass(true);
					}
					return ;
				}else{
					DAGMapList.get(0).setfillbackdone(true);
					//System.out.println("这里会调用到吗？~~~~~~~~~~~~~~~~~~~~~");
					DAGMapList.get(0).setfillbackpass(false);
				}
				
				//得到这些成功作业的任务的现ID集合
				ArrayList<Integer> successTaskId=new ArrayList<Integer>();			
				for(Task dag:taskList){
					int oriDAGTaskId=dag.getOriDagId();
					for(int p=0;p<successMergeJob.size();p++){
						if(successMergeJob.get(p)==oriDAGTaskId){
							successTaskId.add(dag.getid());
							dag.setfillbackdone(true);
						}else{
							dag.setfillbackdone(false);
						}
					}
				}
			//	System.out.println("此时成功的任务有:"+successTaskId.get(0)+"\t"+successTaskId.get(1));
				//将这些成功的任务放置在处理器上
				// 数组0代表task开始时间，1代表task结束时间，2代表dagid，3代表id
				for(int j=0;j<pe_number;j++){
					HashMap<Integer, Integer[]> taskHashMap=TASKListInPes.get(j);
					HashMap<Integer, Integer[]> insteadTaskHashMap=new HashMap<>();
					int count=0;
					for(int k=0;k<taskHashMap.size();k++){
						Integer[] tempInfo=taskHashMap.get(k);
						if(successTaskId.contains(tempInfo[3])){
							insteadTaskHashMap.put(count, tempInfo);
							count++;
						}
					}
					System.out.println("count="+count);
					TASKListInPes.put(j, insteadTaskHashMap);
				}				
			}else{
				// 修复调度之前的样貌
				restoreSlotandTASK(SlotListInPestemp, TASKListInPestemp);
				DAGMapList.get(i).setfillbackdone(false);
				// 整个作业忽略
				DAGMapList.get(i).setfillbackpass(true);
				// 设置作业中所有任务都为忽略（pass）
				ArrayList<Task> DAGTaskList = new ArrayList<Task>();
				for (int j = 0; j < DAGMapList.get(i).gettasklist().size(); j++) {
					DAGTaskList.add((Task) DAGMapList.get(i).gettasklist().get(j));
					DAGTaskList.get(j).setfillbackpass(true);
				}
			}

		} else { // 如果本DAG的backfilling操作成功
			if(i==0){
				//如果调度成功，设置新版第一个DAG中的原始作业内容
				DAG tempDagMap=new DAG();
				tempDagMap = DAGMapList.get(0);
				ArrayList<Task> taskList=tempDagMap.gettasklist();
				//判断这个合并的作业中里面有哪些初始作业是成功的
				for(Task dag:taskList){
					if(mergeDAGEndNode.contains(dag.getid())){
						successMergeJob.add(dag.getOriDagId());
					}
				}
			//	System.out.println("作业0是成功调度的，其中包含的初始作业个数是："+successMergeJob.size());
			}
			
			
			
		//	System.out.println("当前成功的作业是："+i);
			DAGMapList.get(i).setfillbackdone(true);
			DAGMapList.get(i).setfillbackpass(false);
//			if (!DAGMapList.get(i).isSingle){
//				wholerelax(DAGMapList.get(i));
//			}
			repairTaskList();
		}
		
		//System.out.println("===============第一个会不会成功："+DAGMapList.get(0).getfillbackdone());
	}

	/**
	 * 
	* @Title: repairTaskList
	* @Description: 修正TaskList中的时间问题
	* @throws
	 */
	private static void repairTaskList() {
		
		for (int k = 0; k < TASKListInPes.size(); k++) {
			
			HashMap<Integer, Integer[]> TASKInPe = TASKListInPes.get(k);
			for (int j = 0; j < TASKInPe.size(); j++) {
				Integer[] temp = new Integer[4];
				temp=TASKInPe.get(j);
				
				Task tempDag=new Task();
				tempDag=getDAGById(temp[2], temp[3]);
				
				temp[0]=tempDag.getfillbackstarttime();
				temp[1]=tempDag.getfillbackfinishtime();
				TASKInPe.put(j, temp);
			}
			TASKListInPes.put(k, TASKInPe);
		}
		
	}

	/**
	 * @throws IOException 
	 * 
	* @Title: printTaskAndSlot
	* @Description: TODO:
	* @throws
	 */
	private static void printTaskAndSlot() throws IOException {
		// TODO Auto-generated method stub
		
		FileWriter writer = new FileWriter("G:\\task.txt", true);
		FileWriter slotWriter = new FileWriter("G:\\slot.txt", true);

		for(int i=0;i<pe_number;i++){
			HashMap<Integer, Integer[]> taskList=TASKListInPes.get(i);
			ArrayList slotList=SlotListInPes.get(i);
			for(int j=0;j<taskList.size();j++){
				Integer[] result=taskList.get(j);
				Task tempDag=new Task();
				tempDag=getDAGById(result[2], result[3]);
				boolean flag=false;
				if(tempDag.getlength()>(tempDag.getfillbackfinishtime()-tempDag.getfillbackstarttime())){
					flag=true;
				}
				writer.write("当前处理器："+i+"\t任务所属DAG"+tempDag.getOriDagId()+"\t任务编号"+tempDag.getOriID()+"\t开始时间"+result[0]+"="+tempDag.getfillbackstarttime()+"\t结束时间"+result[1]+"="+tempDag.getfillbackfinishtime()+"\t任务长度："+tempDag.getlength()+"\t长度是否合适："+flag+"\n");
			}
			
			for(int k=0;k<slotList.size();k++){
				Slot tempSlot=(Slot) slotList.get(k);
				slotWriter.write("处理器编号："+tempSlot.getPEId()+":"+tempSlot.getslotId()+"\t开始时间"+tempSlot.getslotstarttime()+"\t结束时间"+tempSlot.getslotfinishtime()+"\n");
			}
		}
		if (writer != null) {
			writer.close();
		}
		if (slotWriter != null) {
			slotWriter.close();
		}
		
	}

	/**
	 * 
	 * @Title: printTest
	 * @Description: 测试打印
	 * @param i
	 * @throws IOException
	 *             :
	 * @throws
	 */
	public static void printRelation(int i) throws IOException {

		FileWriter writer = new FileWriter("G:\\x.txt", true);
		DAG tempTestJob = DAGMapList.get(i);
		int dagid = tempTestJob.DAGId;
		int num = tempTestJob.tasknumber;
		for (int o = 0; o < num; o++) {
			Task tempDag = getDAGById(dagid, o);
			ArrayList<Integer> pre = tempDag.getpre();
			for (int p : pre) {
				Task tempPre = getDAGById(dagid, p);
				// if(tempPre.getfillbackstarttime()>tempDag.getfillbackstarttime())
				writer.write("DAG的id=" + i + "；父任务id=" + p + ";开始时间："
						+ tempPre.getfillbackstarttime() + ";子任务id=" + o
						+ ";子任务开始时间：" + tempDag.getfillbackstarttime() + "\n");
			}
		}
		if (writer != null) {
			writer.close();
		}

		/**
		 * 
		 */
	}

	/**
	 * 
	 * @Title: copySlot
	 * @Description: 
	 *               保存现在的SlotListInPes（里面放的是所有处理器各自匹配当前DAG的subimit--deadline时间段的slot
	 *               ），用于还原
	 * @param @return
	 * @return HashMap，SlotListInPestemp
	 * @throws
	 */
	public static HashMap copySlot() {
		HashMap<Integer, ArrayList> SlotListInPestemp = new HashMap<Integer, ArrayList>();

		for (int k = 0; k < SlotListInPes.size(); k++) {

			ArrayList<Slot> slotListinpe = new ArrayList<Slot>();

			for (int j = 0; j < SlotListInPes.get(k).size(); j++) {
				Slot slottemp = new Slot();
				slottemp = (Slot) SlotListInPes.get(k).get(j);
				slotListinpe.add(slottemp);
			}

			SlotListInPestemp.put(k, slotListinpe);
		}

		return SlotListInPestemp;
	}

	/**
	 * 
	 * @Title: copyTASK
	 * @Description: 保存现在的TASKListInPes，用于还原
	 * @param @return
	 * @return HashMap
	 * @throws
	 */
	public static HashMap copyTASK() {
		HashMap<Integer, HashMap> TASKListInPestemp = new HashMap<Integer, HashMap>();

		for (int k = 0; k < TASKListInPes.size(); k++) {
			HashMap<Integer, Integer[]> TASKInPe = new HashMap<Integer, Integer[]>();
			for (int j = 0; j < TASKListInPes.get(k).size(); j++) {
				Integer[] temp = new Integer[4];
				temp = (Integer[]) TASKListInPes.get(k).get(j);
				TASKInPe.put(j, temp);
			}
			TASKListInPestemp.put(k, TASKInPe);
		}

		return TASKListInPestemp;
	}

	/**
	 * @Description:计算调度结果中的关键路径
	 * 
	 * @param dagmap
	 *            ，DAG包括DAG中各个子任务，以及DAG中任务间依赖关系
	 * @return Criticalnumber，关键路径上的子任务个数
	 */
	private static int CriticalPath(DAG dagmap) {

		ArrayList<Task> DAGTaskList = new ArrayList<Task>();
		Map<String, Double> DAGTaskDependValue = new HashMap<String, Double>();

		DAGTaskDependValue = dagmap.getdependvalue(); // DAGDependValueMap

		for (int i = 0; i < dagmap.gettasklist().size(); i++) {
			DAGTaskList.add((Task) dagmap.gettasklist().get(i));
		}

		int Criticalnumber = 0;
		int i = DAGTaskList.size() - 1;

		/**
		 * 如果，本次提交的第一个DAG作业的的第一个节点是附加上去的，也就是长度为0。那么也还是返回为1
		 * 
		 * 
		 * 这里得好好看看阿
		 */
		if ((dagmap.tasknumber == 2) && (DAGTaskList.get(0).length == 0)) {
			return 1;
		}

		while (i >= 0) {
			// 整个DAG（整体）的最后一个任务一定在关键路径上，设置inCriticalPath为true
			if (i == (DAGTaskList.size() - 1)) {
				DAGTaskList.get(i).setinCriticalPath(true);
				Criticalnumber++;
			}

			int max = -1;
			int maxid = -1;
			Iterator<Integer> it = DAGTaskList.get(i).getpre().iterator();
			while (it.hasNext()) {
				int pretempid = it.next();
				int temp = (int) ((int) DAGTaskList.get(pretempid).getheftaft() + (double) DAGTaskDependValue
						.get(String.valueOf(pretempid + " " + i)));
				if (temp > max) {
					max = temp;
					maxid = pretempid;
				}
			}

			// System.out.println("maxid="+maxid);
			DAGTaskList.get(maxid).setinCriticalPath(true);
			Criticalnumber++;
			i = maxid;
			if (maxid == 0)
				i = -1;
		}
		return Criticalnumber;
	}

	/**
	 * @Description: 为调度成功的DAG进行levelRelaxing操作
	 * 
	 * @param dagmap
	 *            ，DAG包括DAG中各个子任务，以及DAG中任务间依赖关系
	 * @param deadline
	 *            ，DAG的截止时间
	 * @param Criticalnumber
	 *            ，关键路径上的子任务个数
	 * @throws IOException
	 */
	private static void levelrelax(DAG dagmap, int deadline,int Criticalnumber) throws IOException {
		ArrayList<Task> DAGTaskList = new ArrayList<Task>();
		ArrayList<Task> DAGTaskListtemp = new ArrayList<Task>();
		ArrayList<Task> setorderbystarttime = new ArrayList<Task>();

		Map<String, Double> DAGTaskDependValue = new HashMap<String, Double>();
		DAGTaskDependValue = dagmap.getdependvalue();

		for (int i = 0; i < dagmap.gettasklist().size(); i++) {
			DAGTaskList.add((Task) dagmap.gettasklist().get(i));
			DAGTaskListtemp.add((Task) dagmap.gettasklist().get(i));
		}

		// 计算每个任务所在的层级
		calculateOriginalLevel(DAGTaskList);

		// 依据调度结果设置新的层级
		calculateNewLevel(dagmap, DAGTaskList, DAGTaskListtemp,setorderbystarttime);

		// calculate weight
		// 计算各层权值从上往下
		int levelnumber = DAGTaskList.get(DAGTaskList.size() - 1).getnewlevel();// 本DAG中最后一各任务的层级
		int[] weight = new int[levelnumber];
		int[] relax = new int[DAGTaskList.size()];// 每个任务的冗余值
		int[] maxlength = new int[levelnumber + 1];
		int weightsum = 0; // 权值总和
		// 本DAG最后一个任务的回填结束时间
		int finishtime = DAGTaskList.get(DAGTaskList.size() - 1).getfillbackfinishtime();
		// 冗余总值
		int totalrelax = deadline - finishtime;

		// 相同层级的任务放在同一个list中
		for (int j = 1; j <= levelnumber; j++) {
			ArrayList<Integer> samelevel = new ArrayList<Integer>();
			for (int i = 0; i < dagmap.gettasklist().size(); i++) {
				if (DAGTaskList.get(i).getnewlevel() == j)
					samelevel.add(i);
			}
			dagmap.taskinlevel.put(j, samelevel);
		}

		for (int i = 1; i <= levelnumber; i++) {
			int max = 0, maxid = 0;
			// 计算得出本层的权值
			for (int j = 0; j < dagmap.taskinlevel.get(i).size(); j++) {
				Task dagtem = new Task();
				dagtem = getDAGById(dagmap.getDAGId(), (int) dagmap.taskinlevel.get(i).get(j));

				if (i == levelnumber) {// 如果是最后一层，那么权值设置为它的执行时间
					max = dagtem.getts();
					maxid = i;
				} else {
					int value = dagtem.getts();
					// 获取下一层当前任务的子任务
					for (int k = 0; k < dagmap.taskinlevel.get(i + 1).size(); k++) {
						Task dagsuc = new Task();
						dagsuc = getDAGById(dagmap.getDAGId(),
								(int) dagmap.taskinlevel.get(i + 1).get(k));
						if (dagmap.isDepend(String.valueOf(dagtem.getid()),String.valueOf(dagsuc.getid()))) {

							// 如果当前任务和子任务不在同一个处理器上
							if (dagtem.getfillbackpeid() != dagsuc.getfillbackpeid()) {
								int tempp = dagtem.getts()+ (int) (double) DAGTaskDependValue.get(dagtem.getid() + " "+ dagsuc.getid());
								if (value < tempp) {
									value = tempp;
									maxid = dagtem.getid();
								}
							}
						}
					}
					if (max < value) {
						max = value;
						maxid = dagtem.getid();
					}
				}
			}

			weight[i - 1] = max;
			maxlength[i - 1] = maxid;// 依据的任务ID
		}

		// 计算权值总和
		for (int i = 0; i < levelnumber; i++) {
			weightsum = weight[i] + weightsum;
		}

		// findcriticalnode 是最后结束的 还是执行时间最长的
		int maxpelength = 0;
		int maxpeid = 0;
		// 找寻所有处理器上最后一个任务执行结束的最晚时间与节点ID
		for (int i = 0; i < pe_number; i++) {
			HashMap<Integer, Integer[]> TASKInPe = new HashMap<Integer, Integer[]>();
			TASKInPe = TASKListInPes.get(i);
			if (TASKInPe.size() > 0) {
				if (maxpelength < TASKInPe.get(TASKInPe.size() - 1)[1]) {
					maxpelength = TASKInPe.get(TASKInPe.size() - 1)[1];
					maxpeid = i;
				}
			}
		}

		// 对应处理器上的所有任务都设置为iscriticalnode
		for (int i = 0; i < TASKListInPes.get(maxpeid).size(); i++) {
			HashMap<Integer, Integer[]> TASKInPe = new HashMap<Integer, Integer[]>();
			TASKInPe = TASKListInPes.get(maxpeid);
			Task dagtem = new Task();
			dagtem = getDAGById((int) TASKInPe.get(i)[2],(int) TASKInPe.get(i)[3]);
			dagtem.setiscriticalnode(true);
		}

		// 计算每一层的冗余值并设置在本层的任务对象中
		for (int i = 1; i <= levelnumber; i++) {
			for (int j = 0; j < dagmap.taskinlevel.get(i).size(); j++) {
				Task dagtem = new Task();
				dagtem = getDAGById(dagmap.getDAGId(), (int) dagmap.taskinlevel.get(i).get(j));
				int tem = weight[i - 1] * totalrelax / weightsum;
				dagtem.setrelax(tem);
			}
		}

		int relaxlength = DAGTaskList.get(0).getrelax();

		// 设置本DAG的第一个任务的fd
		DAGTaskList.get(0).setslidefinishdeadline(DAGTaskList.get(0).getrelax()+ DAGTaskList.get(0).getfillbackfinishtime());
		// 设置本DAG的第一个任务的sd
		DAGTaskList.get(0).setslidedeadline(DAGTaskList.get(0).getrelax()+ DAGTaskList.get(0).getfillbackstarttime());

		// 设置滑动长度
		DAGTaskList.get(0).setslidelength(DAGTaskList.get(0).getrelax());

		for (int i = 2; i <= levelnumber; i++) {
			Task dagtem1 = new Task();
			dagtem1 = getDAGById(dagmap.getDAGId(), (int) dagmap.taskinlevel.get(i - 1).get(0));

			// 同一层的fd是一样的。得到上一层的fd
			int starttime = dagtem1.getslidefinishdeadline();

			int finishdeadline = -1;

			// 设置本层各个任务的s、f
			for (int j = 0; j < dagmap.taskinlevel.get(i).size(); j++) {
				Task dagtem = new Task();
				dagtem = getDAGById(dagmap.getDAGId(), (int) dagmap.taskinlevel.get(i).get(j));

				dagtem.setfillbackstarttime(starttime);
				dagtem.setfillbackfinishtime(dagtem.getfillbackstarttime()+ dagtem.getts());
			}

			// 默认取关键路径上的任务的最迟结束时间为本层所有任务的最迟结束时间fd
			for (int j = 0; j < dagmap.taskinlevel.get(i).size(); j++) {
				Task dagtem = new Task();
				dagtem = getDAGById(dagmap.getDAGId(), (int) dagmap.taskinlevel.get(i).get(j));
				if (dagtem.getiscriticalnode()) {// 找到在关键路径上的任务
					finishdeadline = dagtem.getfillbackfinishtime()+ dagtem.getrelax();
					break;
				}
			}

			// 如果本层没有在关键路径上的任务，那么取本层所有任务中f最晚的时间为finishdeadline，fd
			if (finishdeadline == -1) {
				for (int j = 0; j < dagmap.taskinlevel.get(i).size(); j++) {
					Task dagtem = new Task();
					dagtem = getDAGById(dagmap.getDAGId(),
							(int) dagmap.taskinlevel.get(i).get(j));
					if (finishdeadline < dagtem.getfillbackfinishtime())
						finishdeadline = dagtem.getfillbackfinishtime();
				}

			}

			// 设置本层任务的fd、sd
			for (int j = 0; j < dagmap.taskinlevel.get(i).size(); j++) {
				Task dagtem = new Task();
				dagtem = getDAGById(dagmap.getDAGId(), (int) dagmap.taskinlevel.get(i).get(j));

				dagtem.setslidefinishdeadline(finishdeadline);
				dagtem.setslidedeadline(finishdeadline - dagtem.getts());
				// slidelength=sd-s
				dagtem.setslidelength(dagtem.getslidedeadline()- dagtem.getfillbackstarttime());
			}

		}

	}

	/**
	 * 
	 * @Title: FIFO
	 * @Description: 计算本算法的makespan
	 * @param @param dagmap，DAG包括DAG中各个子任务，以及DAG中任务间依赖关系
	 * @return void
	 * @throws
	 */
	public static void FIFO(DAG dagmap) {

		int time = current_time;

		ArrayList<Task> DAGTaskList = new ArrayList<Task>();// 本DAG中所有任务列表
		Map<String, Double> DAGTaskDependValue = new HashMap<String, Double>(); // 本DAG中所有任务间依赖的传输值
		DAGTaskDependValue = dagmap.getdependvalue();

		// 提取本DAG中所有任务加入到本你方法自定义的变量DAGTaskList中
		for (int i = 0; i < dagmap.gettasklist().size(); i++) {
			DAGTaskList.add((Task) dagmap.gettasklist().get(i));
		}

		
		
//		for(Entry<String, Double> map:DAGTaskDependValue.entrySet()){
//			String key=map.getKey();
//			double value=map.getValue();
//			System.out.println("进行fifo调度时的值的信息:\t"+key+"："+value);
//		}
//
//		System.out.println("+++++++++++++++++++++++++++结束");
		
		
		while (time <= timeWindow) { // timeWindow为处理器的总结束时间
			boolean fini = true;

			// 检查这个DAG里的任务是否进行过回填操作并且pass属性是否设置
			for (Task dag : DAGTaskList) {
				if (dag.getfillbackdone() == false&& dag.getfillbackpass() == false) {
					fini = false;
					break;
				}
			}

			// 如果当前DAG里有任务是进行过回填的，并且是回填失败的。那么就退出，不再进行后续操作
			if (fini) {
				break;
			}

			// 遍历所有任务，查看当前时间本该结束的任务是否能够执行完毕，设置fillbackfinishtime、fillbackdone
			for (Task dag : DAGTaskList) {
				if ((dag.getfillbackstarttime() + dag.getts()) == time&& dag.getfillbackready()&& dag.getfillbackdone() == false) {
					dag.setfillbackfinishtime(time);
					dag.setfillbackdone(true);
					PEList.get(dag.getfillbackpeid()).setfree(true);
				}
			}

			// 查询当前时刻有没有就绪的任务，有就加入就绪队列
			for (Task dag : DAGTaskList) {
				if (dag.getarrive() <= time && dag.getfillbackdone() == false&& dag.getfillbackready() == false&& dag.getfillbackpass() == false) {
					boolean ifready = checkready(dag, DAGTaskList,DAGTaskDependValue, time);
					if (ifready) {
						dag.setfillbackready(true);
						readyqueue.add(dag);// 就绪队列加入这个任务
					}
				}
			}

			// 调度就绪队列
			schedule(DAGTaskList, DAGTaskDependValue, time);

			for (Task dag : DAGTaskList) {
				if (dag.getfillbackstarttime() == time
						&& dag.getfillbackready()
						&& dag.getfillbackdone() == false) {

					if (dag.getdeadline() >= time) {
						if (dag.getts() == 0) {// 当前任务的执行时间为0，那么它应该是归一化时添加进去的节点，所以时间减T
							dag.setfillbackfinishtime(time);
							dag.setfillbackdone(true);
							time = time - T;
						} else {
							PEList.get(dag.getfillbackpeid()).setfree(false);// 其实整个代码没有用到这个字段啊
							PEList.get(dag.getfillbackpeid()).settask(
									dag.getid());// 该处理器不为空闲时正在处理的任务编号
						}
					} else {
						dag.setfillbackpass(true);
					}

				}

			}

			time = time + T;
		}

	}

	/**
	 * @Description:调度readyList
	 * 
	 * @param DAGTaskList
	 *            ，DAG（一个）子任务列表
	 * @param DAGTaskDependValue
	 *            ，该子任务所在的DAG的子任务间依赖关系
	 * @param time
	 *            ，当前时刻
	 */
	private static void schedule(ArrayList<Task> DAGTaskList,
			Map<String, Double> DAGTaskDependValue, int time) {

		ArrayList<Task> buff = new ArrayList<Task>();
		Task min = new Task();
		Task temp = new Task();

		// 将就绪队列中的所有任务按照到达的先后时间进行排序
		// 因为所有任务都是同属于一个DAG，那么他们的到达时间是一样的。
		for (int k = 0; k < readyqueue.size(); k++) {
			int tag = k;
			min = readyqueue.get(k);
			temp = readyqueue.get(k);
			for (int p = k + 1; p < readyqueue.size(); p++) {
				if (readyqueue.get(p).getarrive() < min.getarrive()) {
					min = readyqueue.get(p);
					tag = p;
				}
			}
			if (tag != k) {
				readyqueue.set(k, min);
				readyqueue.set(tag, temp);
			}
		}

		// 为在就绪队列中的每个任务挑选处理器，并设置在DAGTaskList的DAG对象中
		for (int i = 0; i < readyqueue.size(); i++) {
			Task buf1 = new Task();
			buf1 = readyqueue.get(i);

			for (Task dag : DAGTaskList) {
				if (buf1.getid() == dag.getid()) {
					// 为子任务选择处理器，选择可以最早开始处理的PE。
					// 设置：当前任务的fillbackpeid、ts、fillbackstarttime、finish_suppose、fillbackpass
					choosePE(dag, DAGTaskDependValue, time);
					break;
				}
			}
		}

		readyqueue.clear();

	}

	/**
	 * @Description:判断某一子任务是否达到就绪状态。不会设置任务对象中的fillbackready字段
	 * 
	 * @param dag
	 *            ，要判断的子任务dag
	 * @param DAGTaskList
	 *            ，DAG任务列表
	 * @param DAGTaskDependValue
	 *            ，该子任务所在的DAG的子任务间依赖关系
	 * @param time
	 *            ，当前时刻
	 * @return isready，该子任务dag是否可以加入readyList
	 */
	private static boolean checkready(Task dag, ArrayList<Task> DAGTaskList,
			Map<String, Double> DAGTaskDependValue, int time) {

		boolean isready = true;

		if (dag.getfillbackpass() == false && dag.getfillbackdone() == false) {
			if (time > dag.getdeadline()) {
				dag.setfillbackpass(true);
			}
			if (dag.getfillbackstarttime() == 0
					&& dag.getfillbackpass() == false) {
				ArrayList<Task> pre_queue = new ArrayList<Task>();
				ArrayList<Integer> pre = new ArrayList<Integer>();
				pre = dag.getpre();
				if (pre.size() >= 0) {
					for (int j = 0; j < pre.size(); j++) {
						Task buf3 = new Task();
						buf3 = getDAGById(dag.getdagid(), pre.get(j));
						pre_queue.add(buf3);

						if (buf3.getfillbackpass()) {
							dag.setfillbackpass(true);
							isready = false;
							break;
						}

						if (!buf3.getfillbackdone()) {
							isready = false;
							break;
						}

					}
				}
			}
		}

		return isready;
	}

	/**
	 * @Description:为当前任务选择处理器，选择可以最早开始处理的PE。设置：fillbackpeid、ts、fillbackstarttime、finish_suppose、fillbackpass
	 * 
	 * @param dag_temp
	 *            ，要选择处理器的DAG的子任务
	 * @param DAGTaskDependValue
	 *            ，该子任务所在的DAG的子任务间依赖关系
	 * @param time
	 *            ，当前时刻
	 */
	private static void choosePE(Task dag_temp,Map<String, Double> DAGTaskDependValue, int time) {

		// 获取当前任务的所有父任务对象
		ArrayList<Task> pre_queue = new ArrayList<Task>();
		ArrayList<Integer> pre = new ArrayList<Integer>();
		pre = dag_temp.getpre();
		if (pre.size() >= 0) {
			for (int j = 0; j < pre.size(); j++) {
				Task buf = new Task();
				buf = getDAGById(dag_temp.getdagid(), pre.get(j));
				pre_queue.add(buf);
			}
		}

		int temp[] = new int[PEList.size()];// 存的是在各处理器上的最早开始时间

		for (int i = 0; i < PEList.size(); i++) {
			HashMap<Integer, Integer[]> TASKInPe = new HashMap<Integer, Integer[]>();
			TASKInPe = TASKListInPes.get(i);

			if (pre_queue.size() == 0) {// 当前任务没有父任务，它就是开始节点
				if (TASKInPe.size() == 0) {// 当前遍历的处理器上没有执行过任务
					temp[i] = time;
				} else {
					if (time > TASKInPe.get(TASKInPe.size() - 1)[1])
						temp[i] = time;
					else
						temp[i] = TASKInPe.get(TASKInPe.size() - 1)[1];
				}
			} else if (pre_queue.size() == 1) {// 当前任务只有一个父任务
				if (pre_queue.get(0).getfillbackpeid() == PEList.get(i).getID()) {// 如果这个父任务在当前遍历的处理器上，不考虑处理器间的数据传输开销
					if (TASKInPe.size() == 0) {
						temp[i] = time;
					} else {
						if (time > TASKInPe.get(TASKInPe.size() - 1)[1])
							temp[i] = time;
						else
							temp[i] = TASKInPe.get(TASKInPe.size() - 1)[1];
					}
				} else {// 如果这个父任务不在当前遍历的处理器上，考虑处理器间的数据传输开销
					// value,处理器间数据传输的开销
					int value = (int) (double) DAGTaskDependValue.get(String
							.valueOf(pre_queue.get(0).getid())
							+ " "
							+ String.valueOf(dag_temp.getid()));
					if (TASKInPe.size() == 0) {// 当前遍历的处理器上没有执行过任务
						if ((pre_queue.get(0).getfillbackfinishtime() + value) < time)
							temp[i] = time;
						else
							temp[i] = pre_queue.get(0).getfillbackfinishtime()
									+ value;
					} else {
						if ((pre_queue.get(0).getfillbackfinishtime() + value) > TASKInPe
								.get(TASKInPe.size() - 1)[1]
								&& (pre_queue.get(0).getfillbackfinishtime() + value) > time)
							temp[i] = pre_queue.get(0).getfillbackfinishtime()
									+ value;
						else if (time > (pre_queue.get(0)
								.getfillbackfinishtime() + value)
								&& time > TASKInPe.get(TASKInPe.size() - 1)[1])
							temp[i] = time;
						else
							temp[i] = TASKInPe.get(TASKInPe.size() - 1)[1];
					}
				}
			} else {// 当前任务有多个父任务
				int max = time;

				// 遍历每个父任务
				for (int j = 0; j < pre_queue.size(); j++) {
					if (pre_queue.get(j).getfillbackpeid() == PEList.get(i)
							.getID()) {
						if (TASKInPe.size() != 0) {
							if (max < TASKInPe.get(TASKInPe.size() - 1)[1])
								max = TASKInPe.get(TASKInPe.size() - 1)[1];
						}
					} else {
						int valu = (int) (double) DAGTaskDependValue.get(String
								.valueOf(pre_queue.get(j).getid())
								+ " "
								+ String.valueOf(dag_temp.getid()));
						int value = pre_queue.get(j).getfillbackfinishtime()
								+ valu;

						if (TASKInPe.size() == 0) {
							if (max < value)
								max = value;
						} else {
							if (value <= TASKInPe.get(TASKInPe.size() - 1)[1]) {
								if (max < TASKInPe.get(TASKInPe.size() - 1)[1])
									max = TASKInPe.get(TASKInPe.size() - 1)[1];
							} else {
								if (max < value)
									max = value;
							}
						}
					}

				}

				temp[i] = max;
			}
		}

		// 选择该任务最早开始时间最小的处理器,min最早的时间，minpeid对应的处理器编号
		// 如果有最早开始时间相同的，那么久选择编号最小的处理器
		int min = timewindowmax;
		int minpeid = -1;
		for (int i = 0; i < PEList.size(); i++) {
			if (min > temp[i]) {
				min = temp[i];
				minpeid = i;
			}
		}

		if (min <= dag_temp.getdeadline()) { // 最早开始时间 < 该任务的截止时间

			HashMap<Integer, Integer[]> TASKInPe = new HashMap<Integer, Integer[]>();
			TASKInPe = TASKListInPes.get(minpeid);

			dag_temp.setfillbackpeid(minpeid);
			dag_temp.setts(dag_temp.getlength());
			dag_temp.setfillbackstarttime(min);
			dag_temp.setfinish_suppose(dag_temp.getfillbackstarttime()+ dag_temp.getts());

			Integer[] st_fi = new Integer[4];
			st_fi[0] = dag_temp.getfillbackstarttime(); // 任务的最早开始时间
			st_fi[1] = dag_temp.getfillbackstarttime() + dag_temp.getts(); // 任务的最早结束时间
			st_fi[2] = dag_temp.getdagid();
			st_fi[3] = dag_temp.getid();
			TASKInPe.put(TASKInPe.size(), st_fi);

		} else {
			dag_temp.setfillbackpass(true);
		}

	}

	/**
	 * @throws IOException
	 * 
	 * @Title: scheduleFirstDAG
	 * @Description: 
	 *               使用FIFO调度第一个DAG，若调度成功，进行LevelRelaxing操作，并且修改TASKListInPes中各个TASK的开始结束时间
	 * @param
	 * @return void
	 * @throws
	 */
	public static void scheduleFirstDAG() throws IOException {

		FIFO(DAGMapList.get(0));// DAGMapList所有DAG（整体）的信息

		// tem为第一个DAG流中最后一个任务
		Task tem = (Task) DAGMapList.get(0).gettasklist().get(DAGMapList.get(0).gettasknumber() - 1);

		// 如果第一个DAG流中最后一个任务已经被回填成功
		if (tem.getfillbackdone()) {
			DAGMapList.get(0).setfillbackdone(true);
			DAGMapList.get(0).setfillbackpass(false);
		}

		int Criticalnumber = 0;

		// 如果第一个提交的业务是单节点任务
		if (DAGMapList.get(0).isSingle) {
			Criticalnumber = 1; // find the critical path and get the task
								// number on it
		} else {
			Criticalnumber = CriticalPath(DAGMapList.get(0));
		}

		// 松弛首个DAG
		levelrelax(DAGMapList.get(0), DAGMapList.get(0).getDAGdeadline(),Criticalnumber); // relax the scheduling result

		// 改变各个处理器上的任务开始、结束的信息
		changetasklistinpe(DAGMapList.get(0));
	}

	// ==================初始化构建DAGxml中的内容=================
	// ==================初始化构建DAGxml中的内容=================
	// ==================初始化构建DAGxml中的内容=================
	// ==================初始化构建DAGxml中的内容=================
	// ==================初始化构建DAGxml中的内容=================
	// ==================初始化构建DAGxml中的内容=================

	/**
	 * @Description:创建DAGMAP实例并初始化
	 * 
	 * @param dagdepend
	 *            ，工作流依赖关系
	 * @param vcc
	 *            ，计算能力
	 */
	public static void initdagmap(DAGdepend dagdepend, PEComputerability vcc,String pathXML) throws Throwable {
		int pre_exist = 0;

		File file = new File(pathXML);
		String[] fileNames = file.list();
		int num = fileNames.length - 2;

		BufferedReader bd = new BufferedReader(new FileReader(pathXML+ "Deadline.txt"));
		String buffered;

		for (int i = 0; i < num; i++) {
			// 每个DAG有一个dagmap
			DAG dagmap = new DAG();
			DAGdepend dagdepend_persional = new DAGdepend();

			/*
			 * 清空DAG_queue_personal
			 * 里面只有当前这个DAG的任务列表
			 */
			DAG_queue_personal.clear();

			// 获取DAG的arrivetime和deadline，task个数
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

			taskTotal = taskTotal + tasknum;
			int arrivetime = buff[2];

			// 对每个DAG创建其任务间的相关依赖以及基本信息
			pre_exist = initDAG_createDAGdepend_XML(i, pre_exist, tasknum,arrivetime, pathXML);

			vcc.setComputeCostMap(ComputeCostMap);
			vcc.setAveComputeCostMap(AveComputeCostMap);

			dagdepend_persional.setDAGList(DAG_queue_personal);
			dagdepend_persional.setDAGDependMap(DAGDependMap_personal);
			dagdepend_persional.setDAGDependValueMap(DAGDependValueMap_personal);

		

			// 为DAG_queue中的数据设置截止时间
			int number_1 = DAG_queue.size();
			int number_2 = DAG_queue_personal.size();
			for (int k = 0; k < number_2; k++) {
				DAG_queue.get(number_1 - number_2 + k).setdeadline(DAG_queue_personal.get(k).getdeadline());
				DAG_queue.get(number_1 - number_2 + k).setdeadline(DAG_queue_personal.get(k).getSlotDeadLine());
			}

			dagmap.settasknumber(tasknum);
			dagmap.setDAGId(i);
			dagmap.setDAGdeadline(deadline);
			dagmap.setsubmittime(arrivetime);
			dagmap.settasklist(DAG_queue_personal);
			dagmap.setdepandmap(DAGDependMap_personal);
			dagmap.setdependvalue(DAGDependValueMap_personal);
			// 自后往前计算DAG中每个任务的截止时间
			createDeadline_XML(deadline, dagdepend_persional);
//			
//			createSlotDeadline(deadline, dagdepend_persional);
//			createDeadline(deadline, dagdepend_persional, dagmap);
			tempDAGMapList.add(dagmap);

		}

		mergeDAG();
		
//		for(int i=0;i<DAGMapList.size();i++){
//			System.out.println("当前作业的长度:"+DAGMapList.get(i)+"\t是否是被合并的："+DAGMapList.get(i).isMerge);
//		}
		
//		static List<Integer> mergeId;//被合并的作业的原始id集合
//		static ArrayList<Integer> mergeDAGEndNode;//合并的大作业中每个小作业的最后一个任务的集合
//		static ArrayList<Integer> successMergeJob;//合并的大作业中，成功调度的原始作业id

	//	System.out.println("被合并的作业的原始id集合："+mergeId.size()+"\t合并的大作业中每个小作业的最后一个任务的集合："+mergeDAGEndNode.size()+"\t合并的大作业中，成功调度的原始作业id："+successMergeJob.size());
		
		// 所有输入DAGxml文件构成的
		dagdepend.setdagmaplist(DAGMapList);
		dagdepend.setDAGList(DAG_queue);
		dagdepend.setDAGDependMap(DAGDependMap);
		dagdepend.setDAGDependValueMap(DAGDependValueMap);
		
		
		
//		System.out.println("总的dag数：" + dagdepend.getdagmaplist().size());	
//		for(int i=0;i<DAGMapList.size();i++){
//			printInitDagMap(i);
//		}

	}
	
	/**
	 * 
	* @Title: createDeadline
	* @Description: 依据关键路径求取每个任务的截止时间
	* @param dead_line
	* @param dagdepend_persion
	* @param dagmap
	* @throws Throwable:
	* @throws
	 */
	private static void createDeadline(int dead_line,DAGdepend dagdepend_persion, DAG dagmap) throws Throwable {

		int max;
		//计算关键路径，求得其每层分得的均值
		int avgDiff=getRelaxDeadline(dead_line,dagdepend_persion,dagmap);		
		Map<String, Double> transferValueMap=dagdepend_persion.getDAGDependValueMap();
		
		//从后往前推
		for (int k = DAG_queue_personal.size() - 1; k >= 0; k--) {
			max = Integer.MAX_VALUE;

			int from=DAG_queue_personal.get(k).getid();
			ArrayList<Integer> suc = new ArrayList<Integer>();
			suc = DAG_queue_personal.get(k).getsuc();

			// 选择所有子任务的中最早的开始时间为自己的截止时间，忽略的数据的传输开销
			if (suc.size() > 0) {
				for (int j = 0; j < suc.size(); j++) {
					int tem = 0;
					Task tempChild = new Task(); // 获取这个任务的子任务
					tempChild = getDAGById_task(suc.get(j));
					int to=suc.get(j);
					String key=from+" "+to;
					double value=transferValueMap.get(key);
					int childExe=tempChild.getlength()+avgDiff;
					
					tem=(int) (tempChild.getdeadline()-childExe-value);
					//System.out.println("父任务："+from+"\t 子任务："+to+"的截止时间是："+tempChild.getdeadline()+"\t伸缩后："+(tempChild.getlength()+avgDiff)+"传输时间为:"+value+" \t此时的截止时间为："+tem+"\t");
					
					if (max > tem)
						max = tem;
				}		
				DAG_queue_personal.get(k).setdeadline(max);

			} else {
				DAG_queue_personal.get(k).setdeadline(dead_line);
			}
		}
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
	private static int getRelaxDeadline(int dead_line,DAGdepend dagdepend, DAG dagmap) throws Throwable {
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
		//将列表反序，从小到大排列
		Collections.reverse(topoList);
		
//		StringBuffer sb=new StringBuffer();
//		for(int k=0;k<topoList.size();k++){
//			sb.append(topoList.get(k)).append(" ");
//		
//		}
//		System.out.println("原来的拓扑结构："+sb.toString());
		
		
		//得到关键路径
		criticalPath(topoList,ve,dagmap,dagdepend);
		
		//得到整个DAG的最大层数
		int levelNum=getMaxLevelNum(topoList,dagmap,dagdepend);
		//System.out.println("原来的最大层:"+levelNum);

		//求得每个任务的平均分配冗余
		int endIndex=topoList.get(topoList.size()-1);
		Task tempDag=taskList.get(endIndex);
		int newDeadline=ve[topoList.get(topoList.size()-1)]+tempDag.getlength()+dagmap.getsubmittime();
		int deadline=dagmap.getDAGdeadline();
		
		int deadlineDiff=deadline-newDeadline;
		
		int ex=(deadlineDiff)/levelNum;
		//System.out.println(dagmap.getDAGId()+"\tnewDeadline="+newDeadline+"\tdeadline="+deadline+"\tdeadlineDiff="+deadlineDiff+"\tlevelNum="+levelNum+"\tdeadline_ex="+ex);
		return ex;
		
	}
	
	


	/**
	 * 
	* @Title: topologicalSort
	* @Description: 获得其拓扑排序，返回的是其各个任务的最早开始执行时间
	* @param dagmap
	* @param ve
	* @param topo
	* @param dagdepend
	* @return:
	* @throws
	 */
	private static int[] topologicalSort(DAG dagmap, int[] ve,  Stack topo, DAGdepend dagdepend) {
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

	        Map<String,Double> valueMap=dagmap.getdependvalue();
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
	            		String key=currentTask+" "+m;
	            		Double value=valueMap.get(key)+curDag.getlength();//得加上执行时间
	            		 if(ve[currentTask] + value> ve[m])
	 	                    ve[m] = (int) (ve[currentTask] + value);
	            	}
	            }
	        } 
	        
//	        for(Entry<String, Double> map:valueMap.entrySet()){
//	        	System.out.println(""+map.getKey()+"\t距离："+map.getValue());
//	        }
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
            	// System.out.println("关键路径："+i);
            	 criticalNum++;
             }
         }         
         return criticalNum;
    } 

	
	/**
	 * @param topoList 
	 * 
	* @Title: getMaxLevelNum
	* @Description: 求取DAG的最大层数
	* @param dagmap
	* @param dagdepend
	* @return:
	* @throws
	 */
	private static int getMaxLevelNum(ArrayList<Integer> topoList, DAG dagmap, DAGdepend dagdepend) {
		
		ArrayList<Task> taskList=dagmap.gettasklist();
		int taskSize=dagmap.gettasknumber();
		int[] level=new int[taskSize];
		Map<String, Double> valueMap = dagmap.getdependvalue();
		
		for(int k=topoList.size()-1;k>=0;k--){
			int maxlevel=0;	
			int currentTask = (int) topoList.get(k);// 得到当前任务的编号	
			Task tempDag = taskList.get(currentTask);
			ArrayList<Integer> childs = tempDag.getsuc();
			
			if (childs == null){//是最后一个节点
				level[currentTask]=1;
			}else{
				for (int p = 0; p < childs.size(); p++) {
					int childNo=childs.get(p);
					if(maxlevel<level[childNo]){
						maxlevel=level[childNo];
					}
				}
				level[currentTask]=maxlevel+1;			
			}
		} 		
		return level[0];
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
	
	/**
	 * @throws IOException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * 
	* @Title: mergeDAG
	* @Description: 将初始的提交时间相同的DAG合成为一个
	* @param dAGMapList:
	* @throws
	 */
	private static void mergeDAG() throws IllegalAccessException, InvocationTargetException, IOException {
		//获取待合并的作业ID
		mergeId=new ArrayList<>();
		int tailTaskId=0;
		for(DAG dagMap:tempDAGMapList){
			Task tempDag=new Task();
			tempDag=(Task) dagMap.gettasklist().get(0);
			if(tempDag.getarrive()==0){
				mergeId.add(tempDag.getdagid());
				dagMap.setMerge(true);
				tailTaskId=tailTaskId+dagMap.gettasknumber();
				//System.out.println("====>待合并的任务"+tempDag.getdagid());
			}
		}	
		int behindDAGId=mergeId.get(mergeId.size()-1)+1;
		 mergeDAGEndNode=new ArrayList<>();
		
		/**
		 * 不需要合并
		 */
		if(mergeId.size()<=1){
			mergeId.clear();
			for(int i=0;i<tempDAGMapList.size();i++){
				DAG t=new DAG();
				t=tempDAGMapList.get(i);
				DAGMapList.add(t);
			}
			DAGMapList.get(0).setMerge(false);
			for(int i=0;i<DAGMapList.size();i++){
				DAG t=new DAG();
				t=DAGMapList.get(i);
				ArrayList<Task> taskList=t.gettasklist();
				for(int k=0;k<t.gettasknumber();k++){
					Task tempDag=taskList.get(k);
					int oriDagId=tempDag.getdagid();
					int oriTaskId=tempDag.getid();
					tempDag.setOriDagId(oriDagId);
					tempDag.setOriID(oriTaskId);
					tempDag.setdagid(i);
				}
				t.setDAGId(i);
			}
			return ;
		}
		
		
		DAG merDagMap=new DAG();
		merDagMap.setDAGId(0);
		merDagMap.setMerge(true);
		merDagMap.setsubmittime(0);
		
		ComputeCostMap = new HashMap<Integer, int[]>();
		AveComputeCostMap = new HashMap<Integer, Integer>();
		DAGDependMap_personal = new HashMap<Integer, Integer>();
		DAGDependValueMap_personal = new HashMap<String, Double>();
		
		ArrayList<Task> newTaskList=new ArrayList<>();
		int currentTaskId=0;
		
		Task newHeadTask = new Task();
		newHeadTask.setOriDagId(0);
		newHeadTask.setOriID(0);
		newHeadTask.setlength(0);
		newHeadTask.setts(0);
		newHeadTask.setid(currentTaskId);
		newHeadTask.setdagid(0);
		newHeadTask.setarrive(0);
		newHeadTask.setdeadline(0);
		newHeadTask.setSlotDeadLine(0);
		ArrayList<Integer> newHeadParent=new ArrayList<>();
		ArrayList<Integer> newHeadChild=new ArrayList<>();
		//以上是新的头结点的设置，还没有结束
		newHeadTask.setpre(newHeadParent);
		newTaskList.add(newHeadTask);
		
		
		Task newTailTask = new Task();
		newTailTask.setid(tailTaskId+1);
		//System.out.println("tailTaskId="+tailTaskId);
		
		ArrayList<Integer> newTailPre=new ArrayList<>();
		ArrayList<Integer> newTailChild=new ArrayList<>();
		
		int newDeadline=0;
		int newTaskNum=0;
		int flag=1;
		currentTaskId++;
		for(Integer id:mergeId){
			
			//System.out.println("id="+id);
			
			//得到当前需要合并的作业对象
			DAG currentDagMap=tempDAGMapList.get(id);
			//mergeLength=mergeLength+currentDagMap.getDAGdeadline()-currentDagMap.getsubmittime();
			int tasknumber=currentDagMap.gettasknumber();
			newTaskNum=newTaskNum+tasknumber;
			
			if(newDeadline<currentDagMap.getDAGdeadline()){
				newDeadline=currentDagMap.getDAGdeadline();
			}
			ArrayList<Task> taskList=currentDagMap.gettasklist();
			
			for (int j = 0; j < tasknumber; j++) {
				//新的task
				Task merDag = new Task();
				//原作业所对应的任务
				Task tempDag = new Task();
				tempDag=taskList.get(j);	
				
				Task dag_persional = new Task();
				
				BeanUtils.copyProperties(merDag,tempDag);
				BeanUtils.copyProperties(dag_persional,tempDag);
				merDag.setOriDagId(tempDag.getdagid());
				merDag.setOriID(tempDag.getid());
				
				merDag.setdagid(0);
				merDag.setid(currentTaskId);
				currentTaskId++;
				
				dag_persional.setid(Integer.valueOf(j).intValue());

				int x=merDag.getlength();
				int sum = 0;
				int[] bufferedDouble = new int[PEList.size()];
				for (int k = 0; k < PEList.size(); k++) { // x：任务的长度
					bufferedDouble[k] = Integer.valueOf(x/ PEList.get(k).getability());
					sum = sum + Integer.valueOf(x / PEList.get(k).getability());
				}
				ComputeCostMap.put(j, bufferedDouble); // 当前任务在每个处理器上的处理开销
				AveComputeCostMap.put(j, (sum / PEList.size())); // 当前任务在所有处理器上的平均处理开销
				
				ArrayList<Integer> parent=tempDag.getpre();
				ArrayList<Integer> newParent=new ArrayList<>();
				for(Integer per:parent){
					newParent.add(per+flag);
				}
				ArrayList<Integer> newChild=new ArrayList<>();
				ArrayList<Integer> child=tempDag.getsuc();
				for(Integer chi:child){
					newChild.add(chi+flag);
				}	
				
				if(j==0){
					newHeadChild.add(merDag.getid());
					newParent.add(0);//为原本开始的任务指定父节点为新加入的
					
					DAGDependMap_personal.put(0, merDag.getid());
					
					int from=0;
					int to=merDag.getid();
					String key=from+" "+to;
					DAGDependValueMap_personal.put(key, (double) 0);
					//System.out.println("加入新的头结点的链接信息："+key);
				}
				
				if(j==tasknumber-1){
					mergeDAGEndNode.add(merDag.getid());
					newChild.add(newTailTask.getid());
					newTailPre.add(merDag.getid());
					DAGDependMap_personal.put(merDag.getid(), newTailTask.getid());
					int from=merDag.getid();
					int to=newTailTask.getid();
					//System.out.println("to="+to);
					String key=from+" "+to;
					DAGDependValueMap_personal.put(key, (double) 0);
					//System.out.println("++++加入新的尾结点的链接信息："+key);
				}
				/**
				 * 改变父子列表内容
				 */
				merDag.replacePre(newParent);
				merDag.replaceChild(newChild);
				newTaskList.add(merDag); // 当前DAG（一个）的自有任务列表

			}

			HashMap<Integer, Integer> currentTaskDependMap = new HashMap<Integer, Integer>();
			currentTaskDependMap=currentDagMap.getDAGDependMap();
			HashMap<String, Double> currentTaskDependValueMap = new HashMap<String, Double>();
			currentTaskDependValueMap=currentDagMap.getdependvalue();

			for(Entry<Integer, Integer> map:currentTaskDependMap.entrySet()){
				int key=map.getKey()+flag;
				int value=map.getValue()+flag;
				DAGDependMap_personal.put(key, value);
			}
			
			for(Entry<String, Double> mmap:currentTaskDependValueMap.entrySet()){
				String[] key=mmap.getKey().split(" ");
				int newFrom=Integer.valueOf(key[0]).intValue()+flag;
				int newTo=Integer.valueOf(key[1]).intValue()+flag;
				
				String newKey=newFrom+" "+newTo;
				Double value=mmap.getValue();
				DAGDependValueMap_personal.put(newKey, value);
			}
			
			flag=flag+tasknumber;
				
		}
		
		
		newHeadTask.setsuc(newHeadChild);

		//设置尾节点信息
		newTailTask.setOriDagId(0);
		newTailTask.setOriID(currentTaskId);
		newTailTask.setlength(0);
		newTailTask.setts(0);
		newTailTask.setid(currentTaskId);
		newTailTask.setdagid(0);
		newTailTask.setarrive(0);
		newTailTask.setdeadline(newDeadline);
		newTailTask.setSlotDeadLine(newDeadline);
		newTailTask.setpre(newTailPre);
		newTailTask.setsuc(newTailChild);
		newTaskList.add(newTailTask);

		
		merDagMap.settasklist(newTaskList);
		merDagMap.settasknumber(newTaskList.size());
		merDagMap.setDAGdeadline(newDeadline);
		merDagMap.setdepandmap(DAGDependMap_personal);
		merDagMap.setdependvalue(DAGDependValueMap_personal);	
		DAGMapList.add(merDagMap);

	
		for(int i=behindDAGId;i<tempDAGMapList.size();i++){
			DAG t=new DAG();
			t=tempDAGMapList.get(i);
			DAGMapList.add(t);
		}
		
		for(int i=1;i<DAGMapList.size();i++){
			DAG t=new DAG();
			t=DAGMapList.get(i);
			ArrayList<Task> taskList=t.gettasklist();
			for(int k=0;k<t.gettasknumber();k++){
				Task tempDag=taskList.get(k);
				int oriDagId=tempDag.getdagid();
				int oriTaskId=tempDag.getid();
				tempDag.setOriDagId(oriDagId);
				tempDag.setOriID(oriTaskId);
				tempDag.setdagid(i);
			}
			t.setDAGId(i);
		}
		
		
		
//		System.out.println("更新后的作业数目："+DAGMapList.size());
//		
//		for(int j=0;j<DAGMapList.size();j++){
//			System.out.println("当前作业的任务数目:"+DAGMapList.get(j).gettasknumber()+"+++++++++++++++++" +
//					"当前作业编号："+j);
//			printInitDagMap(j);
//		}
		

		/**
		 * 
		 * 
		 * 
		 */
//		FileWriter writer = new FileWriter("G:\\initTaskList.txt", true);	
//		for(DAG mdag:newTaskList){
//			StringBuffer spre=new StringBuffer();
//			for(Integer pre:mdag.getpre()){
//				spre.append(pre).append(";");
//			}
//			StringBuffer schi=new StringBuffer();
//			for(Integer chi:mdag.getsuc()){
//				schi.append(chi).append(";");
//			}
//			writer.write(""+mdag.getdagid()+":"+mdag.getid()+"\t原始信息："+mdag.getOriDagId()+":"+mdag.getid()+"\t父节点有："+spre.toString()+"\t子节点有："+schi.toString()+"\n");
//		}
//		if (writer != null) {
//			writer.close();
//		}
	}

	/**
	 * 
	* @Title: printInitDagMap
	* @Description: 打印初始化生成的所有DAG的内容
	* @param i
	* @throws IOException:
	* @throws
	 */
	public static void printInitDagMap(int i) throws IOException {
		FileWriter writer = new FileWriter("G:\\DAGMapList.txt", true);
		DAG tempTestJob = DAGMapList.get(i);
		
		//System.out.println("当前作业的任务数目==========>:"+tempTestJob.gettasknumber()+"\t编号："+i);
		
		int dagid = tempTestJob.DAGId;
		int mergeDagIndex = i;
		
		int num = tempTestJob.tasknumber;
		
		for (int o = 0; o < num; o++) {
			//DAG tempDag = getMergeDAGById(mergeDagIndex, o);
			Task tempDag = getDAGById(mergeDagIndex, o);
			writer.write("作业编号："+tempDag.getdagid()+":"+tempDag.getid()+"\t到达时间："+tempDag.getarrive()+"\t结束时间："+tempDag.getdeadline()+
					"\t原DAG编号："+tempDag.getOriDagId()+":"+tempDag.getOriID()+"\n");
		}
		if (writer != null) {
			writer.close();
		}

	}

	

	// ===============================================================

	/**
	 * @Description:根据DAX文件为DAG添加相互依赖关系。更新创建了DAG_queue_personal、DAG_queue、AveComputeCostMap、ComputeCostMap、DAGDependValueMap_personal、DAGDependValueMap、DAGDependMap_personal、DAGDependMap
	 * 
	 * @param i
	 *            ，DAGID
	 * @param preexist
	 *            ，将所有的工作流中子任务全部添加到一个队列，在本DAG前已有preexist个任务
	 * @param tasknumber
	 *            ，DAG中任务个数
	 * @param arrivetimes
	 *            ，DAG到达时间
	 * @return back，将所有的工作流中子任务全部添加到一个队列，在本DAG全部添加后，有back个任务
	 */
	@SuppressWarnings("rawtypes")
	private static int initDAG_createDAGdepend_XML(int i, int preexist,int tasknumber, int arrivetimes, String pathXML)throws NumberFormatException, IOException, JDOMException {

		int back = 0;
		DAGDependMap_personal = new HashMap<Integer, Integer>();
		DAGDependValueMap_personal = new HashMap<String, Double>();
		ComputeCostMap = new HashMap<Integer, int[]>();
		AveComputeCostMap = new HashMap<Integer, Integer>();

		// 获取XML解析器
		SAXBuilder builder = new SAXBuilder();
		// 获取document对象
		Document doc = builder.build(pathXML + "/dag" + (i + 1) + ".xml");
		// 获取根节点
		Element adag = doc.getRootElement();

		for (int j = 0; j < tasknumber; j++) {
			Task dag = new Task();
			Task dag_persional = new Task();

			dag.setid(Integer.valueOf(preexist + j).intValue());
			// 为每个任务都设置上所属DAG的到达时间
			dag.setarrive(arrivetimes);
			dag.setdagid(i);
			dag_persional.setid(Integer.valueOf(j).intValue());
			dag_persional.setarrive(arrivetimes);
			dag_persional.setdagid(i);

			XPath path = XPath.newInstance("//job[@id='" + j + "']/@tasklength");
			List list = path.selectNodes(doc);
			Attribute attribute = (Attribute) list.get(0);
			// x：任务的长度
			int x = Integer.valueOf(attribute.getValue()).intValue();
			dag.setlength(x);
			dag.setts(x);
			dag_persional.setlength(x);
			dag_persional.setts(x);

			if (j == tasknumber - 1) {
				dag.setislast(true);
				islastnum++;
			}

			DAG_queue.add(dag); // 所有DAG的任务列表
			DAG_queue_personal.add(dag_persional); // 当前DAG（一个）的自有任务列表

			int sum = 0;
			int[] bufferedDouble = new int[PEList.size()];
			for (int k = 0; k < PEList.size(); k++) { // x：任务的长度
				bufferedDouble[k] = Integer.valueOf(x
						/ PEList.get(k).getability());
				sum = sum + Integer.valueOf(x / PEList.get(k).getability());
			}
			ComputeCostMap.put(j, bufferedDouble); // 当前任务在每个处理器上的处理开销
			AveComputeCostMap.put(j, (sum / PEList.size())); // 当前任务在所有处理器上的平均处理开销
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

			DAGDependMap.put(presuc[0], presuc[1]); // 所有DAG的任务的依赖映射，最终结果放的是这个任务最后一个子任务
			DAGDependValueMap.put((presuc[0] + " " + presuc[1]),(double) datasize);
			DAG_queue.get(presuc[0]).addToSuc(presuc[1]);
			DAG_queue.get(presuc[1]).addToPre(presuc[0]);

			DAGDependMap_personal.put(Integer.valueOf(pre_suc[0]).intValue(),Integer.valueOf(pre_suc[1]).intValue());
			DAGDependValueMap_personal.put((pre_suc[0] + " " + pre_suc[1]),(double) datasize);

			int tem0 = Integer.parseInt(pre_suc[0]);
			int tem1 = Integer.parseInt(pre_suc[1]);
			DAG_queue_personal.get(tem0).addToSuc(tem1);
			DAG_queue_personal.get(tem1).addToPre(tem0);

		}

		
//		for(Entry<Integer, Integer> map:DAGDependMap_personal.entrySet()){
//			System.out.println("======>dag:"+i+"\t"+map.getKey()+":"+map.getValue());
//		}
//		System.out.println("DAGDependValueMap_personal="+DAGDependValueMap_personal.size());
//		
		back = preexist + tasknumber;
		return back;
	}

	/**
	 * @Description:为DAG根据deadline，自后往前计算，给每个子任务计算相应的最迟截止时间，忽略任务之间的数据传输开销
	 * 
	 * @param dead_line
	 *            ，DAG的deadline
	 * @param dagdepend_persion
	 *            ，DAG中相互依赖关系
	 */
	private static void createDeadline_XML(int dead_line,DAGdepend dagdepend_persion) throws Throwable {
		int maxability = 1;// 处理器的处理能力，所有都默认为1
		int max;

		for (int k = DAG_queue_personal.size() - 1; k >= 0; k--) {

			max = Integer.MAX_VALUE;
			ArrayList<Task> suc_queue = new ArrayList<Task>();
			ArrayList<Integer> suc = new ArrayList<Integer>();
			suc = DAG_queue_personal.get(k).getsuc();
			

			StringBuffer sb=new StringBuffer();
			// 选择所有子任务的中最早的开始时间为自己的截止时间，忽略的数据的传输开销
			if (suc.size() > 0) {
				for (int j = 0; j < suc.size(); j++) {
					sb.append(suc.get(j)).append(";");
					int tem = 0;
					Task buf3 = new Task(); // 获取这个任务的子任务
					buf3 = getDAGById_task(suc.get(j));
					tem = (int) (buf3.getdeadline() - (buf3.getlength() / maxability));
				//	System.out.println("当前任务："+k+"\t子任务："+buf3.getid()+"\t相应时间是："+tem);
					if (max > tem)
						max = tem;
				}
				DAG_queue_personal.get(k).setdeadline(max);
				
			} else {
				DAG_queue_personal.get(k).setdeadline(dead_line);
			}
			
//			System.out.println("当前任务是"+k+"\t子任务有："+sb.toString());
	
		}
		
//		for (int i = DAG_queue_personal.size() - 1; i >= 0; i--) {
//			System.out.println("任务编号\t"+i+"任务的截止时间为："+DAG_queue_personal.get(i).getdeadline()+"\t执行时间:"+DAG_queue_personal.get(i).getts());
//		}
	}

	
	
	/**
	 * 
	* @Title: createSlotDeadline_XML
	* @Description: 设置slotDeadLine的值
	* @param dead_line
	* @param dagdepend_persion
	* @throws Throwable:
	* @throws
	 */
	private static void createSlotDeadline(int dead_line,DAGdepend dagdepend_persion) throws Throwable {
		int maxability = 1;// 处理器的处理能力，所有都默认为1
		int max = Integer.MAX_VALUE;

		for (int k = DAG_queue_personal.size() - 1; k >= 0; k--) {

			ArrayList<Task> suc_queue = new ArrayList<Task>();
			ArrayList<Integer> suc = new ArrayList<Integer>();
			suc = DAG_queue_personal.get(k).getsuc();

			if(k==DAG_queue_personal.size() - 1){
				DAG_queue_personal.get(k).setSlotDeadLine(dead_line);
				continue;
			}
			
			// 选择所有子任务的中最早的开始时间为自己的截止时间，带入数据的传输
			if (suc.size() > 0) {
				for (int j = 0; j < suc.size(); j++) {
					int tem = 0;
					Task buf3 = new Task(); // 获取这个任务的子任务
					buf3 = getDAGById_task(suc.get(j));
					suc_queue.add(buf3);
					tem = (int) (buf3.getdeadline() - (buf3.getlength() / maxability)-DAG_queue_personal.get(k).getlength());
					if (max > tem)
						max = tem;
				}
				DAG_queue_personal.get(k).setSlotDeadLine(max);
			} else {
				DAG_queue_personal.get(k).setSlotDeadLine(dead_line);
			}
			
			//System.out.println("此时的结束时间为："+k+"\t"+DAG_queue_personal.get(k).getSlotDeadLine());
		}
	}
	
	/**
	 * 
	 * @Title: initPE
	 * @Description: 创建PE实例并初始化。设置处理器的计算能力为1
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
	 * @Description:根据DAGID和TASKID返回该TASK实例
	 * 
	 * @param DAGId
	 *            ，DAGID
	 * @param dagId
	 *            ，TASKID
	 * @return DAG，TASK实例
	 */
	private static Task getDAGById(int DAGId, int dagId) {
		
		for (int i = 0; i < DAGMapList.get(DAGId).gettasknumber(); i++) {
			Task temp = (Task) DAGMapList.get(DAGId).gettasklist().get(i);
			if (temp.getid() == dagId)
				return temp;
		}

		return null;
	}
	
	
	private static Task getMergeDAGById(int mergeDagIndex, int dagId) {
		
		for (int i = 0; i < DAGMapList.get(mergeDagIndex).gettasknumber(); i++) {
			Task temp = (Task) DAGMapList.get(mergeDagIndex).gettasklist().get(i);
			if (temp.getid() == dagId)
				return temp;
		}

		return null;
	}

	/**
	 * @Description:根据TASKID返回该TASK实例
	 * 
	 * @param dagId
	 *            ，TASKID
	 * @return DAG，TASK实例
	 */
	private static Task getDAGById_task(int dagId) {
		for (Task dag : DAG_queue_personal) {
			if (dag.getid() == dagId)
				return dag;
		}
		return null;
	}

}
