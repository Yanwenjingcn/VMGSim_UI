package org.schedule.model;

import java.util.ArrayList;
import java.util.HashMap;

public class DAG { //一个业务的总集合

	public boolean fillbackpass = false;
	
	public boolean fillbackdone = false;
	
	public int DAGId;
	
	public int tasknumber;
	
	public int DAGdeadline;
	
	public int submittime;
	
	public ArrayList<Task> TaskList;
	
	public HashMap<Integer,Integer> DAGDependMap;
	
	public HashMap<String,Double> DAGDependValueMap;
	
	public ArrayList<Task> orderbystarttime;
	
	public HashMap<Integer,ArrayList> taskinlevel;

	public boolean isSingle=false;
	
	public boolean isMerge=false;
	
	
	
	public boolean isMerge() {
		return isMerge;
	}

	public void setMerge(boolean isMerge) {
		this.isMerge = isMerge;
	}

	public boolean getIsSingle() {
		return isSingle;
	}

	public void setSingle(boolean isSingle) {
		this.isSingle = isSingle;
	}

	public DAG(){
		TaskList = new ArrayList<Task>();
		orderbystarttime = new ArrayList<Task>();
		DAGDependMap = new HashMap<Integer,Integer>();
		DAGDependValueMap = new HashMap<String,Double>();
		taskinlevel = new HashMap<Integer,ArrayList>();
	}
	
	public boolean isDepend(String src,String des){
		if(DAGDependValueMap.containsKey(src+" "+des)){
			return true;
		}
		else return false;
	}
	
	
	
	
	public void setfillbackpass(boolean pass){
		this.fillbackpass = pass;
	}
	
	public boolean getfillbackpass(){
		return fillbackpass;
	}
	
	public void setfillbackdone(boolean done){
		this.fillbackdone = done;
	}
	
	public boolean getfillbackdone(){
		return fillbackdone;
	}
	
	public void settasklist(ArrayList<Task> list){
		for(int i =0;i<list.size();i++)
			this.TaskList.add(list.get(i));
	}
	
	public ArrayList gettasklist(){
		return TaskList;
	}
	
	public void setorderbystarttime(ArrayList<Task> list){
		for(int i =0;i<list.size();i++)
			this.orderbystarttime.add(list.get(i));
	}
	
	public ArrayList getorderbystarttime(){
		return orderbystarttime;
	}
	

	
	public HashMap<Integer, Integer> getDAGDependMap() {
		return DAGDependMap;
	}

	public void setdepandmap(HashMap<Integer, Integer> dAGDependMap) {
		DAGDependMap = dAGDependMap;
	}

	public void setdependvalue(HashMap<String,Double> value){
		this.DAGDependValueMap = value;
	}
	
	public HashMap getdependvalue(){
		return DAGDependValueMap;
	}
	
	public void setDAGId(int id){
		this.DAGId = id;
	}
	
	public int getDAGId(){
		return DAGId;
	}
	
	public void settasknumber(int num){
		this.tasknumber = num;
	}
	
	public int gettasknumber(){
		return tasknumber;
	}
	
	public void setDAGdeadline(int deadline){
		this.DAGdeadline = deadline;
	}
	
	public int getDAGdeadline(){
		return DAGdeadline;
	}
	
	public void setsubmittime(int submit){
		this.submittime = submit;
	}
	
	public int getsubmittime(){
		return submittime;
	}
	
	
}
