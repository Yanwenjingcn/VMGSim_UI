package org.schedule.model;

import java.util.List;

public class PE {
	public int ability;
	
	public int ID;
	
	public int task;
	
	boolean free;
	
	public int busytime;
	
	private double avail;

	private static List<double[]>availPeriod;

	private double[] ast;
	
	private double[] aft;
	
	public PE(){
		ast=new double[100];
		aft=new double[100];
	}
	
	public void setID(int id){
		this.ID = id;
	}
	
	public int getID(){
		return ID;
	}
	
	public void setbusytime(int busytime_){
		this.busytime = busytime_;
	}
	
	public int getbusytime(){
		return busytime;
	}
	
	public void setability(int temp){
		this.ability = temp;
	}
	
	public int getability(){
		return ability;
	}
	
	public void setfree(boolean temp){
		this.free = temp;
	}
	
	public boolean getfree(){
		return free;
	}
	
	public void settask(int task){
		this.task = task;
	}
	
	public int gettask(){
		return task;
	}
	
	public void setAvail(double avail){
		this.avail = avail;
	}
	
	public double getAvail(){
		return avail;
	}
	
	public void setast(int num , double ast){
		this.ast[num] = ast;
	}
	
	public double getast(int num){
		return ast[num];
	}

	public void setaft(int num , double aft){
		this.aft[num] = aft;
	}
	
	public double getaft(int num){
		return aft[num];
	}
}
