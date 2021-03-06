package UI.muxAnaly;

import java.io.File;

import org.compare.algo.DCBF_ID;
import org.compare.algo.DCBF_LF;
import org.compare.algo.DCBF_SF;
import org.compare.algo.DCMG_ID;
import org.compare.algo.DCMG_LF;
import org.compare.algo.DCMG_SF;
import org.compare.algo.HEFT;
import org.compare.algo.MCSW;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.generate.util.CommonParametersUtil;

import UI.parameters.UICommonParameters;

public class MuxAnalyUI {

	protected Object result;
	protected Shell shell;
	//上半部表格
	private static Table tableUp;
	//下半部表格
	private Table tableDown;


	/**
	 * Launch the application.
	 * 
	 * @param args
	 * @throws Throwable
	 */
	public static void openOne(int CaseCount,String RootPath) throws Throwable {
		
		String rePath=RootPath+"\\MultiResult\\";
		try {
			//判断存放结果的文件是否存在？
			File file=new File(rePath);
			if(!file.exists()) {
				file.mkdir();
			}
			
			//1、先进行多伦计算，获取计算结果
			BatchCalculate batchAnaly = new BatchCalculate(CommonParametersUtil.defaultRoundTime);
			batchAnaly.roundAnaly(CaseCount,RootPath);
			
			//2、将计算平均结果值翻入到UI的全局结果参数中
			UICommonParameters.resultMap = batchAnaly.getResult();

			//3、打开UI进行结果填充
			MuxAnalyUI muxAnalyUI = new MuxAnalyUI();
			muxAnalyUI.open();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the dialog.
	 * @wbp.parser.entryPoint
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {

		shell = new Shell();
		shell.setSize(800, 500);
		shell.setText("Multiple Analysis");
		shell.setLayout(new FillLayout(SWT.VERTICAL));

		Composite compositeUp = new Composite(shell, SWT.H_SCROLL | SWT.V_SCROLL);
		compositeUp.setLayout(new FillLayout(SWT.HORIZONTAL));

		tableUp = new Table(compositeUp, SWT.BORDER | SWT.FULL_SELECTION);

		Composite compositeDown = new Composite(shell, SWT.H_SCROLL | SWT.V_SCROLL);
		compositeDown.setLayout(new FillLayout(SWT.HORIZONTAL));
		tableDown = new Table(compositeDown, SWT.BORDER | SWT.FULL_SELECTION);
		
		
		tableDown.setHeaderVisible(true);
		tableDown.setLinesVisible(true);
		tableUp.setHeaderVisible(true);
		tableUp.setLinesVisible(true);

		// 向上表插入数据
		insertDataInTableUp();

		// 向下标插入数据
		insertDataInTableDown();
		
		

	}

	/**
	 * 
	 * @Title: insertDataInTableDown
	 * @Description: 鏋勫缓涓嬭〃鏍煎苟鍚戝叾涓彃鍏ユ暟鎹�
	 * @return void
	 */
	private void insertDataInTableDown() {
		// 创建下表头的字符串数组
		String[] tableDownHeader = { "Item", "Algorithms", "Data" };
		for (int i = 0; i < tableDownHeader.length; i++) {
			TableColumn tableColumn = new TableColumn(tableDown, SWT.NONE);
			tableColumn.setText(tableDownHeader[i]);
			// 设置表头可移动，默认为false
			tableColumn.setMoveable(true);
		}

		//往下表格中添加内容
		MuxAnalyConclusion.setConclusion(tableDown,UICommonParameters.resultMap);
		
		
		// 重新布局表格
		for (int i = 0; i < tableDownHeader.length; i++) {
			tableDown.getColumn(i).pack();
		}
		
	}
	
	/**
	 * 
	 * @Title: insertDataInTableUp
	 * @Description: 构建下表格并向其中插入数据
	 * @return void
	 */
	public static void insertDataInTableUp() {

		// 创建上表内容表头的字符串数组
		String[] tableUpHeader = { "Algorithms", "ER(%)", "UER(%)", "SR(%)", "Execute Time(ms)"};
		for (int i = 0; i < tableUpHeader.length; i++) {
			TableColumn tableColumn = new TableColumn(tableUp, SWT.NONE);
			tableColumn.setText(tableUpHeader[i]);
			// 设置表头可移动，默认为false
			tableColumn.setMoveable(true);
		}
		if (CommonParametersUtil.FIFO == 1) {
			insertDataUp("FIFO", UICommonParameters.resultMap.get("FIFO"));
		}
		if (CommonParametersUtil.EDF == 1) {
			insertDataUp("EDF", UICommonParameters.resultMap.get("EDF"));
		}

		if (CommonParametersUtil.STF == 1) {
			insertDataUp("STF", UICommonParameters.resultMap.get("STF"));
		}

		if (CommonParametersUtil.EFTF == 1) {
			insertDataUp("EFTF", UICommonParameters.resultMap.get("EFTF"));
		}

		if (CommonParametersUtil.DCBF_ID == 1) {
			insertDataUp("DCBF_ID", UICommonParameters.resultMap.get("DCBF_ID"));
		}
		

		if (CommonParametersUtil.DCBF_LF == 1) {
			insertDataUp("DCBF_LF", UICommonParameters.resultMap.get("DCBF_LF"));
		}

		if (CommonParametersUtil.DCBF_SF == 1) {
			insertDataUp("DCBF_SF", UICommonParameters.resultMap.get("DCBF_SF"));
		}

		if (CommonParametersUtil.DCMG_ID == 1) {
			insertDataUp("DCMG_ID", UICommonParameters.resultMap.get("DCMG_ID"));
		}

		if (CommonParametersUtil.DCMG_LF == 1) {
			insertDataUp("DCMG_LF", UICommonParameters.resultMap.get("DCMG_LF"));
		}

		if (CommonParametersUtil.DCMG_SF == 1) {
			insertDataUp("DCMG_SF", UICommonParameters.resultMap.get("DCMG_SF"));
		}

		if (CommonParametersUtil.MCSW == 1) {
			insertDataUp("MCSW", UICommonParameters.resultMap.get("MCSW"));
		}

		if (CommonParametersUtil.HEFT == 1) {
			insertDataUp("HEFT", UICommonParameters.resultMap.get("HEFT"));
		}
		
		/**
		 * 添加新算法时这里也要添加
		 */

		// 重新布局表格
		for (int i = 0; i < tableUpHeader.length; i++) {
			tableUp.getColumn(i).pack();
		}
	}

	private static void insertDataUp(String algoName, String data) {
		TableItem item;
		item = new TableItem(tableUp, SWT.NONE);
		String ER = data.split(",")[0];
		String UER = data.split(",")[1];
		String CR = data.split(",")[2];
		String ET = data.split(",")[3];
		item.setText(new String[] { algoName, ER, UER, CR, ET });
	}
}
