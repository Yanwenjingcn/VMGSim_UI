package org.schedule.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * 
* @ClassName: DAGdepend 
* @Description: 各个DAG之间的依赖关系，以DAG为基本调度单位
* @author YWJ
* @date 2017-9-10 下午8:19:48
 */
public class DAGdepend { //一个工作流中的TASK间依赖关系，以下的DAG均为工作流中的子任务TASK

	private List<Task> DAGList;	//DAG列表
	
	private Map<Integer,Integer> DAGDependMap;
	
	private Map<String,Double> DAGDependValueMap;

	public ArrayList<DAG> DAGMapList;
	
	
	

	public void setdagmaplist(ArrayList<DAG> list){
		this.DAGMapList = list;
	}
	
	public ArrayList getdagmaplist(){
		return DAGMapList;
	}
	
	/**
	 * 
	* @Title: isDepend 
	* @Description: 两者是否存在依赖   
	* @return boolean    
	* @throws
	 */
	public boolean isDepend(String src,String des){
		if(DAGDependValueMap.containsKey(src+" "+des)){
			return true;
		}
		else return false;
	}
	
	/**
	 * 
	* @Title: getDependValue 
	* @Description: 得到依赖值   
	* @return double    
	* @throws
	 */
	public double getDependValue(int src,int des){
		return DAGDependValueMap.get(String.valueOf(src)+" "+String.valueOf(des));
	}
	

	public void setDAGList(List cl){
		this.DAGList = cl;
	}

	public List getDAGList(){
		return DAGList;		
	}
	

	public void setDAGDependMap(Map cd){
		this.DAGDependMap = cd;
	}

	public Map getDAGDependMap(){
		return DAGDependMap;
	}
	

	public void setDAGDependValueMap(Map cdv){
		this.DAGDependValueMap = cdv;
	}

	public Map getDAGDependValueMap(){
		return DAGDependValueMap;
	}

}
