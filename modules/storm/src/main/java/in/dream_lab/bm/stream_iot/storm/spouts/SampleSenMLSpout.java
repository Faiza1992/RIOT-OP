package in.dream_lab.bm.stream_iot.storm.spouts;

import in.dream_lab.bm.stream_iot.storm.genevents.EventGen;
import in.dream_lab.bm.stream_iot.storm.genevents.ISyntheticEventGen;
import in.dream_lab.bm.stream_iot.storm.genevents.logging.BatchedFileLogging;
import in.dream_lab.bm.stream_iot.storm.genevents.utils.GlobalConstants;
import in.dream_lab.bm.stream_iot.storm.genevents.logging.JRedis;
import java.io.BufferedReader;  
import java.io.FileReader;
import java.io.IOException;   
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import java.util.ArrayList;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SampleSenMLSpout extends BaseRichSpout implements ISyntheticEventGen {
	SpoutOutputCollector _collector;
	EventGen eventGen;
	BlockingQueue<List<String>> eventQueue;
	String csvFileName;
	String outSpoutCSVLogFileName;
	String experiRunId;
	double scalingFactor;
	BatchedFileLogging ba;
	JRedis jr;
	long msgId;
	String line;
	int p=1;
	String priority[];
	public SampleSenMLSpout(){
		//			this.csvFileName = "/home/ubuntu/sample100_sense.csv";
		//			System.out.println("Inside  sample spout code");
		this.csvFileName = "/home/tarun/j2ee_workspace/eventGen-anshu/eventGen/bangalore.csv";
		this.scalingFactor = GlobalConstants.accFactor;
		//			System.out.print("the output is as follows");
	}

	public SampleSenMLSpout(String csvFileName, String outSpoutCSVLogFileName, double scalingFactor, String experiRunId){
		this.csvFileName = csvFileName;
		this.outSpoutCSVLogFileName = outSpoutCSVLogFileName;
		this.scalingFactor = scalingFactor;
		this.experiRunId = experiRunId;
	}

	public SampleSenMLSpout(String csvFileName, String outSpoutCSVLogFileName, double scalingFactor){
		this(csvFileName, outSpoutCSVLogFileName, scalingFactor, "");
	}


	//public static void main(String[] args)   
	//{   
	//} 
	int p1=0;
	//Values values = new Values();
	Values[] values1;
	int[] priority1; 
	@Override
	public void nextTuple() 
	{

		int count = 0, MAX_COUNT=10; // FIXME?
		while(count < MAX_COUNT) 
		{
			List<String> entry = this.eventQueue.poll(); // nextTuple should not block!
			if(entry == null) return;
			count++;
			Values values=new Values();
			StringBuilder rowStringBuf = new StringBuilder();
			for(String s : entry){
				rowStringBuf.append(",").append(s);
			}
			String rowString = rowStringBuf.toString().substring(1);
			String newRow = rowString.substring(rowString.indexOf(",")+1);
			msgId++;
			values.add(msgId);
			values.add(newRow);
			priority1[count]=Integer.parseInt(priority[p1]);
			values1[count]=values;

		}
		for (int i = 0; i <priority1.length; i++){
			int index = i;
			for (int j = i ; j <= priority1.length-1; j++){
				if (priority1[j] > priority1[index]){
					index = j;
				}
			}
			int temp = priority1[i];
			Values temp1 = values1[i];
			priority1[i] = priority1[index];
			values1[i] = values1[index];
			priority1[index] = temp;
			values1[index] = temp1;
		}
		System.out.printf("Sorted Events "+ values1);
		System.out.printf("Sorted priorities "+ priority1);
		for (int k=1; k<values1.length; k++){
			this._collector.emit(values1[k]);
			try 
			{
				//ba.batchLogwriter(System.currentTimeMillis(),"MSGID," + msgId);
				jr.batchWriter(System.currentTimeMillis(),"MSGID_" + msgId);


			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		values1= null;
		priority1= null;
	}

	@Override
	public void open(Map map, TopologyContext context, SpoutOutputCollector collector) 
	{

		BatchedFileLogging.writeToTemp(this,this.outSpoutCSVLogFileName);
		Random r=new Random();
		try 
		{
			msgId= (long) (1*Math.pow(10,12)+(r.nextInt(1000)*Math.pow(10,9))+r.nextInt(10));
			
		} catch (Exception e) {

			e.printStackTrace();
		}
		_collector = collector;
		this.eventGen = new EventGen(this,this.scalingFactor);
		this.eventQueue = new LinkedBlockingQueue<List<String>>();
		String uLogfilename=this.outSpoutCSVLogFileName+msgId;
		this.eventGen.launch(this.csvFileName, uLogfilename, -1, true); //Launch threads

		ba=new BatchedFileLogging(uLogfilename, context.getThisComponentId());
		jr=new JRedis(this.outSpoutCSVLogFileName);
		try   
		{  
			//parsing a CSV file into BufferedReader class constructor  
			BufferedReader br = new BufferedReader(new FileReader("/home/amna/priority_sys.csv"));   
			while ((br.readLine()) != null)   //returns a Boolean value  
			{  
				line = br.readLine();
				System.out.printf("data "+ line);
				priority[p]=line;
				p++;
			} 
		}   
		catch (IOException e)   
		{  
			e.printStackTrace();  
		}  




	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) 
	{
		declarer.declare(new Fields("MSGID" , "PAYLOAD"));
	}

	@Override
	public void receive(List<String> event) 
	{
		try 
		{
			this.eventQueue.put(event);
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

