package org.schedule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.generate.util.CommonParametersUtil;
import org.schedule.model.Task;
import org.schedule.model.DAGdepend;
import org.schedule.model.PE;

/**
 * 
* @ClassName: EFFF 
* @Description: �������ʱ�����ȵ����㷨
* @author YWJ
* @date 2017-9-10 ����10:46:53
 */
public class EFTF {

	public static int tasknum;
	int dagnummax = 10000;
	int mesnum = 5;
	public static int[][] message;
	
	public static ArrayList<Task> dag_queue;
	public static ArrayList<Task> dag_queue_ori;
	public static ArrayList<Task> readyqueue;
	public static int course_number;

	public static int current_time;
	public static int T = 1;
	
	public static ArrayList<PE> pe_list;
	
	public static DAGdepend dagdepend;
	
	public static int[][] petimelist;
	public static int[] petimes;
	
	public static int pe_number;
	public static int proceesorEndTime = CommonParametersUtil.timeWindow;//ʱ�䴰
	public static int timeWindow;
	
	public EFTF(int PEnumber){
		dagdepend = new DAGdepend();
		dag_queue = new ArrayList<Task>();
		dag_queue_ori = new ArrayList<Task>();
		readyqueue = new ArrayList<Task>();
		course_number = 0;
		current_time = 0;
		petimelist = new int[PEnumber][2000];
		petimes = new int[PEnumber];
		timeWindow = proceesorEndTime/PEnumber;
		message = new int[dagnummax][mesnum];
	}
	
    /**
     * �ж�ĳһ�������Ƿ�ﵽ����״̬
     *
     * @param dag��Ҫ�жϵ�������dag
     * @param dagdepand�������������ڵ�DAG���������������ϵ
     * @param current����ǰʱ��
     * @return isready����������dag�Ƿ���Լ���readyList
     */
	private static boolean checkready(Task dag,ArrayList<Task> queue1,DAGdepend dagdepend,int current){
		
		boolean isready = true;
		
		if(dag.getpass() == false && dag.getdone() == false)
		{
			if(current >= dag.getdeadline())
			{
				dag.setpass(true);
			}
			if(dag.getstart()==0 && dag.getpass() == false)
			{
				ArrayList<Task> pre_queue = new ArrayList<Task>();
				ArrayList<Integer> pre = new ArrayList<Integer>(); 
				pre = dag.getpre();
				if(pre.size()>=0)
				{
					for(int j = 0;j<pre.size();j++)
					{
						Task buf3 = new Task();
						buf3 = getDAGById(pre.get(j));
						pre_queue.add(buf3);
					
						if(buf3.getpass())
						{
							dag.setpass(true);
							isready = false;
							break;
						}
					
						if(!buf3.done)
						{
							isready = false;
							break;
						}
						
					}
				}

			}
		}
		
		return isready;
	}
	
    /**
     * ���㱾�㷨��makespan
     *
     * @param PEnumber����������Դ����
     * @return temp�����ڼ��㴦������Դ�����ʺ����������
     */
	public static int[] makespan(int PEnumber) throws Throwable{

		tasknum = dag_queue_ori.size();
		
		pe_number = PEnumber;
		for(Task dag_:dag_queue_ori)
		{
			Task dag_new = new Task();
			dag_new.setarrive(dag_.getarrive());
			dag_new.setdeadline(dag_.getdeadline());
			dag_new.setid(dag_.getid());
			dag_new.setlength(dag_.getlength());
			dag_new.setpre(dag_.getpre());
			dag_new.setsuc(dag_.getsuc());
			dag_new.setislast(dag_.getislast());
			dag_new.setdagid(dag_.getdagid());
			dag_queue.add(dag_new);
		}
		sort(dag_queue,course_number);
		
		for(int i = 0;i<pe_list.size();i++)
		{
			if(petimes[i]==0)
				petimelist[i][0] = 0;
		}
		
		while(current_time <= timeWindow)
		{
			for(Task dag:dag_queue)
			{
				if((dag.getstart()+dag.getts()) == current_time && dag.getready() && dag.getdone() == false && dag.getpass() == false)
				{
					dag.setfinish(current_time);
					dag.setdone(true);
					pe_list.get(dag.getpeid()).setfree(true);
				}
			}
			
			for(Task dag:dag_queue)
			{
				if(dag.getarrive() <= current_time && dag.getdone() == false && dag.getready() == false && dag.getpass() == false)
				{
					boolean ifready = checkready(dag,dag_queue,dagdepend,current_time);
					if(ifready)
					{
						dag.setready(true);
						readyqueue.add(dag);

					}
				}

			}
			
			schedule(dag_queue,dagdepend,current_time);
			
			for(Task dag:dag_queue)
			{
				
				if(dag.getstart() == current_time && dag.getready() && dag.getdone() == false && dag.getpass() == false)
				{
					if(dag.getdeadline() > current_time)
					{
						if(dag.getts() == 0)
						{
							dag.setfinish(current_time);
							dag.setdone(true);
							current_time = current_time -T;
						}
						else
						{
							pe_list.get(dag.getpeid()).setfree(false);
							pe_list.get(dag.getpeid()).settask(dag.getid());
						}
					}	
					else
					{
						dag.setpass(true);
					}
				}
				
			}
			current_time = current_time + T;
		}

		int temp[] = new int[pe_number+3];
		temp = storeresult();

		return temp;
		
	}
	
