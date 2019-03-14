package org.generate.model;

import java.util.ArrayList;
import java.util.List;

import org.generate.util.RandomParametersUtil;

/**
 * 
* @ClassName: Processor 
* @Description: ��������
* @author YWJ
* @date 2017-9-9 ����3:13:43
 */
public class Processor {
    public String processorId;//�������ı��
    public static int capacity;//�������Ĵ�������    
    public int startWorkTime;//��������ʼʱ��
    public int endWorkTime;//��������������ʱ��
    public List<TaskNode> nodeList;//����������������ڵ�
	
    public Processor(int id,int endTime) {
		processorId = "processor"+id;
		capacity = 1;		//���Ǹ����������ô�������ִ������Ŀ�����
		startWorkTime = 0;
		endWorkTime = endTime;
		nodeList = new ArrayList<TaskNode>();
		RandomParametersUtil randomCreater = new RandomParametersUtil();
		
		//����������ʱ��������������ϵĽڵ���Ϣ
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
