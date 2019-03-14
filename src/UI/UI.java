package UI;

import java.io.File;
import java.util.Random;

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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.ToolItem;
import org.generate.DagBuilder;
import org.generate.util.CommonParametersUtil;
import org.schedule.Makespan;

import UI.dialog.GUIAddRoundTimesSettingUI;
import UI.dialog.GUIAlgothrimSettingUI;
import UI.dialog.GUIParameterSettingUI;
import UI.muxAnaly.MuxAnalyUI;
import UI.parameters.UICommonParameters;
import UI.singleAnaly.SingleAnalyUI;
import swing2swt.layout.BorderLayout;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;

/**
 * 
 * 需要改进的地方：
 * 
 * 1、按钮的可见与不可见应该与tabfloder相反 2、每次点击还原后tabfloder应该是重新生成的
 * 3、tabfloder的可适应性变化 4、多次分析的内容完善（建议还是创建新的类吧）
 * 
 * 
 */

/**
 * 
 * @ClassName: UI
 * @Description: TODO
 * @author Wengie Yan
 * @date 2018年12月12日
 */
class UI {

	protected Shell shell;
	protected Display display;

	protected TabFolder tabFolder;

	ToolItem danci, duoci;
	protected Composite composite;
	ScrolledComposite scrolledComposite;

	static boolean flag = false;

	private static int[][] color = new int[500][3];
	private int leftmargin = 110;
	private int maxheight = 1000;
	private int width;
	private int height;
	private int timewind;
	int dagnummax = 10000;
	int mesnum = 5;

	public static int CaseCount = 0;
	public static String wfPath = System.getProperty("user.dir") + "\\DAG_XML\\";
	public static String rePath = System.getProperty("user.dir") + "\\MultiResult\\";
	public static String rootPath = System.getProperty("user.dir") ;

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			/**
			 * 1、清空case路径下的所有文件
			 */
			deleteWFPath(wfPath);
			
			//2、清空多轮分析模式下的文件
			deleteMultiResPath();
			UI window = new UI();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void deleteMultiResPath() {

		File reFile = new File(rePath);
		if (!reFile.exists()) {// 判断是否待删除目录是否存在
			reFile.mkdir();
			System.err.println("The dir are not exists!");
		}
		String[] reContent = reFile.list();// 取得当前目录下所有文件和文件夹
		for (String name : reContent) {
			File temp = new File(rePath, name);
			if (!temp.delete()) {// 直接删除文件
				System.err.println("Failed to delete " + name);
			}
		}
		
	}

	private static void deleteWFPath(String filePath) {
		File file = new File(filePath);
		if (!file.exists()) {// 判断是否待删除目录是否存在
			System.err.println("The dir are not exists!");
		}
		String[] content = file.list();// 取得当前目录下所有文件和文件夹
		for (String name : content) {
			File temp = new File(filePath, name);
			if (temp.isDirectory()) {// 判断是否是目录
				deleteWFPath(temp.getAbsolutePath());// 递归调用，删除目录里的内容
				temp.delete();// 删除空目录
			} else {
				if (!temp.delete()) {// 直接删除文件
					System.err.println("Failed to delete " + name);
				}
			}
		}

	}

	/**
	 * Open the window.
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
	 * Create contents of the window.
	 */
	protected void createContents() {
		shell = new Shell();
		UICommonParameters.shell = shell;
		shell.setSize(800, 500);
		shell.setText("VWGSim");
		shell.setLayout(new BorderLayout(0, 0));

		final Button button = new Button(shell, SWT.NONE);
		final Button button_1 = new Button(shell, SWT.NONE);
		button.setBounds(260, 100, 200, 80);// 这几个数应该是（x，y，长，宽）
		button_1.setBounds(260, 250, 200, 80);

		Menu menu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menu);