	/**
     * ���汾�㷨�ĸ�������Ŀ�ʼ����ʱ��
     *
     * @return temp�����ڼ��㴦������Դ�����ʺ����������
     */
	public static int[] storeresult()
	{
		int temp[] = new int[pe_number+3];
		int tempp = 0;
		temp[0] = current_time-T;
		temp[pe_number+2] = 0;

		for(Task dag_temp:dag_queue)
		{
			for(int q = 1;q<1+pe_number;q++)
			{
				if(dag_temp.getpeid() == (q-1) && dag_temp.getdone())
				{
					temp[q] = temp[q]+dag_temp.getts();
					break;
				}
			}
		}
		
		for(Task dag:dag_queue)
		{
			if(dag.islast == true)
			{
				if(dag.done == true)
				{
					temp[pe_number+1]++;
					
					for(Task dag_temp:dag_queue)
					{
						if(dag_temp.getdagid() == dag.getdagid())
							temp[pe_number+2] = temp[pe_number+2] + dag_temp.getts();
					}
				}
			}
		}
		
		int dagcount = 0;
		for(Task dag:dag_queue)
		{
			if (dag.done) {
				message[dagcount][0] = dag.getdagid();
				message[dagcount][1] = dag.getid();
				message[dagcount][2] = dag.getpeid();
				message[dagcount][3] = dag.getstart();
				message[dagcount][4] = dag.getfinish();
				dagcount++;
			}
			
		}

		return temp;
	}
	
	/**
     * ����readyList
     *
     * @param queue1��readyList
     * @param dagdepand�������������ڵ�DAG���������������ϵ
     * @param current����ǰʱ��
     */				
	private static void schedule(ArrayList<Task> queue1,DAGdepend dagdepend,int current){
		
		for(int k = 0 ; k < readyqueue.size() ; k++)
		{
			readyqueue.get(k).setchoosePEpre(choosePE_pre(readyqueue.get(k)));
		}
		
		ArrayList<Task> buff = new ArrayList<Task>();
		Task min = new Task();
		Task temp = new Task();
		for(int k = 0 ; k < readyqueue.size() ; k++)
		{
			int tag = k;
			min = readyqueue.get(k);
			temp = readyqueue.get(k);
			for(int p = k+1 ; p < readyqueue.size() ; p++ )
			{
				if(readyqueue.get(p).getchoosePEpre() < min.getchoosePEpre() )
				{
					min = readyqueue.get(p);
					tag = p;
				}
			}
			if(tag != k)
			{
				readyqueue.set(k, min);
				readyqueue.set(tag, temp);
			}
		}
		
		for(int i = 0;i<readyqueue.size();i++)
		{
			Task buf1 = new Task();
			buf1 = readyqueue.get(i);
		
			//System.out.println(buf1.getid()+" "+buf1.getchoosePEpre()+" "+buf1.getlength());
			
			for(Task dag:dag_queue)
			{
				if(buf1.getid() == dag.getid())
				{
					choosePE(dag);
					break;
				}
			}

		}
		
		readyqueue.clear();
	}

	/**
     * Ϊ������ѡ��������ѡ��������翪ʼ�����PE
     *
     * @param dag_temp��Ҫѡ��������������
     */
	private static void choosePE(Task dag_temp){
		
		ArrayList<Task> pre_queue = new ArrayList<Task>();
		ArrayList<Integer> pre = new ArrayList<Integer>(); 
		pre = dag_temp.getpre();
		if(pre.size()>=0)
		{
			for(int j = 0;j<pre.size();j++)
			{
				Task buf = new Task();
				buf = getDAGById(pre.get(j));
				pre_queue.add(buf);
			}
		}
		
		int temp[] = new int[pe_list.size()];
		for(int i=0;i<pe_list.size();i++)
		{
			if(pre_queue.size() == 0)
			{
				if(current_time>petimelist[i][petimes[i]])
					temp[i] = current_time;
				else
					temp[i] = petimelist[i][petimes[i]];
			}
			else if(pre_queue.size() == 1)
			{
				if(pre_queue.get(0).getpeid() == pe_list.get(i).getID())
				{
					if(current_time>petimelist[i][petimes[i]])
						temp[i] = current_time;
					else
						temp[i] = petimelist[i][petimes[i]];
				}
				else
				{
					int value = (int)dagdepend.getDependValue(pre_queue.get(0).getid(),dag_temp.getid());
					if((pre_queue.get(0).getfinish()+value) > petimelist[i][petimes[i]] && (pre_queue.get(0).getfinish()+value) > current_time)
						temp[i] = pre_queue.get(0).getfinish() + value;
					else if(current_time>(pre_queue.get(0).getfinish()+value) && current_time>petimelist[i][petimes[i]])
						temp[i] = current_time;
					else
						temp[i] = petimelist[i][petimes[i]];
				}
			}
			else
			{
				int max = current_time;
				for(int j=0;j<pre_queue.size();j++){
					if(pre_queue.get(j).getpeid() == pe_list.get(i).getID())
					{
						if(max < petimelist[i][petimes[i]])
							max = petimelist[i][petimes[i]];
					}
					else
					{
						int value = pre_queue.get(j).getfinish()+(int)dagdepend.getDependValue(pre_queue.get(j).getid(),dag_temp.getid());
						if(value <= petimelist[i][petimes[i]])
						{
							if(max <petimelist[i][petimes[i]])
								max = petimelist[i][petimes[i]];
						}
						else
						{
							if(max < value)
								max = value;
						}
					}
				}
				temp[i] = max;
			}
		}		
		
		int min = 300000;
		int minpeid = 0;
		for(int i=0;i<pe_list.size();i++){
			if(min > temp[i])
				{
					min = temp[i];
					minpeid = i;
				}
		}

		if(min < dag_temp.getdeadline())
		{
			dag_temp.setpeid(minpeid);
			dag_temp.setstart(min);
			dag_temp.setts(dag_temp.getlength());
			dag_temp.setfinish_suppose(dag_temp.getstart()+dag_temp.getts());
			petimes[minpeid]++;
			petimelist[minpeid][petimes[minpeid]] = dag_temp.getfinish_suppose();
			
		}
		else
		{
			dag_temp.setpass(true);
		}

	}
	
