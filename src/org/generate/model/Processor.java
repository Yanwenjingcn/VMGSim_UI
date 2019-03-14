package org.generate.model;

import java.util.ArrayList;
import java.util.List;

import org.generate.util.RandomParametersUtil;

/**
 * 
* @ClassName: Processor 
* @Description: 处理器类
* @author YWJ
* @date 2017-9-9 下午3:13:43
 */
public class Processor {
    public String processorId;//处理器的编号
    public static int capacity;//处理器的处理能力    
    public int startWorkTime;//处理器开始时间
    public int endWorkTime;//处理器结束工作时间
    public List<TaskNode> nodeList;//处理器包含的任务节点
	
    public Processor(int id,int endTime) {
		processorId = "processor"+id;
		capacity = 1;		//就是各个任务来该处理器能执行任务的快慢。
		startWorkTime = 0;
		endWorkTime = endTime;
		nodeList = new ArrayList<TaskNode>();
		RandomParametersUtil randomCreater = new RandomParametersUtil();
		
		//处理器创建时就随机建立起其上的节点信息
		randomCreater.randomCreateNodes(id, nodeList, capacity, endWorkTime);	
	}
    
    
    
    public void printNodes(){
    	System.out.print(processorId+":");
    	for(TaskNode node:nodeList)
    	{
    		System.out.print(node.nodeId+":"+node.taskLength+" ");
    	}
    	System.out.println();
    }
    
    
}
