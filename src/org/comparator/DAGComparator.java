package org.comparator;
import java.util.Comparator;

import org.schedule.model.Task;

public class DAGComparator implements Comparator {
	public int compare(Object arg0,Object arg1){
		Task cloudlet1 = (Task)arg0;
		Task cloudlet2 = (Task)arg1;
		if((cloudlet1.getUpRankValue() - cloudlet2.getUpRankValue()) > 0)
			return -1;
		else if((cloudlet1.getUpRankValue() - cloudlet2.getUpRankValue()) < 0)
			return 1;
		else return 0;
		
	}

}