		// --------------------选择算法按钮------------------------
		MenuItem Chooseslgorithm = new MenuItem(menu, SWT.NONE);
		Chooseslgorithm.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ChooseAlgorithm();
			}
		});
		Chooseslgorithm.setText("Algorithms");

		// ----------------------构建参数按钮---------------------
		MenuItem buildPeremetersMenu = new MenuItem(menu, SWT.CASCADE);
		buildPeremetersMenu.setText("Set Parameters");

		Menu menu_2 = new Menu(buildPeremetersMenu);
		buildPeremetersMenu.setMenu(menu_2);

		MenuItem mntmBuildcommomparameters = new MenuItem(menu_2, SWT.NONE);
		mntmBuildcommomparameters.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				BuildParameters();
			}
		});
		mntmBuildcommomparameters.setText("Set Common Parameters");

		MenuItem mntmBuildroundtimes = new MenuItem(menu_2, SWT.NONE);
		mntmBuildroundtimes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				BuildRoundTimesParameters();
			}
		});
		mntmBuildroundtimes.setText("Set Cycle Times");

		// -----------------还原按钮------------------
		MenuItem mntmHuanyuan = new MenuItem(menu, SWT.NONE);
		mntmHuanyuan.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {

				// 点击后就设置这个按钮可见
				button.setVisible(true);
				button_1.setVisible(true);
				flag = true;
				Checkbox();

				// 关闭上一次的选项卡
				tabFolder.dispose();
				// 开启一次新的选项卡进行操作
				tabFolder = new TabFolder(shell, SWT.NONE);
				tabFolder.setBounds(0, 0, 780, 440);
				tabFolder.setLayoutData(BorderLayout.CENTER);

				clearAlgoFlag();

			}

			private void clearAlgoFlag() {
				CommonParametersUtil.FIFO = 0;
				CommonParametersUtil.EDF = 0;
				CommonParametersUtil.STF = 0;
				CommonParametersUtil.EFTF = 0;
				CommonParametersUtil.DCBF_ID = 0;
				CommonParametersUtil.DCBF_LF = 0;
				CommonParametersUtil.DCBF_SF = 0;
				CommonParametersUtil.DCMG_ID = 0;
				CommonParametersUtil.DCMG_LF = 0;
				CommonParametersUtil.DCMG_SF = 0;
				CommonParametersUtil.MCSW = 0;
				CommonParametersUtil.HEFT = 0;
				CommonParametersUtil.defaultRoundTime = 2;

				// 还清除已经生成的文件（现在可以不用了，因为平均值无所谓）

			}

			private void Checkbox() {
				// 按钮的可见与不可见应该与tab相反
				if (flag == true) {
					tabFolder.setVisible(false);
				} else {
					tabFolder.setVisible(true);
				}
			}
		});
		mntmHuanyuan.setText("Restore");

		// ----------------------工具按钮-----------------------
		MenuItem mntmTools = new MenuItem(menu, SWT.CASCADE);
		mntmTools.setText("Tools");

		Menu menu_1 = new Menu(mntmTools);
		mntmTools.setMenu(menu_1);

		MenuItem mntmHelp = new MenuItem(menu_1, SWT.NONE);
		mntmHelp.setText("Help");

		MenuItem mntmAbouttools = new MenuItem(menu_1, SWT.NONE);
		mntmAbouttools.setText("AboutTools");

		// ------------------单次分析--------------------------

		button.addMouseListener(new MouseAdapter() {
			// private String dataPath;

			@Override
			public void mouseUp(MouseEvent arg0) {
				
				deleteMultiResPath();
				// 点击后就设置这个按钮不可见
				button.setVisible(false);
				button_1.setVisible(false);
				tabFolder.setVisible(true);

				// 测试用例路径
				String casePath = wfPath + CaseCount + "\\";
				// 生成DAG
				DagBuilder dagbuilder = new DagBuilder(casePath);
				dagbuilder.initDags();

				// 计算常规的算法
				Makespan ms = new Makespan();
				try {
					ms.runMakespan_xml(casePath, "");
				} catch (Throwable throwable) {
					throwable.printStackTrace();
				}

				// 计算附加的新算法
				try {
					exeOtherAlgorithms(casePath);
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// 设置要展示的窗口大小以及高度
				width = (int) (CommonParametersUtil.timeWindow / CommonParametersUtil.processorNumber) + leftmargin
						+ 20;
				height = CommonParametersUtil.processorNumber * 50 + 100;
				timewind = (int) (CommonParametersUtil.timeWindow / CommonParametersUtil.processorNumber);

				UICommonParameters.width = width;
				UICommonParameters.height = height;
				UICommonParameters.timewind = timewind;

				// 随机产生新的颜色
				randomColor();
				UICommonParameters.color = color;

				// 展示生成DAG页面（concole）
				SingleAnalyUI.getConsoleTabItem(tabFolder, display, "Standard Result");

				// 展示所选择算法的单个 结果页面
				displayChooseAlgo();

				// 测试用例编号+1
				CaseCount++;

			}
		});
		button.setText("Single Analysis");

		// -----------------多次分析--------------

		button_1.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent arg0) {
				deleteMultiResPath();
				try {
					MuxAnalyUI.openOne(CaseCount, rootPath);
					clearAlgoFlag();
					CaseCount = CaseCount + CommonParametersUtil.defaultRoundTime;

				} catch (Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			private void clearAlgoFlag() {
				CommonParametersUtil.FIFO = 0;
				CommonParametersUtil.EDF = 0;
				CommonParametersUtil.STF = 0;
				CommonParametersUtil.EFTF = 0;
				CommonParametersUtil.DCBF_ID = 0;
				CommonParametersUtil.DCBF_LF = 0;
				CommonParametersUtil.DCBF_SF = 0;
				CommonParametersUtil.DCMG_ID = 0;
				CommonParametersUtil.DCMG_LF = 0;
				CommonParametersUtil.DCMG_SF = 0;
				CommonParametersUtil.MCSW = 0;
				CommonParametersUtil.HEFT = 0;
				CommonParametersUtil.defaultRoundTime = 2;
			}
		});
		button_1.setText("Multiple Analysis");

		tabFolder = new TabFolder(shell, SWT.NONE);
		tabFolder.setBounds(0, 0, 780, 440);
		tabFolder.setLayoutData(BorderLayout.CENTER);

	}

	private void displayChooseAlgo() {

		if (CommonParametersUtil.FIFO == 1) {
			DisplayNewResult("FIFO");
		}
		if (CommonParametersUtil.EDF == 1) {
			DisplayNewResult("EDF");
		}

		if (CommonParametersUtil.STF == 1) {
			DisplayNewResult("STF");
		}

		if (CommonParametersUtil.EFTF == 1) {
			DisplayNewResult("EFTF");
		}

		if (CommonParametersUtil.DCBF_ID == 1) {
			DisplayNewResult("DCBF_ID");
		}

		if (CommonParametersUtil.DCBF_LF == 1) {
			DisplayNewResult("DCBF_LF");
		}

		if (CommonParametersUtil.DCBF_SF == 1) {
			DisplayNewResult("DCBF_SF");
		}

		if (CommonParametersUtil.DCMG_ID == 1) {
			DisplayNewResult("DCMG_ID");
		}

		if (CommonParametersUtil.DCMG_LF == 1) {
			DisplayNewResult("DCMG_LF");
		}

		if (CommonParametersUtil.DCMG_SF == 1) {
			DisplayNewResult("DCMG_SF");
		}

		if (CommonParametersUtil.MCSW == 1) {
			DisplayNewResult("MCSW");
		}

		if (CommonParametersUtil.HEFT == 1) {
			DisplayNewResult("HEFT");
		}
	}

	/**
	 * @throws @Title: randomColor
	 * @Description: 随机生成任务块的颜色
	 */
	public void randomColor() {
		Random random = new Random();
		int max = 230;
		int min = 30;
		for (int i = 0; i < DagBuilder.finishDagList.size(); i++) {
			color[i][0] = random.nextInt(max) % (max - min + 1) + min;
			color[i][1] = random.nextInt(max) % (max - min + 1) + min;
			color[i][2] = random.nextInt(max) % (max - min + 1) + min;
		}

	}

	public int[] getcolor(int dagcount) {
		return color[dagcount];
	}

	public void exeOtherAlgorithms(String casePath) throws Throwable {

		if (CommonParametersUtil.DCBF_ID == 1) {
			DCBF_ID dcbf_ID = new DCBF_ID();
			dcbf_ID.runMakespan(casePath,rePath+"dcbf_id.txt");
		}

		if (CommonParametersUtil.DCBF_LF == 1) {
			DCBF_LF dcbf_LF = new DCBF_LF();
			dcbf_LF.runMakespan(casePath, rePath+"dcbf_lf.txt");
		}

		if (CommonParametersUtil.DCBF_SF == 1) {
			DCBF_SF dcbf_SF = new DCBF_SF();
			dcbf_SF.runMakespan(casePath, rePath+"dcbf_sf.txt");
		}

		if (CommonParametersUtil.DCMG_ID == 1) {
			DCMG_ID dcmg_ID = new DCMG_ID();
			dcmg_ID.runMakespan(casePath,rePath+ "dcmg_id.txt");
		}

		if (CommonParametersUtil.DCMG_LF == 1) {
			DCMG_LF dcmg_LF = new DCMG_LF();
			dcmg_LF.runMakespan(casePath, rePath+"dcmg_lf.txt");
		}

		if (CommonParametersUtil.DCMG_SF == 1) {
			DCMG_SF dcmg_SF = new DCMG_SF();
			dcmg_SF.runMakespan(casePath,rePath+ "dcmg_sf.txt");
		}

		if (CommonParametersUtil.MCSW == 1) {
			MCSW mcsw = new MCSW();
			mcsw.runMakespan(casePath, rePath+"mcsw.txt");
		}

		if (CommonParametersUtil.HEFT == 1) {
			HEFT heft = new HEFT();
			heft.runMakespan(casePath, rePath+"heft.txt");
		}

	}

	/**
	 * 
	 * @Title: ChooseAlgorithm
	 * @Description: 选择要参与比较的算法
	 * @return void
	 */
	public void ChooseAlgorithm() {

		// 构建一个新的页面（填写参数的那种）
		GUIAlgothrimSettingUI algorithmDialog = new GUIAlgothrimSettingUI(new Shell(), SWT.TITLE);

		if (algorithmDialog.open() != SWT.OK)
			return;

		// 接收里面获得的数据结果,是否选择了这个算法
		CommonParametersUtil.FIFO = algorithmDialog.FIFOFlag;
		CommonParametersUtil.EDF = algorithmDialog.EDFFlag;
		CommonParametersUtil.STF = algorithmDialog.STFFlag;
		CommonParametersUtil.EFTF = algorithmDialog.EFTFFlag;

		CommonParametersUtil.DCBF_ID = algorithmDialog.DCBF_IDFlag;
		CommonParametersUtil.DCBF_LF = algorithmDialog.DCBF_LFFlag;
		CommonParametersUtil.DCBF_SF = algorithmDialog.DCBF_SFFlag;
		CommonParametersUtil.DCMG_ID = algorithmDialog.DCMG_IDFlag;
		CommonParametersUtil.DCMG_LF = algorithmDialog.DCMG_LFFlag;
		CommonParametersUtil.DCMG_SF = algorithmDialog.DCMG_SFFlag;
		CommonParametersUtil.MCSW = algorithmDialog.MCSWFlag;
		CommonParametersUtil.HEFT = algorithmDialog.HEFTFlag;

	}

	/**
	 * 
	 * @Title: BuildParameters
	 * @Description:设置参数
	 * @return void
	 */
	public void BuildParameters() {

		GUIParameterSettingUI pasetdialog = new GUIParameterSettingUI(new Shell(), SWT.TITLE);

		if (pasetdialog.open() != SWT.OK)
			return;
		CommonParametersUtil.timeWindow = pasetdialog.timeWindow;
		CommonParametersUtil.taskAverageLength = pasetdialog.taskAverageLength;
		CommonParametersUtil.dagAverageSize = pasetdialog.dagAverageSize;
		CommonParametersUtil.dagLevelFlag = pasetdialog.dagLevelFlag;
		CommonParametersUtil.deadLineTimes = pasetdialog.deadLineTimes;
		CommonParametersUtil.processorNumber = pasetdialog.processorNumber;

		// System.out.println("CommonParametersUtil.timeWindow="+CommonParametersUtil.timeWindow+"*****************CommonParametersUtil.taskAverageLength="+CommonParametersUtil.taskAverageLength);
	}

	public void BuildRoundTimesParameters() {

		GUIAddRoundTimesSettingUI roundTimesdialog = new GUIAddRoundTimesSettingUI(new Shell(), SWT.TITLE);
		if (roundTimesdialog.open() != SWT.OK)
			return;
		System.out.println("CommonParametersUtil.defaultRoundTime=" + CommonParametersUtil.defaultRoundTime);

	}

	public void DisplayNewResult(final String algorithmName) {
		try {
			Display display = Display.getDefault();
			display.syncExec(new Runnable() {
				public void run() {

					SingleAnalyUI.getAlgorithmTabItem(tabFolder, algorithmName);

				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
