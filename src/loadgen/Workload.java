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
import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author rmfaller
 */
public class Workload {

    private final HashMap<String, String> workload;
    private String uid = null;
    private int loops = 0;
    private Task[] task;

    private JSONObject loadTasks(String jfile) throws FileNotFoundException, IOException, ParseException {
        JSONObject jo;
        JSONParser jparser = new JSONParser();
        FileReader fr = new FileReader("./workloads/" + jfile + ".json");
        jo = (JSONObject) jparser.parse(fr);
        return (jo);
    }

    private static Task[] createTask(JSONObject configdata) throws IOException, FileNotFoundException, ParseException {
        JSONArray ja = (JSONArray) configdata.get("tasks");
        Task[] task = new Task[ja.size()];
        for (int i = 0; i < ja.size(); i++) {
            task[i] = new Task();
            JSONObject jo = (JSONObject) ja.get(i);
            Iterator<String> iter = jo.keySet().iterator();
            while (iter.hasNext()) {
                String uid = (iter.next());
                Long x = (Long) jo.get(uid);
                task[i] = new Task(uid, x.intValue());
            }
        }
        return (task);
    }

    public Workload() {
        workload = new HashMap<>();
    }

    public Workload(String uid, int loops) throws IOException, FileNotFoundException, ParseException {
        workload = new HashMap<>();
        this.uid = uid;
        this.loops = loops;
        JSONObject tasks = loadTasks(uid);
        this.task = createTask(tasks);
//        System.out.println("From Workload = " + task[0].getThreshold());
    }

    public void put(String attr, String value) {
        workload.put(attr, value);
    }

    public String getUid() {
        return (uid);
    }

    public int getLoops() {
        return loops;
    }
    
    public Task[] getTask() {
        return this.task;
    }

}
