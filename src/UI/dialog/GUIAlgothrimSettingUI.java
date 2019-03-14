package UI.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class GUIAlgothrimSettingUI extends Dialog {

	protected int result;
	protected Shell shell;

	//如果算法被选择，这里会被标记为1。
	public int FIFOFlag = 0;
	public int EDFFlag = 0;
	public int STFFlag = 0;
	public int EFTFFlag = 0;
	public int DCBF_IDFlag = 0;
	public int DCBF_LFFlag = 0;
	public int DCBF_SFFlag = 0;
	public int DCMG_IDFlag = 0;
	public int DCMG_LFFlag = 0;
	public int DCMG_SFFlag = 0;
	public int MCSWFlag = 0;

	public int HEFTFlag = 0;
	/**
	 * Create the dialog.
	 * 
	 * @param parent
	 * @param style
	 */
	public GUIAlgothrimSettingUI(Shell parent, int style) {
		super(parent, style);
		setText("SWT Dialog");
	}

	/**
	 * Open the dialog.
	 * 
	 * @return the result
	 */
	public int open() {
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {

		shell = new Shell(getParent(), getStyle());
		shell.setSize(410, 780);
		shell.setText("Algorithms");
		shell.setLayout(null);

		Label FIFOLabel = new Label(shell, SWT.NONE);
		FIFOLabel.setText("FIFO");
		FIFOLabel.setBounds(70, 30, 58, 17);

		Label EDFLabel = new Label(shell, SWT.NONE);
		EDFLabel.setText("EDF");
		EDFLabel.setBounds(70, 80, 88, 17);

		Label STFLabel = new Label(shell, SWT.NONE);
		STFLabel.setText("STF");
		STFLabel.setBounds(70, 130, 88, 17);

		Label EFTFLabel = new Label(shell, SWT.NONE);
		EFTFLabel.setText("EFTF");
		EFTFLabel.setBounds(70, 180, 88, 17);

		Label DCBF_IDLabel = new Label(shell, SWT.NONE);
		DCBF_IDLabel.setText("DCBF_ID");
		DCBF_IDLabel.setBounds(70, 230, 114, 17);
		
		Label DCBF_LFLabel = new Label(shell, SWT.NONE);
		DCBF_LFLabel.setText("DCBF_LF");
		DCBF_LFLabel.setBounds(70, 280, 114, 17);
		
		Label DCBF_SFLabel = new Label(shell, SWT.NONE);
		DCBF_SFLabel.setText("DCBF_SF");
		DCBF_SFLabel.setBounds(70, 330, 114, 17);
		
		Label DCMG_IDLabel = new Label(shell, SWT.NONE);
		DCMG_IDLabel.setText("DCMG_ID");
		DCMG_IDLabel.setBounds(70, 380, 114, 17);
		
		Label DCMG_LFLabel = new Label(shell, SWT.NONE);
		DCMG_LFLabel.setText("DCMG_LF");
		DCMG_LFLabel.setBounds(70, 430, 114, 17);
		
		Label DCMG_SFLabel = new Label(shell, SWT.NONE);
		DCMG_SFLabel.setText("DCMG_SF");
		DCMG_SFLabel.setBounds(70, 480, 114, 17);
		
		Label MCSWLabel = new Label(shell, SWT.NONE);
		MCSWLabel.setText("MCSW");
		MCSWLabel.setBounds(70, 530, 114, 17);
		
		Label HEFTLabel = new Label(shell, SWT.NONE);
		HEFTLabel.setText("HEFT");
		HEFTLabel.setBounds(70, 580, 114, 17);


	       /**
         * 每行的纵左边都是间隔50
         */
		final Button FIFOButton = new Button(shell, SWT.CHECK);
		FIFOButton.setBounds(220, 30, 90, 17);
		FIFOButton.setText("FIFO");
		
		final Button EDFButton = new Button(shell, SWT.CHECK);
		EDFButton.setBounds(220, 80, 97, 17);
		EDFButton.setText("EDF");
		
		final Button STFButton = new Button(shell, SWT.CHECK);
		STFButton.setBounds(220, 130, 97, 17);
		STFButton.setText("STF");
		
		final Button EFTFButton = new Button(shell, SWT.CHECK);
		EFTFButton.setBounds(220, 180, 97, 17);
		EFTFButton.setText("EFTF");
		
		final Button DCBF_IDButton = new Button(shell, SWT.CHECK);
		DCBF_IDButton.setBounds(220, 230, 126, 17);
		DCBF_IDButton.setText("DCBF_ID");
		
		final Button DCBF_LFButton = new Button(shell, SWT.CHECK);
		DCBF_LFButton.setBounds(220, 280, 126, 17);
		DCBF_LFButton.setText("DCBF_LF");
		
		final Button DCBF_SFButton = new Button(shell, SWT.CHECK);
		DCBF_SFButton.setBounds(220, 330, 126, 17);
		DCBF_SFButton.setText("DCBF_SF");
		
		final Button DCMG_IDButton = new Button(shell, SWT.CHECK);
		DCMG_IDButton.setBounds(220, 380, 126, 17);
		DCMG_IDButton.setText("DCMG_ID");
		
		final Button DCMG_LFButton = new Button(shell, SWT.CHECK);
		DCMG_LFButton.setBounds(220, 430, 126, 17);
		DCMG_LFButton.setText("DCMG_LF");
		
		final Button DCMG_SFButton = new Button(shell, SWT.CHECK);
		DCMG_SFButton.setBounds(220, 480, 126, 17);
		DCMG_SFButton.setText("DCMG_SF");
		
		final Button MCSWButton = new Button(shell, SWT.CHECK);
		MCSWButton.setBounds(220, 530, 126, 17);
		MCSWButton.setText("MCSW");
		
		final Button HEFTButton = new Button(shell, SWT.CHECK);
		HEFTButton.setBounds(220, 580, 126, 17);
		HEFTButton.setText("HEFT");
		
		
		

		//确定按钮
		Button btnNewButton = new Button(shell, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				if (FIFOButton.getSelection()) {
					FIFOFlag = 1;
				} else {
					FIFOFlag = 0;
				}

				if (EDFButton.getSelection()) {
					EDFFlag = 1;
				} else {
					EDFFlag = 0;
				}

				if (STFButton.getSelection()) {
					STFFlag = 1;
				} else {
					STFFlag = 0;
				}

				if (EFTFButton.getSelection()) {
					EFTFFlag = 1;
				} else {
					EFTFFlag = 0;
				}
				if (DCBF_IDButton.getSelection()) {
					DCBF_IDFlag = 1;
				} else {
					DCBF_IDFlag = 0;
				}
				
				if (DCBF_LFButton.getSelection()) {
					DCBF_LFFlag = 1;
				} else {
					DCBF_LFFlag = 0;
				}
				
				if (DCBF_SFButton.getSelection()) {
					DCBF_SFFlag = 1;
				} else {
					DCBF_SFFlag = 0;
				}
				
				if (DCMG_IDButton.getSelection()) {
					DCMG_IDFlag = 1;
				} else {
					DCMG_IDFlag = 0;
				}
				
				if (DCMG_LFButton.getSelection()) {
					DCMG_LFFlag = 1;
				} else {
					DCMG_LFFlag = 0;
				}
				
				if (DCMG_SFButton.getSelection()) {
					DCMG_SFFlag = 1;
				} else {
					DCMG_SFFlag = 0;
				}
				
				if (MCSWButton.getSelection()) {
					MCSWFlag = 1;
				} else {
					MCSWFlag = 0;
				}
				
				if (HEFTButton.getSelection()) {
					HEFTFlag = 1;
				} else {
					HEFTFlag = 0;
				}
				
				

				result = SWT.OK;

				shell.close();
			}
		});

		btnNewButton.setSelection(true);
		btnNewButton.setBounds(69, 700, 80, 27);
		btnNewButton.setText("OK");

		//取消按钮
		Button btnNewButton_1 = new Button(shell, SWT.NONE);
		btnNewButton_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				result = SWT.CANCEL;
				shell.close();
			}
		});
		btnNewButton_1.setBounds(244, 700, 80, 27);
		btnNewButton_1.setText("Cancel");

	}

}
