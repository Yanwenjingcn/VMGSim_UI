package UI.singleAnaly;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.compare.algo.DCBF_ID;
import org.compare.algo.DCBF_LF;
import org.compare.algo.DCBF_SF;
import org.compare.algo.DCMG_ID;
import org.compare.algo.DCMG_LF;
import org.compare.algo.DCMG_SF;
import org.compare.algo.HEFT;
import org.compare.algo.MCSW;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.generate.DagBuilder;
import org.generate.model.RandomDag;
import org.generate.model.TaskNode;
import org.generate.util.CommonParametersUtil;
import org.schedule.EDF;
import org.schedule.EFTF;
import org.schedule.FIFO;
import org.schedule.Makespan;
import org.schedule.STF;

import UI.parameters.UICommonParameters;

public class SingleAnalyUI {

	public static int width = UICommonParameters.width;
	public static int height = UICommonParameters.height;
	public static int timewind = UICommonParameters.timewind;

	public static int[][] color = UICommonParameters.color;
	public static int leftmargin = 110;
	public static int maxheight = 1000;
	static Display display = null;

	static ArrayList<Integer[]> locatConsole = new ArrayList<Integer[]>();
	static int loccountConsole = 0;

	static ArrayList<Integer[]> locatAlgo = new ArrayList<Integer[]>();
	static int loccountAlgo = 0;

	static Composite Consolecomposite;// 鎬荤殑
	public static Shell shell = UICommonParameters.shell;

	public static HashMap<String, Integer> TaskNums = new HashMap<>();

