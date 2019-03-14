package org.generate.util;

/**
 * 
 * @ClassName: BuildParameters
 * @Description: 构造DAG图生成的默认参数
 * @author YWJ
 * @date 2017-9-9 下午3:23:04
 */
public class CommonParametersUtil {

	public static int taskAverageLength = 40;

	public static int dagAverageSize = 40;

	public static int dagLevelFlag = 2;

	public static double deadLineTimes = 1.5;

	public static int processorNumber = 8;
	
	public static int timeWindow = 40000;

	public static double singleDAGPercent = 0.5;// 单个DAG产生的概率

	public static int FIFO = 0;
	public static int EDF = 0;
	public static int STF = 0;
	public static int EFTF = 0;
	public static int DCBF_ID = 0;
	public static int DCBF_LF = 0;
	public static int DCBF_SF = 0;
	public static int DCMG_ID = 0;
	public static int DCMG_LF = 0;
	public static int DCMG_SF = 0;

	public static int MCSW = 0;
	public static int HEFT = 0;

	public static int defaultRoundTime = 2;




	public static int getMCSW() {
		return MCSW;
	}

	public static void setMCSW(int mCSW) {
		MCSW = mCSW;
	}

	public static int getTimeWindow() {
		return timeWindow;
	}

	public static void setTimeWindow(int timeWindow) {
		CommonParametersUtil.timeWindow = timeWindow;
	}

	public static int getTaskAverageLength() {
		return taskAverageLength;
	}

	public static void setTaskAverageLength(int taskAverageLength) {
		CommonParametersUtil.taskAverageLength = taskAverageLength;
	}

	public static int getDagAverageSize() {
		return dagAverageSize;
	}

	public static void setDagAverageSize(int dagAverageSize) {
		CommonParametersUtil.dagAverageSize = dagAverageSize;
	}

	public static int getDagLevelFlag() {
		return dagLevelFlag;
	}

	public static void setDagLevelFlag(int dagLevelFlag) {
		CommonParametersUtil.dagLevelFlag = dagLevelFlag;
	}

	public static double getDeadLineTimes() {
		return deadLineTimes;
	}

	public static void setDeadLineTimes(double deadLineTimes) {
		CommonParametersUtil.deadLineTimes = deadLineTimes;
	}

	public static int getProcessorNumber() {
		return processorNumber;
	}

	public static void setProcessorNumber(int processorNumber) {
		CommonParametersUtil.processorNumber = processorNumber;
	}

	public static int getFIFO() {
		return FIFO;
	}

	public static void setFIFO(int fIFO) {
		FIFO = fIFO;
	}

	public static int getEDF() {
		return EDF;
	}

	public static void setEDF(int eDF) {
		EDF = eDF;
	}

	public static int getSTF() {
		return STF;
	}

	public static void setSTF(int sTF) {
		STF = sTF;
	}

	public static int getEFTF() {
		return EFTF;
	}

	public static void setEFTF(int eFTF) {
		EFTF = eFTF;
	}

	public static int getDefaultRoundTime() {
		return defaultRoundTime;
	}

	public static void setDefaultRoundTime(int defaultRoundTime) {
		CommonParametersUtil.defaultRoundTime = defaultRoundTime;
	}

	public static int getDCBF_ID() {
		return DCBF_ID;
	}

	public static void setDCBF_ID(int dCBF_ID) {
		DCBF_ID = dCBF_ID;
	}

	public static int getDCBF_LF() {
		return DCBF_LF;
	}

	public static void setDCBF_LF(int dCBF_LF) {
		DCBF_LF = dCBF_LF;
	}

	public static int getDCBF_SF() {
		return DCBF_SF;
	}

	public static void setDCBF_SF(int dCBF_SF) {
		DCBF_SF = dCBF_SF;
	}

	public static int getDCMG_ID() {
		return DCMG_ID;
	}

	public static void setDCMG_ID(int dCMG_ID) {
		DCMG_ID = dCMG_ID;
	}

	public static int getDCMG_LF() {
		return DCMG_LF;
	}

	public static void setDCMG_LF(int dCMG_LF) {
		DCMG_LF = dCMG_LF;
	}

	public static int getDCMG_SF() {
		return DCMG_SF;
	}

	public static void setDCMG_SF(int dCMG_SF) {
		DCMG_SF = dCMG_SF;
	}

	public static int getHEFT() {
		return HEFT;
	}

	public static void setHEFT(int hEFT) {
		HEFT = hEFT;
	}
	

	public static double getSingleDAGPercent() {
		return singleDAGPercent;
	}

	public static void setSingleDAGPercent(double singleDAGPercent) {
		CommonParametersUtil.singleDAGPercent = singleDAGPercent;
	}


}
