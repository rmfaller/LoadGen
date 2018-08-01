/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package loadgen;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author rmfaller
 */
public class Task {
    private final HashMap<String, String> workload;
    private String uid = null;
    private int threshold = 0;
    private JSONObject action;

    private JSONObject loadTasks(String jfile) throws FileNotFoundException, IOException, ParseException {
        JSONObject jo;
        JSONParser jparser = new JSONParser();
        FileReader fr = new FileReader("./tasks/" + jfile + ".json");
        jo = (JSONObject) jparser.parse(fr);
        return (jo);
    }

    public Task() {
        workload = new HashMap<>();
    }

    public Task(String uid, int threshold) throws IOException, FileNotFoundException, ParseException {
        workload = new HashMap<>();
        this.uid = uid;
        this.threshold = threshold;
        this.action = loadTasks(uid);
//        System.out.println("From task = " + action);
    }

    public void put(String attr, String value) {
        workload.put(attr, value);
    }

    public String getUid() {
        return (uid);
    }
    
    public int getThreshold() {
        return threshold;
    }
    
    public JSONObject getAction() {
        return action;
    }
   
}
