package org.generate.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.generate.model.DagEdge;
import org.generate.model.RandomDag;
import org.generate.model.TaskNode;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * 
 * @ClassName: XMLDag
 * @Description: 将DAG图转换为XML格式
 * @author YWJ
 * @date 2017-9-9 下午3:08:55
 */
public class XMLOutputUtil {

	private String filePathxml;
	private String fileName;
	
	//private String basePath = System.getProperty("user.dir") + "\\DAG_XML\\";
	private File file;
	private FileWriter fileWriter;
	private List<String> nodeIdList;

	// ===============
	private int caseCount = 0;

	private String pathXML ;

	public XMLOutputUtil( String pathXML) {
		super();
		//this.caseCount = caseCount;
		this.pathXML = pathXML;
		File file=new File(pathXML);
		if(!file.exists()){//如果文件夹不存在
			file.mkdir();//创建文件夹
		}
	}



	/**
	 * 清空DAX路径下所有文件
	 */
	public void clearDir() {
		file = new File(pathXML);
		String[] fileNames = file.list();
		if (fileNames != null) {
			File tmp;
			for (int i = 0; i < fileNames.length; i++) {
				tmp = new File(pathXML + fileNames[i]);
				tmp.delete();
			}
		}
	}

	/**
	 * 将DAG写入XML文件
	 * 
	 * @param dag
	 *            DAG文件
	 */
	public void writeDataToXML(RandomDag dag) {
		try {

			fileName = dag.dagId + ".xml";
			filePathxml = pathXML + dag.dagId + ".xml";
			String name = "DAG" + dag.dagId;
			int childcount = dag.taskList.size() - 1;

			nodeIdList = new ArrayList<String>();
			for (TaskNode node : dag.taskList) {
				nodeIdList.add(node.nodeId);
			}

			Element root = new Element("adag");
			Document doc = new Document(root);
			// root.setAttribute("xmlns", "http://pegasus.isi.edu/schema/DAX");
			// root.setAttribute("xmlns:xsi",
			// "http://www.w3.org/2001/XMLSchema-instance");
			// root.setAttribute("xsi:schemaLocation",
			// "http://pegasus.isi.edu/schema/DAX http://pegasus.isi.edu/schema/dax-2.1.xsd");
			root.setAttribute("version", "2.1");
			root.setAttribute("count", "1");
			root.setAttribute("index", "0");
			root.setAttribute("name", name);
			root.setAttribute("jobCount", dag.taskList.size() + "");
			root.setAttribute("fileCount", "0");
			root.setAttribute("childCount", childcount + "");

			for (TaskNode node : dag.taskList) {
				Element job = new Element("job");
				job.setAttribute("id", nodeIdList.indexOf(node.nodeId) + "");
				job.setAttribute("nammespace", "DAG");
				job.setAttribute("name", nodeIdList.indexOf(node.nodeId) + "");
				job.setAttribute("version", "1.0");
				job.setAttribute("tasklength", node.taskLength + "");

				for (DagEdge dagEdge : dag.edgeList) {
					if (dagEdge.head.nodeId.equals(node.nodeId)) {
						String filename = nodeIdList
								.indexOf(dagEdge.head.nodeId)
								+ "_"
								+ nodeIdList.indexOf(dagEdge.tail.nodeId);
						Element use = new Element("uses");
						use.setAttribute("file", filename);
						use.setAttribute("link", "output");
						use.setAttribute("register", "false");
						use.setAttribute("transfer", "false");
						use.setAttribute("optional", "false");
						use.setAttribute("type", "data");
						use.setAttribute("size", dagEdge.transferData + "");
						job.addContent(use);
					} else if (dagEdge.tail.nodeId.equals(node.nodeId)) {
						String filename = nodeIdList
								.indexOf(dagEdge.head.nodeId)
								+ "_"
								+ nodeIdList.indexOf(dagEdge.tail.nodeId);
						Element use = new Element("uses");
						use.setAttribute("file", filename);
						use.setAttribute("link", "input");
						use.setAttribute("register", "false");
						use.setAttribute("transfer", "false");
						use.setAttribute("optional", "false");
						use.setAttribute("type", "data");
						use.setAttribute("size", dagEdge.transferData + "");
						job.addContent(use);
					}
				}
				root.addContent(job);
			}

			for (TaskNode node : dag.taskList) {
				String[] processorId = node.nodeId.split("_");
				if (nodeIdList.indexOf(node.nodeId) > 0) {
					Element child = new Element("child").setAttribute("ref",
							nodeIdList.indexOf(node.nodeId) + "");
					for (DagEdge dagEdge : dag.edgeList) {
						// System.out.println(dagEdge.tail.nodeId+" "+node.nodeId);
						if (dagEdge.tail.nodeId.equals(node.nodeId)) {
							child.addContent(new Element("parent").setAttribute(
									"ref",
									nodeIdList.indexOf(dagEdge.head.nodeId)
											+ ""));
						}
					}

					root.addContent(child);
				}

			}

			Format format = Format.getPrettyFormat();
			XMLOutputter out = new XMLOutputter(format);
			// 输出XML文件
			out.output(doc, new FileOutputStream(filePathxml));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
