package org.generate.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.generate.model.DagEdge;
import org.generate.model.RandomDag;
import org.generate.model.TaskNode;

/**
 * 
 * @ClassName: FileDag
 * @Description: 将DAG生成结果写入到txt文件中，两份都是不一样的。
 * @author YWJ
 * @date 2017-9-9 下午3:12:56
 */
public class FileOutputUtil {
	private String filePath;
	private String pathTXT;
	private File file;
	private FileWriter fileWriter;
	private List<String> nodeIdList;

	private int caseCount = 0;

	// ==============================

	public FileOutputUtil(int caseCount,String pathTXT) {
		super();
		this.pathTXT = pathTXT;
		this.caseCount = caseCount;
	}



	// ===================================

	public void clearDir() {
		file = new File(pathTXT);
		String[] fileNames = file.list();
		if (fileNames != null) {
			File tmp;
			for (int i = 0; i < fileNames.length; i++) {
				tmp = new File(pathTXT + fileNames[i]);
				tmp.delete();
			}
		}
	}

	/**
	 * 将DAG写入TXT文件
	 * 
	 * @param dag
	 *            DAG文件
	 */
	public void writeData(RandomDag dag) {
		try {

			filePath = pathTXT + dag.dagId + ".txt";

			nodeIdList = new ArrayList<String>();
			file = new File(filePath);
			fileWriter = new FileWriter(file, true);
			// 第一行写入Dag的size 提交时间 截止时间
			fileWriter.write(dag.taskList.size() + " " + dag.submitTime + " "
					+ dag.deadlineTime);
			fileWriter.write("\r\n");
			for (TaskNode node : dag.taskList) {
				nodeIdList.add(node.nodeId);
			}
			for (DagEdge dagEdge : dag.edgeList) {
				fileWriter.append(nodeIdList.indexOf(dagEdge.head.nodeId) + " "
						+ nodeIdList.indexOf(dagEdge.tail.nodeId) + " "
						+ dagEdge.transferData);
				fileWriter.append("\r\n");
			}

			fileWriter.flush();
			fileWriter.close();

			String path = "DAG_TXT/" + dag.dagId + "_.txt";
			PrintStream out = System.out;
			PrintStream ps = new PrintStream(new FileOutputStream(path));
			System.setOut(ps); // 重定向输出流
			int num = 0;

			for (TaskNode node : dag.taskList) {
				System.out.println(num + " " + (node.taskLength));
				num++;
			}

			ps.close();
			System.setOut(out);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
