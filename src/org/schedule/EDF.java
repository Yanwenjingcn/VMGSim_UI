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
* @ClassName: EDF 
* @Description: �����ֹʱ�����ȵ����㷨
* @author YWJ
* @date 2017-9-10 ����10:46:13
 */
public class EDF {
	
	public static int tasknum; //��������Ҳ����DAG��
	int dagnummax = 10000;
	int mesnum = 5;
	public static int[][] message;	//�������̵���ִ����ɺ�ÿ��DAG�Ļ�����Ϣ
	
	public static ArrayList<Task> dag_queue;	//���ս�ֹʱ����絽��������DAG�б�
	public static ArrayList<Task> dag_queue_ori;	//ԭʼDAG�б�
	public static ArrayList<Task> readyqueue;	//DAG�ľ�������
	public static int course_number;	

	public static int current_time;
	public static int T = 1;	//������ʱ�䴰��С
	
	public static ArrayList<PE> pe_list;	//�������б�
	
	public static DAGdepend dagdepend;	//����DAG֮���������ϵ
	
	public static int[][] petimelist;	///ĳ���������ڼ��δ�����ɵ�ʱ�䡾�ڼ��������������ڼ��δ��������=����ʱ��
	public static int[] petimes;	//ĳ������ִ�����DAG�Ĵ���
	
	public static int pe_number ;	//����������
	public static int proceesorEndTime = CommonParametersUtil.timeWindow;//ʱ�䴰
	public static int timeWindow;	//ÿ����������ִ�е�ʱ�䳤��
	