	private static int choosePE_pre(Task dag_temp){
		
		ArrayList<Task> pre_queue = new ArrayList<Task>();
		ArrayList<Integer> pre = new ArrayList<Integer>(); 
		pre = dag_temp.getpre();
		if(pre.size()>=0)
		{
			for(int j = 0;j<pre.size();j++)
			{
				Task buf = new Task();
				buf = getDAGById(pre.get(j));
				pre_queue.add(buf);
			}
		}
		
		int temp[] = new int[pe_list.size()];
		for(int i=0;i<pe_list.size();i++)
		{
			if(pre_queue.size() == 0)
			{
				if(current_time>petimelist[i][petimes[i]])
					temp[i] = current_time;
				else
					temp[i] = petimelist[i][petimes[i]];
			}
			else if(pre_queue.size() == 1)
			{
				if(pre_queue.get(0).getpeid() == pe_list.get(i).getID())
				{
					if(current_time>petimelist[i][petimes[i]])
						temp[i] = current_time;
					else
						temp[i] = petimelist[i][petimes[i]];
				}
				else
				{
					int value = (int)dagdepend.getDependValue(pre_queue.get(0).getid(),dag_temp.getid());
					if((pre_queue.get(0).getfinish()+value) > petimelist[i][petimes[i]] && (pre_queue.get(0).getfinish()+value) > current_time)
						temp[i] = pre_queue.get(0).getfinish() + value;
					else if(current_time>(pre_queue.get(0).getfinish()+value) && current_time>petimelist[i][petimes[i]])
						temp[i] = current_time;
					else
						temp[i] = petimelist[i][petimes[i]];
				}
			}
			else
			{
				int max = current_time;
				for(int j=0;j<pre_queue.size();j++){
					if(pre_queue.get(j).getpeid() == pe_list.get(i).getID())
					{
						if(max < petimelist[i][petimes[i]])
							max = petimelist[i][petimes[i]];
					}
					else
					{
						int value = pre_queue.get(j).getfinish()+(int)dagdepend.getDependValue(pre_queue.get(j).getid(),dag_temp.getid());
						if(value <= petimelist[i][petimes[i]])
						{
							if(max <petimelist[i][petimes[i]])
								max = petimelist[i][petimes[i]];
						}
						else
						{
							if(max < value)
								max = value;
						}
					}
				}
				temp[i] = max;
			}
		}		
		
		int min = 300000;
		int minpeid = 0;
		for(int i=0;i<pe_list.size();i++){
			if(min > temp[i])
				{
					min = temp[i];
					minpeid = i;
				}
		}
		
		return min+dag_temp.getlength();
	}
	
	private static void sort(ArrayList<Task> ready_queue,int course_num) throws Throwable{
		ArrayList<Task> buff = new ArrayList<Task>();
		Task min = new Task();
		Task temp = new Task();
		for(int i = 0 ; i<course_num ; i++)
		{
			int tag = i;
			min = ready_queue.get(i);
			temp = ready_queue.get(i);
			for(int j = i+1 ; j<course_num ; j++ )
			{
				if(ready_queue.get(j).getarrive() < min.getarrive() )
				{
					min = ready_queue.get(j);
					tag = j;
				}
			}
			if(tag != i)
			{
				ready_queue.set(i, min);
				ready_queue.set(tag, temp);
			}
		}
	}
	
	private static Task getDAGById(int dagId){
		for(Task dag:dag_queue){
			if(dag.getid() == dagId)
				return dag;
		}
		return null;
	}

}