	/**
	 * 单页算法页面（不带内容）
	 * 
	 * @param tabFolder
	 * @param algorithmName
	 * @return
	 */
	public static TabItem getAlgorithmTabItem(TabFolder tabFolder, String algorithmName) {
		TabItem algoTabItem = new TabItem(tabFolder, SWT.NONE);
		algoTabItem.setText(algorithmName);
		/**
		 * 改变标签名称
		 */
		if(algorithmName.equals("DCBF_ID")) {
			algoTabItem.setText("DCBF_ID");
		}
		if(algorithmName.equals("DCBF_LF")) {
			algoTabItem.setText("DCBF_LF");
		}
		if(algorithmName.equals("DCBF_SF")) {
			algoTabItem.setText("DCBF_SF");
		}
		if(algorithmName.equals("DCMG_ID")) {
			algoTabItem.setText("DCMG_ID");
		}
		if(algorithmName.equals("DCMG_LF")) {
			algoTabItem.setText("DCMG_LF");
		}
		if(algorithmName.equals("DCMG_SF")) {
			algoTabItem.setText("DCMG_SF");
		}
		
		if(algorithmName.equals("MCSW")) {
			algoTabItem.setText("MCSW");
		}

		
		if(algorithmName.equals("HEFT")) {
			algoTabItem.setText("HEFT");
		}

		// 构建带滑块的组件页面
		ScrolledComposite scrolledCompositeAlgo = new ScrolledComposite(tabFolder,SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledCompositeAlgo.setExpandHorizontal(true);
		scrolledCompositeAlgo.setExpandVertical(true);

		algoTabItem.setControl(scrolledCompositeAlgo);

		// 构建滑块页面里面的内容（划线）
		Composite compositeAlgo = new Composite(scrolledCompositeAlgo, SWT.NONE);
		compositeAlgo.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				paintStartLine(e);
				paintEndLine(e);
			}
		});
		scrolledCompositeAlgo.setContent(compositeAlgo);
		scrolledCompositeAlgo.setMinSize(width, height);
		compositeAlgo.layout();

		Button btnSave = new Button(compositeAlgo, SWT.NONE);
		btnSave.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// SaveImagefifo();
			}
		});
		btnSave.setText("Save");
		btnSave.setBounds(20, 10, 65, 20);

		for (int i = 1; i <= CommonParametersUtil.processorNumber; i++) {
			Label lblprocessor = new Label(compositeAlgo, SWT.NONE);
			lblprocessor.setText("Processor" + i);
			lblprocessor.setBounds(20, 50 * i - 30 + 30, 75, 50);
		}

		// 往页面添加结果块
		addAlgothrimTask(compositeAlgo, algorithmName);
		return algoTabItem;
	}

	/**
	 * 
	 * @Title: addAlgothrimTask
	 * @Description: 灏嗙畻娉曚腑task鍧楃粨鏋滄坊鍔犲埌鐣岄潰涓�
	 * @param compositeAlgo
	 * @param AlgoName
	 * @return void
	 */
	public static void addAlgothrimTask(Composite compositeAlgo, String AlgoName) {

		int lengthtimes = 1;
		if (timewind < 800) {
			lengthtimes = (int) 800 / timewind;
		}

		if (CommonParametersUtil.timeWindow > 100000)
			Consolecomposite.dispose();

		Label lblrate = new Label(compositeAlgo, SWT.NONE);
		lblrate.setBounds(leftmargin + 5, 10, 700, 20);

		// 给各处理器上色
		Color timewindow = new Color(display, 230, 230, 230);
		for (int i = 0; i < CommonParametersUtil.processorNumber; i++) {
			Label lblproce = new Label(compositeAlgo, SWT.NONE);
			lblproce.setBackground(timewindow);
			lblproce.setBounds(leftmargin + 5, 50 * i + 35 + 30, timewind * lengthtimes, 10);
		}

		int taskNum = TaskNums.get(AlgoName);
		int[][] message = getMessage(AlgoName, lblrate);

		for (int k = 0; k < taskNum; k++) {
			int[] color = new int[3];
			color = getcolor(message[k][0]);
			Color dagcolor = new Color(display, color[0], color[1], color[2]);

			for (int j = 0; j < CommonParametersUtil.processorNumber; j++) {
				if (j == message[k][2]) {// message[k][2] 所在的处理器
					Label lbltask = new Label(compositeAlgo, SWT.BORDER);
					lbltask.setBackground(dagcolor);
					lbltask.setText("dag" + (message[k][0]+1) + ":task" + message[k][1]);
					lbltask.setAlignment(1);
					lbltask.setBounds((leftmargin + 5 + message[k][3] * lengthtimes), 50 * j + 15 + 30,
							(message[k][4] - message[k][3]) * lengthtimes, 20);

					Integer[] loc = new Integer[7];
					loc[0] = color[0];
					loc[1] = color[1];
					loc[2] = color[2];
					loc[3] = leftmargin + 5 + message[k][3];
					loc[4] = 50 * j + 15 + 30;
					loc[5] = message[k][4] - message[k][3];
					loc[6] = 20;
					locatAlgo.add(loc);
					loccountAlgo++;// 计数+1
					break;

				}

			}

		}
		locatAlgo.clear();
		loccountAlgo = 0;
	}

	private static int[][] getMessage(String itemname, Label lblrate) {

		DecimalFormat df = new DecimalFormat("0.00");
		// 构建TabItem
		if (itemname.equals("FIFO")) {
			lblrate.setText("    UR : " + df.format(Float.parseFloat(Makespan.rate[0][0])*100)+"%" + "    EUR : " + df.format(Float.parseFloat(Makespan.rate[0][2])*100)+"%" + "    SR : "
					+ df.format(Float.parseFloat(Makespan.rate[0][1])*100)+"%");
			return FIFO.message;

		}
		if (itemname.equals("EDF")) {
			lblrate.setText("    UR : " + df.format(Float.parseFloat(Makespan.rate[1][0])*100)+"%" + "    EUR : " + df.format(Float.parseFloat(Makespan.rate[1][2])*100)+"%" + "    SR : "
					+ df.format(Float.parseFloat(Makespan.rate[1][1])*100)+"%");
			return EDF.message;
		}
		if (itemname.equals("STF")) {
			lblrate.setText("    UR : " + df.format(Float.parseFloat(Makespan.rate[2][0])*100)+"%"+ "    EUR : " + df.format(Float.parseFloat(Makespan.rate[2][2])*100)+"%" + "    SR : "
					+ df.format(Float.parseFloat(Makespan.rate[2][1])*100)+"%");
			return STF.message;
		}
		if (itemname.equals("EFTF")) {
			lblrate.setText("    UR : " + df.format(Float.parseFloat(Makespan.rate[3][0])*100)+"%" + "    EUR : " + df.format(Float.parseFloat(Makespan.rate[3][2])*100)+"%" + "    SR : "
					+ df.format(Float.parseFloat(Makespan.rate[3][1])*100)+"%");
			return EFTF.message;
		}
		if (itemname.equals("DCBF_ID")) {
			lblrate.setText("    UR : " + df.format(Float.parseFloat(DCBF_ID.rateResult[0][0])*100)+"%" + "    EUR : " + df.format(Float.parseFloat(DCBF_ID.rateResult[0][1])*100)+"%" + "    SR : "
					+ df.format(Float.parseFloat(DCBF_ID.rateResult[0][2])*100)+"%");
			return DCBF_ID.message;
		}
		if (itemname.equals("DCBF_LF")) {
			lblrate.setText("    UR : " + df.format(Float.parseFloat(DCBF_LF.rateResult[0][0])*100)+"%" + "    EUR : " + df.format(Float.parseFloat(DCBF_LF.rateResult[0][1])*100)+"%" + "    SR : "
					+ df.format(Float.parseFloat(DCBF_LF.rateResult[0][2])*100)+"%");
			return DCBF_LF.message;
		}
		if (itemname.equals("DCBF_SF")) {
			lblrate.setText("    UR : " + df.format(Float.parseFloat(DCBF_SF.rateResult[0][0])*100)+"%" + "    EUR : " + df.format(Float.parseFloat(DCBF_SF.rateResult[0][1])*100)+"%" + "    SR : "
					+ df.format(Float.parseFloat(DCBF_SF.rateResult[0][2])*100)+"%");
			return DCBF_SF.message;
		}
		
		
		if (itemname.equals("DCMG_ID")) {
			lblrate.setText("    UR : " + df.format(Float.parseFloat(DCMG_ID.rateResult[0][0])*100)+"%" + "    EUR : " + df.format(Float.parseFloat(DCMG_ID.rateResult[0][1])*100)+"%" + "    SR : "
					+ df.format(Float.parseFloat(DCMG_ID.rateResult[0][2])*100)+"%");
			return DCMG_ID.message;
		}
		if (itemname.equals("DCMG_LF")) {
			lblrate.setText("    UR : " + df.format(Float.parseFloat(DCMG_LF.rateResult[0][0])*100)+"%" + "    EUR : " + df.format(Float.parseFloat(DCMG_LF.rateResult[0][1])*100)+"%" + "    SR : "
					+ df.format(Float.parseFloat(DCMG_LF.rateResult[0][2])*100)+"%");
			return DCMG_LF.message;
		}
		if (itemname.equals("DCMG_SF")) {
			lblrate.setText("    UR : " + df.format(Float.parseFloat(DCMG_SF.rateResult[0][0])*100)+"%" + "    EUR : " + df.format(Float.parseFloat(DCMG_SF.rateResult[0][1])*100)+"%" + "    SR : "
					+ df.format(Float.parseFloat(DCMG_SF.rateResult[0][2])*100)+"%");
			return DCMG_SF.message;
		}
		
		if (itemname.equals("MCSW")) {
			lblrate.setText("    UR : " + df.format(Float.parseFloat(MCSW.rateResult[0][0])*100)+"%" + "    EUR : " + df.format(Float.parseFloat(MCSW.rateResult[0][1])*100)+"%" + "    SR : "
					+ df.format(Float.parseFloat(MCSW.rateResult[0][2])*100)+"%");
			return MCSW.message;
		}
		
		if (itemname.equals("HEFT")) {
			lblrate.setText("    UR : " + df.format(Float.parseFloat(HEFT.rateResult[0][0])*100)+"%" + "    EUR : " + df.format(Float.parseFloat(HEFT.rateResult[0][1])*100)+"%" + "    SR : "
					+ df.format(Float.parseFloat(HEFT.rateResult[0][2])*100)+"%");
			return HEFT.message;
		}
		
		return null;
	}

	/**
	 * 
	 * @param tabFolder
	 * @param display
	 * @param text
	 * @return
	 */
	public static TabItem getConsoleTabItem(TabFolder tabFolder, Display display, String text) {

		putTaskNnums();

		TabItem tbtmConsole = new TabItem(tabFolder, SWT.NONE);
		tbtmConsole.setText(text);

		// 滚动面板
		ScrolledComposite scrolledComposite = new ScrolledComposite(tabFolder,SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		tbtmConsole.setControl(scrolledComposite);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);

		Consolecomposite = new Composite(scrolledComposite, SWT.NONE);
		Consolecomposite.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				paintStartLine(e);
				paintEndLine(e);
			}
		});
		scrolledComposite.setContent(Consolecomposite);
		scrolledComposite.setMinSize(UICommonParameters.width, UICommonParameters.height);
		Consolecomposite.layout();

		M_paintControl();

		displayTask();

		return tbtmConsole;

	}

	public static void M_paintControl() {

		// 保存图片的按钮
		Button btnSave = new Button(Consolecomposite, SWT.NONE);
		btnSave.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// SaveImage();
			}
		});
		btnSave.setText("Save");
		btnSave.setBounds(20, 10, 65, 20);

		// 设置处理器名称
		for (int i = 1; i <= CommonParametersUtil.processorNumber; i++) {
			Label lblprocessor = new Label(Consolecomposite, SWT.NONE);
			lblprocessor.setText("Processor" + i);
			lblprocessor.setBounds(20, 50 * i - 30 + 30, 75, 50);
		}

	}

	public static void displayTask() {
		int lengthtimes = 1;
		if (timewind < 800) {
			lengthtimes = (int) 800 / timewind;
		}

		Label lblrate = new Label(Consolecomposite, SWT.NONE);
		lblrate.setText("    UR : 100%    SR : 100%");
		lblrate.setBounds(leftmargin + 5, 10, 400, 20);

		Color timewindow = new Color(display, 230, 230, 230);
		for (int i = 0; i < CommonParametersUtil.processorNumber; i++) {
			Label lblproce = new Label(Consolecomposite, SWT.NONE);
			lblproce.setBackground(timewindow);
			lblproce.setBounds(leftmargin + 5, 50 * i + 35 + 30, timewind * lengthtimes, 10);
		}

		for (RandomDag dag : DagBuilder.finishDagList) {
			int[] color = new int[3];
			String[] number = dag.dagId.split("dag");

			color = getcolor(Integer.valueOf(number[1]).intValue());
			Color dagcolor = new Color(display, color[0], color[1], color[2]);

			List<String> nodeIdList = new ArrayList<String>();
			for (TaskNode node : dag.taskList) {
				nodeIdList.add(node.nodeId);
			}

			for (TaskNode node : dag.taskList) {
				if (node.getProcessorId() == 0)
					continue;
				for (int j = 1; j <= CommonParametersUtil.processorNumber; j++) {
					if (j == node.getProcessorId()) {
						Label lbltask = new Label(Consolecomposite, SWT.BORDER);
						lbltask.setBackground(dagcolor);
						lbltask.setText(dag.dagId + ":task" + nodeIdList.indexOf(node.nodeId));
						lbltask.setAlignment(1);
						lbltask.setBounds((leftmargin + 5 + node.startTime * lengthtimes), 50 * (j - 1) + 15 + 30,
								(node.endTime - node.startTime) * lengthtimes, 20);

						Integer[] loc = new Integer[7];
						loc[0] = color[0];
						loc[1] = color[1];
						loc[2] = color[2];
						loc[3] = leftmargin + 5 + node.startTime;
						loc[4] = 50 * (j - 1) + 15 + 30;
						loc[5] = node.endTime - node.startTime;
						loc[6] = 20;
						locatConsole.add(loc);
						loccountConsole++;
						break;
					}

				}

			}
		}
		locatConsole.clear();
		loccountConsole = 0;

	}

	/**
	 * 获取task应该对应的颜色
	 * 
	 * @param dagcount
	 * @return
	 */
	public static int[] getcolor(int dagcount) {
		return color[dagcount];
	}

	private void SaveImage() {
		FileDialog dlg = new FileDialog(shell, SWT.SAVE);
		dlg.setFilterExtensions(new String[] { "*.jpg" });
		dlg.open();
		String path = dlg.getFilterPath() + "\\" + dlg.getFileName();

		Image image = new Image(Consolecomposite.getDisplay(), width, height);
		GC gc = new GC(image);
		ShowImage(gc);

		ImageData imageData = image.getImageData();
		ImageLoader imageLoader = new ImageLoader();
		imageLoader.data = new ImageData[] { imageData };
		imageLoader.save(path, SWT.BITMAP);

		image.dispose();
		gc.dispose();
		MessageBox box = new MessageBox(shell);
		box.setMessage("Save successful!");
		box.open();

	}

	public void ShowImage(GC gc) {
		Color linecolor = new Color(display, 255, 255, 255);
		gc.setBackground(linecolor);
		gc.setLineWidth(2);
		gc.drawLine(leftmargin, 0, leftmargin, maxheight);

		gc.setLineWidth(2);
		gc.drawLine(leftmargin + 5 + 2 + timewind, 0, leftmargin + 5 + timewind, maxheight);

		for (int i = 1; i <= CommonParametersUtil.processorNumber; i++) {
			gc.setBackground(new Color(display, 255, 255, 255));
			gc.drawText("Processor" + i, 20, 50 * i - 30 + 30);
			gc.setBackground(new Color(display, 230, 230, 230));
			gc.drawRectangle(leftmargin + 5, 50 * (i - 1) + 35 + 30, timewind, 10);
			gc.fillRectangle(leftmargin + 5, 50 * (i - 1) + 35 + 30, timewind, 10);

		}

	}

	/**
	 * @param e:
	 * @throws @Title:
	 *             paintStartLine
	 * @Description: start line of time window
	 */
	public static void paintStartLine(PaintEvent e) {
		Color linecolor = new Color(display, 255, 255, 255);
		e.gc.setBackground(linecolor);
		e.gc.setLineWidth(2);
		e.gc.drawLine(leftmargin, 0, leftmargin, maxheight);

	}

	/**
	 * @param e:
	 * @throws @Title:
	 *             paintEndLine
	 * @Description: end line of time window
	 */
	public static void paintEndLine(PaintEvent e) {
		int lengthtimes = 1;
		if (timewind < 800) {
			lengthtimes = (int) 800 / timewind;
		}
		Color linecolor = new Color(display, 255, 255, 255);
		e.gc.setBackground(linecolor);
		e.gc.setLineWidth(2);
		e.gc.drawLine(leftmargin + 5 + 2 + timewind * lengthtimes, 0, leftmargin + 5 + timewind * lengthtimes,
				maxheight);
	}

	public static void putTaskNnums() {
		// 提取任务数
		TaskNums.put("FIFO", FIFO.tasknum);
		TaskNums.put("EDF", EDF.tasknum);
		TaskNums.put("STF", STF.tasknum);
		TaskNums.put("EFTF", EFTF.tasknum);
		TaskNums.put("DCBF_ID", DCBF_ID.finishTaskCount);
		TaskNums.put("DCBF_LF", DCBF_LF.finishTaskCount);
		TaskNums.put("DCBF_SF", DCBF_SF.finishTaskCount);
		TaskNums.put("DCMG_ID", DCMG_ID.finishTaskCount);
		TaskNums.put("DCMG_LF", DCMG_LF.finishTaskCount);
		TaskNums.put("DCMG_SF", DCMG_SF.finishTaskCount);
		TaskNums.put("MCSW", MCSW.finishTaskCount);
		TaskNums.put("HEFT", HEFT.finishTaskCount);

	}

}