	public EDF(int PEnumber){
		dagdepend = new DAGdepend();
		dag_queue = new ArrayList<Task>();
		dag_queue_ori = new ArrayList<Task>();
		readyqueue = new ArrayList<Task>();//��������
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
				//System.out.println(current_time+" "+queue1.get(i).Deadline);
			}
			if(dag.getstart()==0 && dag.getpass() == false)
			{
				//��DAG�Ķ���
				ArrayList<Task> pre_queue = new ArrayList<Task>();
				ArrayList<Integer> pre = new ArrayList<Integer>(); 
				//��ǰDAG�ĸ�DAG��ID�б�
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
							break;
						}
					//�����ǰDAG�ĸ�DAGû����ɣ���ô֤����ǰDAG��������׼���õ�DAG����ֻready��־λfalse������
						if(!buf3.done)
						{
							isready = false;
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
     * ���㱾�㷨��makespan������깤ʱ�䣩
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
		
		//
		for(int i = 0;i<pe_list.size();i++)
		{
			if(petimes[i]==0)
				petimelist[i][0] = 0;
		}
		
		while(current_time <= timeWindow)
		{
			//Ϊÿ��DAG��Ѱ���Ľ���ʱ��
			for(Task dag:dag_queue)
			{
				if((dag.getstart()+dag.getts()) == current_time && dag.getready() && dag.getdone() == false && dag.getpass() == false)
				{
					dag.setfinish(current_time); //���õ�ǰʱ��Ϊ��DAG�Ľ���ʱ��
					dag.setdone(true);	//���õ��Ȳ�ִ����ɱ�ʶ
					pe_list.get(dag.getpeid()).setfree(true);
				}
			}
			
			//��ѯ��ǰʱ��׼���õ�DAG������DAG��������
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
			
			//ִ��һ�ε���
			schedule(dag_queue,dagdepend,current_time);
			
			//��������DAG���У���ǵ�ǰʱ��������æ�̵Ĵ�����
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
			
			//��ǰʱ��+�������ڴ�С
			current_time = current_time + T;
		}

		//�ഴ��һ�����飬��Ŀ�ȴ���������������������λ�÷ֱ��ţ�
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
		temp[0] = current_time-T; //��ǰʱ��
		temp[pe_number+2] = 0;

		//��ȡÿ���������ϵ�ʵ��������ִ�е���ʱ��
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
					temp[pe_number+1]++; //��ɵ�DAG��Ŀ
					//����DAG��ʵ��ִ��ʱ��
					for(Task dag_temp:dag_queue)
					{
						if(dag_temp.getdagid() == dag.getdagid())
							temp[pe_number+2] = temp[pe_number+2] + dag_temp.getts();
					}
				}
			}
		}
		
		//����������Ƚ������ÿ��DAG������Ϣ����
		int dagcount = 0;
		for(Task dag:dag_queue)
		{
			
			if(dag.done){
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
     * @param queue1 ����DAG���У�readyList DAG�ľ�������
     * @param dagdepand�������������ڵ�DAG���������������ϵ
     * @param current����ǰʱ��
     */
	private static void schedule(ArrayList<Task> queue1,DAGdepend dagdepend,int current){
		
		ArrayList<Task> buff = new ArrayList<Task>();
		Task min = new Task();
		Task temp = new Task();
		//���������а��ս�ֹʱ����絽������
		for(int k = 0 ; k < readyqueue.size() ; k++)
		{
			int tag = k;
			min = readyqueue.get(k);
			temp = readyqueue.get(k);
			for(int p = k+1 ; p < readyqueue.size() ; p++ )
			{
				//�ھ���������Ѱ�ҽ�ֹʱ�������DAG����
				if(readyqueue.get(p).getdeadline() < min.getdeadline() )
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

		//Ϊ�������������ÿ��DAG��Ѱ��Ӧ�Ĵ�����
		for(int i = 0;i<readyqueue.size();i++)
		{
			Task buf1 = new Task();
			buf1 = readyqueue.get(i);
			
			//Ϊ����������ÿ��DAGѡ����ʵĴ�����ִ��
			for(Task dag:dag_queue)
			{
				if(buf1.getid() == dag.getid())
				{			
					choosePE(dag);
					break;
				}
			}

		}
		
		//�����ǰ�ľ������С���������ʵʱˢ��
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
		//��ȡ��DAG��ID
		pre = dag_temp.getpre();
		
		//��ȡ��DAG���б�
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
		//ѡ����Կ�ʼ����ʱ������Ĵ�����
		for(int i=0;i<pe_list.size();i++){
			if(min > temp[i])
				{
					min = temp[i];
					minpeid = i;
				}
		}
		//�޸ĵ�ǰDAG����Ϣ
		if(min < dag_temp.getdeadline())
		{
			dag_temp.setpeid(minpeid);
			dag_temp.setts(dag_temp.getlength()/pe_list.get(minpeid).getability());
			dag_temp.setstart(min);
			dag_temp.setfinish_suppose(dag_temp.getstart()+dag_temp.getts());//���ø�DAG���п��ܵĽ���ʱ��
			//�ô�����ִ�еĴ���+1
			petimes[minpeid]++;
			petimelist[minpeid][petimes[minpeid]] = dag_temp.getfinish_suppose();
		}
		else
		{
			dag_temp.setpass(true);	//�Ƿ�������ΪTRUE֤�����DAGû�ܱ�����
		}

	}
	
	/**
	 * 
	* @Title: sort 
	* @Description: ������DAG�����䵽����Ⱥ�˳������   
	* @return void    
	* @throws
	 */
	private static void sort(ArrayList<Task> ready_queue,int course_num) throws Throwable{
		ArrayList<Task> buff = new ArrayList<Task>();
		Task min = new Task();
		Task temp = new Task();
		for(int i = 0 ; i<course_num ; i++)
		{
			int tag = i;
			min = ready_queue.get(i);
			temp = ready_queue.get(i);
			
			//Ѱ�����ȵ����DAG��Ϊmin
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
	
	/**
	 * 
	* @Title: getDAGById 
	* @Description: ͨ��DAG��id�������ȡDAG   
	* @return DAG    
	* @throws
	 */
	private static Task getDAGById(int dagId){
		for(Task dag:dag_queue){
			if(dag.getid() == dagId)
				return dag;
		}
		return null;
	}

}
