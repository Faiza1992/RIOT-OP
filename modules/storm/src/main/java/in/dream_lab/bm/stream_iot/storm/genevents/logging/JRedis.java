package in.dream_lab.bm.stream_iot.storm.genevents.logging;

import in.dream_lab.bm.stream_iot.storm.genevents.utils.GlobalConstants;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import java.util.ArrayList;
import java.util.List;

public class JRedis{
    int counter=0;
    List<TupleType> batch = new ArrayList<TupleType>();
    int threshold; //Count of rows after which the map should be flushed to log file
    String appName;

    public JRedis(){
        this.threshold = GlobalConstants.thresholdFlushToLog; //2000 etc
    	this.appName = "/home/amna/test_test.log";
 
    }
 
    public JRedis(String fileName){
	//System.out.println("kpppp_"+fileName);
        this.threshold = GlobalConstants.thresholdFlushToLog; //2000 etc
    	String[] fileName2 = fileName.split("/");
	String[] file = fileName2[fileName2.length-1].split("-");
	this.appName = file[1];
	//System.out.println("kpppp_"+this.appName);
        
    }
 
    public void batchWriter(long ts,String identifierData) throws Exception
    {
        if (counter<this.threshold)
        {
            batch.add(new TupleType(ts, identifierData));
            counter += 1;
        }
        else
        {
            Jedis jedis = new Jedis("127.0.0.1", 6379);
            Pipeline p = jedis.pipelined(); 
            for(TupleType tp : batch){
                //this.out.write( this.logStringPrefix + "," + tp.ts + "," + tp.identifier + "\n");
		//System.out.println("kpppp_"+tp.identifier);
                if(tp.identifier.contains("MSGID")){
                	//p.set(this.appName + "_"+tp.ts + "_" + tp.identifier, "-1");
			p.hset(this.appName + "_spout", tp.identifier, String.valueOf(tp.ts));
		}else {
                     	//p.set(this.appName + "_"+tp.ts + "_" + tp.identifier, String.valueOf(tp.ts));
			p.hset(this.appName + "_sink", tp.identifier, String.valueOf(tp.ts));
		}

            }
	    p.sync();
            batch.clear();

            counter = 1 ;
            batch.add(new TupleType(ts, identifierData));
        }
    }

}
