package UI.muxAnaly;

import org.compare.algo.DCBF_ID;
import org.compare.algo.DCBF_LF;
import org.compare.algo.DCBF_SF;
import org.compare.algo.DCMG_ID;
import org.compare.algo.DCMG_LF;
import org.compare.algo.DCMG_SF;
import org.compare.algo.HEFT;
import org.compare.algo.MCSW;
import org.eclipse.swt.internal.win32.TCHITTESTINFO;
import org.generate.DagBuilder;
import org.generate.util.CommonParametersUtil;
import org.schedule.Makespan;

import java.io.*;
import java.text.DecimalFormat;
import java.util.HashMap;

/**
 * 
 * @ClassName: BatchCalculate
 * @Description: 多次分析时用于进行计算与结果统计
 * @author Wengie Yan
 * @date 2018骞�12鏈�12鏃�
 */
public class BatchCalculate {

	//默认计算轮次为2
	private static int count = 2;
	
	//在多次分析后对每种算法的计算结果统计Map
	public static HashMap<String, String> resultMap=new HashMap<>();
	
	public static String CaseRootPath="";


	/**
	 * 
	 * @Description: 构造函数
	 * @param roundTime：用户选定的计算轮次
	 */
	public BatchCalculate(int roundTime) {
		count = roundTime;
	}

	/**
	 * 
	 * @Title: roundAnaly
	 * @Description: 轮次计算并计算结果平均值
	 * @param @throws Throwable
	 * @return void
	 * @throws
	 */
	
	/**
	 * 
	 * @Title: roundAnaly  
	 * @Description: TODO
	 * @param CaseCount :第一个用例的编号
	 * @param rootPath
	 * @throws Throwable
	 * @return void
	 */
	public static void roundAnaly(int CaseCount,String rootPath) throws Throwable {
		
		CaseRootPath=rootPath;
		//结果目录
		String resultPath=rootPath+"\\MultiResult\\";
		//用例目录
		String casePath=rootPath+"\\DAG_XML\\";
		System.out.println("用例起始编号"+CaseCount+"--循环轮次"+count);
		
		//1、循环计算;
		for (int i = 0; i < count; i++) {
			String inputPath=casePath+(CaseCount+i)+"\\";

			DagBuilder dagBuilder = new DagBuilder(inputPath);
			dagBuilder.initDags();
			//执行普遍的算法
			Makespan makespan=new Makespan();
			makespan.runMakespan_xml(inputPath,"");
			
			if (CommonParametersUtil.DCBF_ID == 1) {
				DCBF_ID dcbf_ID = new DCBF_ID();
				dcbf_ID.runMakespan(inputPath, resultPath+"dcbf_id.txt");
			}
			
			if (CommonParametersUtil.DCBF_LF == 1) {
				DCBF_LF dcbf_LF = new DCBF_LF();
				dcbf_LF.runMakespan(inputPath, resultPath+"dcbf_lf.txt");
			}
			
			if (CommonParametersUtil.DCBF_SF == 1) {
				DCBF_SF dcbf_SF = new DCBF_SF();
				dcbf_SF.runMakespan(inputPath, resultPath+"dcbf_sf.txt");
			}
			
			if (CommonParametersUtil.DCMG_ID == 1) {
				DCMG_ID dcmg_ID = new DCMG_ID();
				dcmg_ID.runMakespan(inputPath, resultPath+"dcmg_id.txt");
			}
			
			if (CommonParametersUtil.DCMG_LF == 1) {
				DCMG_LF dcmg_LF = new DCMG_LF();
				dcmg_LF.runMakespan(inputPath, resultPath+"dcmg_lf.txt");
			}
			
			if (CommonParametersUtil.DCMG_SF == 1) {
				DCMG_SF dcmg_SF = new DCMG_SF();
				dcmg_SF.runMakespan(inputPath, resultPath+"dcmg_sf.txt");
			}
			
			if (CommonParametersUtil.MCSW == 1) {
				MCSW mcsw = new MCSW();
				mcsw.runMakespan(inputPath, resultPath+"mcsw.txt");
			}
			
			if (CommonParametersUtil.HEFT == 1) {
				HEFT heft = new HEFT();
				heft.runMakespan(inputPath, resultPath+"heft.txt");
				System.out.println("=====================>"+resultPath+"heft.txt");
			}
		}

		//2、计算均值
		calculateAvg();
	}



