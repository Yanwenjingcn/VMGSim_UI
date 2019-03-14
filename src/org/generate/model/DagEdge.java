package org.generate.model;

import org.generate.DagBuilder;

/**
 * 
* @ClassName: DagEdge
* @Description: DAG�����ߵĶ���
* @author YanWenjing
* @date 2017-10-20 ����12:52:52
 */
public class DagEdge {
	public TaskNode head;
	public TaskNode tail;
	public int transferData;
	
	public DagEdge(TaskNode head, TaskNode tail) {
		this.head = head;
		this.tail = tail;
		
		/**
		 * ȷ���ڵ��ߴ���������
		 * ������һ����������
		 * 		Ϊ0��
		 * ������
		 * 		��0���Լ�ִ�п�ʼʱ��-���ڵ�ִ�н���ʱ�䡿�����ֵ��
		 */
		if(head.getProcessorId() == tail.getProcessorId()) //�����ͬһ̨����
			this.transferData = DagBuilder.randomCreater.randomTranferData((head.taskLength+tail.taskLength)/2);
		else //�������һ������������Ϊ�߲�ֵ�����ֵ
		{
			if(tail.startTime!=100*10000)
				this.transferData = DagBuilder.randomCreater.randomTranferData(tail.startTime - head.endTime);
			else
				this.transferData = 0;
		}
		//System.out.println(head.nodeId+" "+head.getProcessorId()+" "+tail.nodeId+" "+tail.getProcessorId()+" "+this.transferData);
	}
	
	/**
	 * 
	 * @param head
	 * @param tail
	 * @param transferData
	 * ���췽�����Ѵ�����صĴ���߳�
	 */
	public DagEdge(TaskNode head, TaskNode tail , int transferData) {
		this.head = head;
		this.tail = tail;
		this.transferData = 0;	
	}
	
	/**
	 * 
	* @Title: printEdge 
	* @Description: ��ӡ����Ϣ   
	* @return void    
	* @throws
	 */
	public void printEdge(){
		System.out.print(head.nodeId+"����>"+tail.nodeId+" ");
	}
}
