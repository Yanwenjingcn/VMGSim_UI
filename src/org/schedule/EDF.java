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
* @Description: 最早截止时间优先调度算法
* @author YWJ
* @date 2017-9-10 下午10:46:13
 */
public class EDF {
	
	public static int tasknum; //任务数，也就是DAG数
	int dagnummax = 10000;
	int mesnum = 5;
	public static int[][] message;	//整个过程调度执行完成后，每个DAG的汇总信息
	
	public static ArrayList<Task> dag_queue;	//按照截止时间从早到晚排序后的DAG列表
	public static ArrayList<Task> dag_queue_ori;	//原始DAG列表
	public static ArrayList<Task> readyqueue;	//DAG的就绪队列
	public static int course_number;	

	public static int current_time;
	public static int T = 1;	//滑动的时间窗大小
	
	public static ArrayList<PE> pe_list;	//处理器列表
	
	public static DAGdepend dagdepend;	//各个DAG之间的依赖关系
	
	public static int[][] petimelist;	///某个处理器第几次处理完成的时间【第几个处理器】【第几次处理结束】=结束时间
	public static int[] petimes;	//某处理器执行完成DAG的次数
	
	public static int pe_number ;	//处理器个数
	public static int proceesorEndTime = CommonParametersUtil.timeWindow;//时间窗
	public static int timeWindow;	//每个处理器个执行的时间长度
	
	public EDF(int PEnumber){
		dagdepend = new DAGdepend();
		dag_queue = new ArrayList<Task>();
		dag_queue_ori = new ArrayList<Task>();
		readyqueue = new ArrayList<Task>();//就绪队列
		course_number = 0;
		current_time = 0;
		petimelist = new int[PEnumber][2000];
		petimes = new int[PEnumber];
		timeWindow = proceesorEndTime/PEnumber;
		message = new int[dagnummax][mesnum];
	}
	
    /**
     * 判断某一子任务是否达到就绪状态
     *
     * @param dag，要判断的子任务dag
     * @param dagdepand，该子任务所在的DAG的子任务间依赖关系
     * @param current，当前时刻
     * @return isready，该子任务dag是否可以加入readyList
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
				//父DAG的队列
				ArrayList<Task> pre_queue = new ArrayList<Task>();
				ArrayList<Integer> pre = new ArrayList<Integer>(); 
				//当前DAG的父DAG的ID列表
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
					//如果当前DAG的父DAG没有完成，那么证明当前DAG不可能是准备好的DAG，这只ready标志位false并返回
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
     * 计算本算法的makespan（最大完工时间）
     *
     * @param PEnumber，处理器资源个数
     * @return temp，用于计算处理器资源利用率和任务完成率
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
			//为每个DAG找寻它的结束时间
			for(Task dag:dag_queue)
			{
				if((dag.getstart()+dag.getts()) == current_time && dag.getready() && dag.getdone() == false && dag.getpass() == false)
				{
					dag.setfinish(current_time); //设置当前时间为该DAG的结束时间
					dag.setdone(true);	//设置调度并执行完成标识
					pe_list.get(dag.getpeid()).setfree(true);
				}
			}
			
			//查询当前时刻准备好的DAG并加入DAG就绪队列
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
			
			//执行一次调度
			schedule(dag_queue,dagdepend,current_time);
			
			//遍历整个DAG队列，标记当前时间上正在忙绿的处理器
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
			
			//当前时间+滑动窗口大小
			current_time = current_time + T;
		}

		//多创建一个数组，数目比处理器个数多三，这三个位置分别存放：
		int temp[] = new int[pe_number+3];
		temp = storeresult();

		return temp;
		
	}
	
	/**
     * 保存本算法的各个任务的开始结束时间
     *
     * @return temp，用于计算处理器资源利用率和任务完成率
     */
	public static int[] storeresult()
	{
		int temp[] = new int[pe_number+3];
		int tempp = 0;
		temp[0] = current_time-T; //当前时间
		temp[pe_number+2] = 0;

		//求取每个处理器上的实际有任务执行的总时间
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
					temp[pe_number+1]++; //完成的DAG数目
					//所有DAG的实际执行时间
					for(Task dag_temp:dag_queue)
					{
						if(dag_temp.getdagid() == dag.getdagid())
							temp[pe_number+2] = temp[pe_number+2] + dag_temp.getts();
					}
				}
			}
		}
		
		//所有任务调度结束后的每个DAG基本信息汇总
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
     * 调度readyList
     *
     * @param queue1 整个DAG队列，readyList DAG的就绪队列
     * @param dagdepand，该子任务所在的DAG的子任务间依赖关系
     * @param current，当前时刻
     */
	private static void schedule(ArrayList<Task> queue1,DAGdepend dagdepend,int current){
		
		ArrayList<Task> buff = new ArrayList<Task>();
		Task min = new Task();
		Task temp = new Task();
		//将就绪队列按照截止时间从早到晚排列
		for(int k = 0 ; k < readyqueue.size() ; k++)
		{
			int tag = k;
			min = readyqueue.get(k);
			temp = readyqueue.get(k);
			for(int p = k+1 ; p < readyqueue.size() ; p++ )
			{
				//在就绪队列中寻找截止时间最早的DAG任务
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

		//为就绪队列里面的每个DAG找寻对应的处理器
		for(int i = 0;i<readyqueue.size();i++)
		{
			Task buf1 = new Task();
			buf1 = readyqueue.get(i);
			
			//为就绪队列中每个DAG选择合适的处理器执行
			for(Task dag:dag_queue)
			{
				if(buf1.getid() == dag.getid())
				{			
					choosePE(dag);
					break;
				}
			}

		}
		
		//清楚当前的就绪队列。就绪队列实时刷新
		readyqueue.clear();
			
	}

	/**
     * 为子任务选择处理器，选择可以最早开始处理的PE
     *
     * @param dag_temp，要选择处理器的子任务
     */
	private static void choosePE(Task dag_temp){
		
		ArrayList<Task> pre_queue = new ArrayList<Task>();
		ArrayList<Integer> pre = new ArrayList<Integer>(); 
		//获取父DAG的ID
		pre = dag_temp.getpre();
		
		//获取父DAG的列表
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
		//选择可以开始处理时间最早的处理器
		for(int i=0;i<pe_list.size();i++){
			if(min > temp[i])
				{
					min = temp[i];
					minpeid = i;
				}
		}
		//修改当前DAG的信息
		if(min < dag_temp.getdeadline())
		{
			dag_temp.setpeid(minpeid);
			dag_temp.setts(dag_temp.getlength()/pe_list.get(minpeid).getability());
			dag_temp.setstart(min);
			dag_temp.setfinish_suppose(dag_temp.getstart()+dag_temp.getts());//设置该DAG最有可能的结束时间
			//该处理器执行的次数+1
			petimes[minpeid]++;
			petimelist[minpeid][petimes[minpeid]] = dag_temp.getfinish_suppose();
		}
		else
		{
			dag_temp.setpass(true);	//是否跳过（为TRUE证明这个DAG没能被处理）
		}

	}
	
	/**
	 * 
	* @Title: sort 
	* @Description: 对所有DAG按照其到达的先后顺序排序   
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
			
			//寻找最先到达的DAG作为min
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
	* @Description: 通过DAG的id编号来获取DAG   
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
