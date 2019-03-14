package org.generate.model;

import org.generate.DagBuilder;

/**
 * 
* @ClassName: DagEdge
* @Description: DAG依赖边的对象
* @author YanWenjing
* @date 2017-10-20 下午12:52:52
 */
public class DagEdge {
	public TaskNode head;
	public TaskNode tail;
	public int transferData;
	
	public DagEdge(TaskNode head, TaskNode tail) {
		this.head = head;
		this.tail = tail;
		
		/**
		 * 确定节点间边传输数据量
		 * 若是在一个处理器上
		 * 		为0；
		 * 若不在
		 * 		【0，自己执行开始时间-父节点执行结束时间】的随机值；
		 */
		if(head.getProcessorId() == tail.getProcessorId()) //如果是同一台机器
			this.transferData = DagBuilder.randomCreater.randomTranferData((head.taskLength+tail.taskLength)/2);
		else //如果不是一个机器数据量为边差值的随机值
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
	 * 构造方法，已传入相关的传输边长
	 */
	public DagEdge(TaskNode head, TaskNode tail , int transferData) {
		this.head = head;
		this.tail = tail;
		this.transferData = 0;	
	}
	
	/**
	 * 
	* @Title: printEdge 
	* @Description: 打印边信息   
	* @return void    
	* @throws
	 */
	public void printEdge(){
		System.out.print(head.nodeId+"——>"+tail.nodeId+" ");
	}
}
