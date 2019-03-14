package org.generate.model;

/**
 * 
* @ClassName: TaskNode 
* @Description: ��������
* @author YWJ
* @date 2017-9-9 ����3:06:48
 */
public class TaskNode {
	public String nodeId;//����ڵ���
	public int taskLength;//����ĳ���
	public int startTime;//����Ŀ�ʼʱ��
	public int endTime;//����Ľ���ʱ��
	
	public TaskNode(String nodeId, int taskLength, int startTime,int endTime) {
		this.nodeId = nodeId;
		this.taskLength = taskLength;
		this.startTime = startTime;
		this.endTime = endTime;
	}
	   
	/**
	 * @return ���ض�Ӧ��������id
	 */
	public int getProcessorId(){
		String[] processorId = nodeId.split("_");
		//ÿ��DAGͼ��ͷ��β���ı�Ƕ�Ϊ0
		if(!processorId[0].equals("root")&&!processorId[0].equals("foot"))
		  return Integer.parseInt(processorId[0]);	
		else
		  return 0;
	}
}