	/**
	 * 
	 * @Title: calculateAvg
	 * @Description: 统计每种算法执行结果的平均值
	 * @param @throws FileNotFoundException
	 * @return void
	 * @throws
	 */
	private static void calculateAvg() throws FileNotFoundException {
		//清空结果Map
		resultMap.clear();
	
		
		String fifo = CaseRootPath+"\\MultiResult\\fifo.txt";
		if(CommonParametersUtil.FIFO==1){
			calcute(fifo,"FIFO");
		}else {
			clearFile(fifo);
		}
		
		String edf = CaseRootPath+"\\MultiResult\\edf.txt";
		if(CommonParametersUtil.EDF==1){
			
			calcute(edf,"EDF");
		}else {
			clearFile(edf);
		}
		

		String stf = CaseRootPath+"\\MultiResult\\stf.txt";
		if(CommonParametersUtil.STF==1){
			calcute(stf,"STF");
		}else {
			clearFile(stf);
		}
		
		String eftf = CaseRootPath+"\\MultiResult\\eftf.txt";
		if(CommonParametersUtil.EFTF==1){
			calcute(eftf,"EFTF");
		}else {
			clearFile(eftf);
		}
		
		String dcbf_id = CaseRootPath+"\\MultiResult\\dcbf_id.txt";
		if(CommonParametersUtil.DCBF_ID==1){
			calcute(dcbf_id,"DCBF_ID");
		}else {
			clearFile(dcbf_id);
		}
		/**
		 * 添加新算法这里要改
		 */
		String  dcbf_lf=CaseRootPath+"\\MultiResult\\dcbf_lf.txt";
		if(CommonParametersUtil.DCBF_LF==1){
			calcute(dcbf_lf,"DCBF_LF");
		}else {
			clearFile(dcbf_lf);
		}
		
		String  dcbf_sf= CaseRootPath+"\\MultiResult\\dcbf_sf.txt";
		if(CommonParametersUtil.DCBF_SF==1){
			calcute(dcbf_sf,"DCBF_SF");
		}else {
			clearFile(dcbf_sf);
		}
		
		String dcmg_id = CaseRootPath+"\\MultiResult\\dcmg_id.txt";
		if(CommonParametersUtil.DCMG_ID==1){
			calcute(dcmg_id,"DCMG_ID");
		}else {
			clearFile(dcmg_id);
		}
		String dcmg_lf = CaseRootPath+"\\MultiResult\\dcmg_lf.txt";
		if(CommonParametersUtil.DCMG_LF==1){
			calcute(dcmg_lf,"DCMG_LF");
		}else {
			clearFile(dcmg_lf);
		}
		
		String dcmg_sf = CaseRootPath+"\\MultiResult\\dcmg_sf.txt";
		if(CommonParametersUtil.DCMG_SF==1){
			calcute(dcmg_sf,"DCMG_SF");
		}else {
			clearFile(dcmg_sf);
		}
		
		String mcsw = CaseRootPath+"\\MultiResult\\mcsw.txt";
		if(CommonParametersUtil.MCSW==1){
			calcute(mcsw,"MCSW");
		}else {
			clearFile(mcsw);
		}
		
		String heft = CaseRootPath+"\\MultiResult\\heft.txt";
		if(CommonParametersUtil.HEFT==1){
			calcute(heft,"HEFT");
		}else {
			clearFile(heft);
		}
		
	}

	/**
	 * 
	 * @Title: clearFile
	 * @Description: 如果算法没有参与计算，则要清空计算结果
	 * @param @param path
	 * @return void
	 * @throws
	 */
	private static void clearFile(String path) {
		File f = new File(path);
		FileWriter fw;
		try {
			fw = new FileWriter(f);
			fw.write("");
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	/**
	 * 
	 * @Title: calcute
	 * @Description: 真正计算的位置
	 * @param path：结果文件所在的位置
	 * @throws FileNotFoundException
	 * @return void
	 */
	private static void calcute(String resultPath,String algoName) throws FileNotFoundException {

		File file = new File(resultPath);
		BufferedReader reader = null;
		int lineCount=0;//统计有多少行，这样就可以获取平均数值（其实传参也可以完成）
		try {
			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			double ER = 0;
			double CR = 0;
			double UER=0;
			double ET=0;//执行时长
			// 一次读入一行，直到读入null为文件结束
			while ((tempString = reader.readLine()) != null) {
				System.out.println(file.getName()+"\t"+tempString);
				String[] split = tempString.split("\t");
				ER += Double.valueOf(split[0]);
				UER += Double.valueOf(split[1]);
				CR += Double.valueOf(split[2]);
				ET += Double.valueOf(split[3]);
				lineCount++;
			}
			reader.close();

			File f = new File(resultPath);
			FileWriter fw = new FileWriter(f);
			fw.write("");
			fw.close();
			
			DecimalFormat df = new DecimalFormat("0.00");
			resultMap.put(algoName, df.format(((double) ER / lineCount)*100) + "," +df.format(((double) UER / lineCount)*100)+ "," + df.format(((double) CR / lineCount)*100)+ "," +Float.parseFloat(df.format( ((double) ET / lineCount))));
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
		}
	}

	public HashMap<String, String> getResult() {
		System.out.println("resultMap.size()"+resultMap.size());
		return resultMap;
	}
}
