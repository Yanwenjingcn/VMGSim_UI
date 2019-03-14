package org.generate.util;

import java.util.List;

import org.generate.model.TaskNode;

public class RandomParametersUtil {
	
	public double taskLengthRate = 0.5;//���񳤶ȸ�����
//	public double bandWithRate = 0.25; //��������	
	//public double levelRate = 0.5;//ÿһ������ĸ�����
	
	
	
	/**
	 * @param id ���������
	 * @param nodeList �������ڵ��б�
	 * @param capacity ��������peak�������������ٶ�
	 * @param endTime  ���������н���ʱ��
	 * 
	 * ��ʼ��������ʱ   �������ϵ�   ��ʼ����ڵ���Ϣ
	 */
	public void randomCreateNodes(int id,List<TaskNode> nodeList,int capacity,int endTime){
		int capacityLength = (int) (capacity*endTime); //��������100%����������£��ܴ���������ܳ���
		int nodeNum = 0; //task����ڵ���
		int totalLength = 0;//���񳤶�	
		
		while(totalLength < capacityLength){
			nodeNum++;//��1��ʼ
			
			/**
			 * ��������ĳ���ȡֵ��Χ��0.5*������ƽ�����ȣ�1.5*������ƽ�����ȡ�
			 */
			int taskLength = random((int)(CommonParametersUtil.taskAverageLength*(1-taskLengthRate)), (int)(CommonParametersUtil.taskAverageLength*(1+taskLengthRate)));//�������񳤶�	
			
			TaskNode taskNode;
			if(taskLength+totalLength > capacityLength)//�����������ĳ��� �����˴����������񳤶�
				taskLength = capacityLength -totalLength;//�����������񳤶�-��ǰ�ܳ��ȣ���Ϊ���һ������ĳ���
			
			//�����ţ����������_���ұ��
			//���������������������id�����񳤶ȣ�ִ��ʱ�䣬����ʱ�䣩
			taskNode = new TaskNode(Integer.toString(id)+"_"+Integer.toString(nodeNum),taskLength,(totalLength/capacity),(taskLength+totalLength)/capacity);	
			nodeList.add(taskNode);
			totalLength+=taskLength;
		}			
	}
	
	
	
	/**
	 * @param lastDagtime ��һ��Dag�ύ��ʱ��
	 * @param startTime ��ǰDag��ʼִ�е�ʱ��
	 * @return ������ɵ�Dag���ύʱ��
	 */
	public int randomSubmitTime(int lastDagtime,int startTime){
		return random(lastDagtime, startTime);
	}
	
	
	
	/**
	 * @param maxlength ���ϵĲ�ֵ
	 * @return ������ɵ����ݴ���ʱ��
	 */
	public int randomTranferData(int maxlength)
	{
		if(maxlength == 0)
			return 0;
		else
			return random(1, maxlength);
	}
	
	
	
	/**
	 * 
	* @Title: randomDagSize 
	* @Description: ���� ƽ��DAG������ �������ĳDAGͼ��������   
	* 				��Χ��0.5*ƽ��DAG��������1.5*ƽ��DAG��������
	* @return int  �������dag���е�������Ŀ  
	* @throws
	 */
	public int randomDagSize(int dagAverageSize){
		return random((int)(dagAverageSize*0.5),(int)(dagAverageSize*1.5));
		
	}
	
	/**
	 * 
	* @Title: randomDagSizeWithSingle
	* @Description: ���뵥���ڵ�����DAG��С
	* @param @param dagAverageSize
	* @param @return
	* @return int
	* @throws
	 */
	public int randomDagSizeWithSingle(int dagAverageSize){
		/**
		 * 
		 */
		float singleFlag=randomFloat(0, 1);
		if(singleFlag<=CommonParametersUtil.getSingleDAGPercent())
			return 1;
		/**
		 * 
		 */
		return random((int)(dagAverageSize*0.5),(int)(dagAverageSize*1.5));
		
	}
	
	/**
	 * 
	* @Title: randomLevelNum 
	* @Description: ���ݴ��ж���������DAGͼ�Ĳ���   
	* @return int    �������dag���ж��ٲ�
	* @throws
	 */
	public int randomLevelNum(int dagSize,int levelFlag){
		/**
		 * ���뵥����������
		 */
		if(dagSize==1){
			return 1;
		}
		/**
		 * 
		 */
			
		int sqrt = (int)Math.sqrt(dagSize-2);
		if(levelFlag == 1)
			return random(1, sqrt);
		else if(levelFlag == 2)
			return random(sqrt,sqrt+3);
		else if(levelFlag == 3)
			return random(sqrt+3,dagSize-2);
		else
			return sqrt;		
	}
	
	
	/**
	 * 
	* @Title: randomLevelSizes 
	* @Description: ����DAGͼ�ĵڶ����������ڶ�����ÿһ���������Ŀ���������   
	* 				������ǵ���������ֱ�Ӳ������ѭ����
	* @return void    
	* @throws
	 */
	public void randomLevelSizes(int[] dagLevel,int nodeNumber){
		//�ȸ�ÿ���������Ŀ����ʼ��Ϊ1,ÿ�㶼������һ������
		for(int j = 0;j < dagLevel.length;j++)
			 dagLevel[j] = 1;
		
		int i = nodeNumber - dagLevel.length;
		
		//���Ϊÿһ������������
		 while(i > 0)
		 {
			 for(int j = 0;j < dagLevel.length;j++)
		
				  if(random(0, 1) == 1)
				  {
					  dagLevel[j]++;
					  i--;
					  if(i == 0)
						  break;
				  }	
			 if(i == 0)
				 break;
		 }
		 
	}
	
	
	/**
	 * 
	* @Title: random
	* @Description: ����[min,max]֮��������
	* @param @param min
	* @param @param max
	* @param @return
	* @return int
	* @throws
	 */
	public static int random(int min,int max){
		return (int)(min + Math.random()*(max-min+1));
	}
	
	public float randomFloat(int min,int max){
		return (float) (Math.random());
	}

}

