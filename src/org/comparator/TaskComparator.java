package org.comparator;

import java.util.Comparator;

import org.schedule.model.Task;

public class TaskComparator implements Comparator {
	public int compare(Object arg0,Object arg1){
		Task task1 = (Task)arg0;
		Task task2 = (Task)arg1;
		if((task1.getUpRankValue() - task2.getUpRankValue()) > 0)
			return -1;
		else if((task1.getUpRankValue() - task2.getUpRankValue()) < 0)
			return 1;
		else return 0;
		
	}

}