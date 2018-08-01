package loadgen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author rmfaller
 */
public class Workers extends Thread {

    private int threadid;
    private Results results;
    private int minloop;
    private JSONObject configdata;
    private Workload[] workload;
    private JSONObject[] response;
    private Long randomvalue;
    private JSONObject dpljo;
    private JSONObject jtask;

    public Workers() {
    }

    public Workers(int threadid, Results results, int minloop, JSONObject configdata, Workload[] workload) {
        this.threadid = threadid;
        this.results = results;
        this.minloop = minloop;
        this.configdata = configdata;
        this.workload = workload;
        for (int i = 0; i < workload.length; i++) {
            Task[] t = workload[i].getTask();
            for (int j = 0; j < t.length; j++) {
//                System.out.println("thread =" + threadid + " i = " + i + " j = " + j + " workload = " + workload[i].getUid() + " task = " + t[j].getUid());
                results.put(t[j].getUid() + "-passed", 0);
                results.put(t[j].getUid() + "-exceeded", 0);
                results.put(t[j].getUid() + "-failed", 0);
                results.put(t[j].getUid() + "-lapsedtime", 0);
                results.put(t[j].getUid() + "-exceededtime", 0);
                results.put(t[j].getUid() + "-failedtime", 0);
                results.put(t[j].getUid() + "-tasktype", 0);
                results.put(t[j].getUid() + "-threshold", t[j].getThreshold());
            }
//            this.response = new JSONObject[t.length];
        }
    }

    private static JSONObject replaceJO(JSONObject cjo, String rv) {
        JSONObject jo;
        jo = (JSONObject) cjo.clone();
        Iterator<String> iter = jo.keySet().iterator();
        while (iter.hasNext()) {
            String attr = iter.next();
            if (jo.get(attr).getClass() == JSONObject.class) {
                JSONObject njo = (JSONObject) jo.get(attr);
//                System.out.println("njo = " + njo.toJSONString());
                replaceJO(njo, rv);
            }
            if (jo.get(attr).getClass() == JSONArray.class) {
                JSONArray ja = (JSONArray) jo.get(attr);
                for (int i = 0; i < ja.size(); i++) {
                    JSONObject njo = (JSONObject) ja.get(i);
//                    System.out.println("njo array = " + njo.toJSONString() + " | " + ja.toJSONString());
                    replaceJO(njo, rv);
                }
            }
            if (jo.get(attr).getClass() == String.class) {
                String value = (String) jo.get(attr);
//                System.out.println("value : " + value + " -- " + rv);
                if (value.contains("$RANDOMVALUE")) {
                    value = value.replace("$RANDOMVALUE", rv);
                    jo.put(attr, value);
//                System.out.println("value : " + value + " -- " + rv + " : " + jo.toJSONString());
                }
                if (value.contains("$INCREMENT")) {
                    value = value.replace("$INCREMENT", rv);
                    jo.put(attr, value);
//                System.out.println("value : " + value + " -- " + rv  + " : " + jo.toJSONString());
                }
            }
        }
        return (jo);
    }

    @Override
    public void run() {
        Task[] task;
        int loopcnt = 0;
        int innerloopcnt = 0;
        int counter = 0;
        boolean incrementcounter = false;
        boolean worked;
        boolean withinthreshold;
        JSONObject joheader;
        HashMap hhm;
        URI uri;
        URL url;
        String uep = null;
//        System.out.println("running threadid = " + this.threadid);
        int sleeptime = 0;
        while (loopcnt < minloop) {
            for (int i = 0; i < workload.length; i++) {
                innerloopcnt = 0;
                while (innerloopcnt < (workload[i].getLoops() / minloop)) {
                    task = workload[i].getTask();
                    this.randomvalue = (long) (Math.random() * ((long) configdata.get("upperbounds") + 1));
                    worked = true;
                    withinthreshold = true;
                    this.response = new JSONObject[task.length];
                    for (int j = 0; j < task.length; j++) {
//                        randomvalue = (long) (Math.random() * ((long) configdata.get("upperbounds") + 1));
                        this.jtask = task[j].getAction();
//                        System.out.println("executing j= " + j + " :: " + task[j].getAction() + " " + this.jtask.get("request") + "threadid=" + threadid + " rnad=" + this.randomvalue);
                        long taskstart = (long) new Date().getTime();
                        if (worked) { // if worked
                            if (incrementcounter) {
                                incrementcounter = false;
                                counter++;
                            }
                            try {
                                uep = (String) this.jtask.get("url-endpoint");
                                if (!uep.contains("$SLEEP")) {
                                    String upl = (String) this.jtask.get("url-payload");
                                    String dpl = null;
                                    String replacement = null;
                                    if (upl.contains("$RANDOMVALUE")) {
                                        replacement = this.randomvalue.toString();
                                        upl = upl.replace("$RANDOMVALUE", replacement);
                                    }
                                    if (upl.contains("$TOKENID")) {
                                        replacement = (String) this.response[0].get("tokenId");
                                        upl = upl.replace("$TOKENID", replacement);
                                    }
                                    if (upl.contains("$THREADID")) {
                                        replacement = Integer.toString(this.threadid);
                                        upl = upl.replace("$THREADID", replacement);
                                    }
                                    if (upl.contains("$INCREMENT")) {
//                                        replacement = this.threadid + "." + counter;
                                        replacement = Integer.toString(counter);
                                        upl = upl.replace("$INCREMENT", replacement);
                                        incrementcounter = true;
                                    }
                                    String encodedupl = URLEncoder.encode(upl, "UTF-8");
                                    if (jtask.containsKey("data-payload")) {
                                        if (jtask.get("data-payload").getClass() != String.class) {
                                            JSONObject tmpjo = (JSONObject) this.jtask.get("data-payload");
                                            String tmpjs = tmpjo.toJSONString();
                                            if (tmpjs.contains("$INCREMENT")) {
//                                                System.out.println("FOUND INCREMENT");
                                                incrementcounter = true;
                                            }
                                            tmpjs = tmpjs.replace("$INCREMENT", Integer.toString(counter));
                                            tmpjs = tmpjs.replace("$RANDOMVALUE", this.randomvalue.toString());
                                            tmpjs = tmpjs.replace("$THREADID", Integer.toString(this.threadid));
                                            JSONParser jp = new JSONParser();
                                            try {
                                                tmpjo = (JSONObject) jp.parse(tmpjs);
                                            } catch (ParseException ex) {
                                                Logger.getLogger(Workers.class.getName()).log(Level.SEVERE, null, ex);
                                            }
//                                            System.out.println("tmpjo -> " + tmpjo.toJSONString());
                                            this.dpljo = tmpjo;
//                                            this.dpljo = replaceJO((JSONObject) this.jtask.get("data-payload"), replacement);
//                                            System.out.println("dpl = " + threadid + "::" + this.dpljo);
                                        }
                                    }
                                    /*                                    if (jtask.containsKey("service-location") && this.jtask.containsKey("service-port")) {
                                     uri = new URI((String) this.jtask.get("service-location") + (String) this.jtask.get("service-port") + uep + upl);
                                     } else {
                                     uri = new URI((String) configdata.get("service-location") + (String) configdata.get("service-port") + uep + upl);
                                     } */
                                    if (jtask.containsKey("service-location-port")) {
                                        uri = new URI((String) this.jtask.get("service-location-port") + uep + upl);
                                    } else {
                                        /* Inject code to read service-location-port from workload. Need code in Workload to expose */
                                        uri = new URI((String) configdata.get("service-location-port") + uep + upl);
                                    }
                                    url = uri.toURL();
                                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                    conn.setRequestMethod((String) this.jtask.get("request"));
                                    joheader = (JSONObject) this.jtask.get("header");
//                                    System.out.println("HEADER++++++++++++++++++++++++ " + joheader);
                                    for (int h = 0; h < joheader.size(); h++) {
                                        Iterator<String> iter = joheader.keySet().iterator();
                                        while (iter.hasNext()) {
                                            String headerattr = iter.next();
                                            String headervalue = (String) joheader.get(headerattr);
                                            if (headervalue.contains("$ADMINUSERID")) {
                                                headervalue = headervalue.replace("$ADMINUSERID", (String) configdata.get("ADMINUSERID"));
                                            }
                                            if (headervalue.contains("$ADMINUSERPASSWORD")) {
                                                headervalue = headervalue.replace("$ADMINUSERPASSWORD", (String) configdata.get("ADMINUSERPASSWORD"));
                                            }
                                            if (headervalue.contains("$RANDOMVALUE")) {
                                                headervalue = headervalue.replace("$RANDOMVALUE", this.randomvalue.toString());
                                            }
                                            if (headerattr.contains("iplanetDirectoryPro")) {
                                                headervalue = headervalue.replace("tokenId", (String) this.response[0].get("tokenId"));
                                            }
                                            if (headervalue.contains("$AAD-ACCESS-TOKEN")) {
//                                                System.out.println("------------:::::" + (String) this.response[0].get("access_token"));
                                                headervalue = headervalue.replace("$AAD-ACCESS-TOKEN", (String) this.response[0].get("access_token"));
                                            }
                                            conn.setRequestProperty(headerattr, headervalue);
//                                    System.out.println("====" + headerattr + "______" + headervalue);
                                        }
                                    }
                                    conn.setDoOutput(true);
                                    conn.setConnectTimeout(task[j].getThreshold());
                                    conn.setReadTimeout(task[j].getThreshold());
//                                    System.out.println("payload=" + this.jtask.get("data-payload").toString());
                                    if (jtask.containsKey("data-payload")) {
                                        try (OutputStreamWriter cwr = new OutputStreamWriter(conn.getOutputStream())) {
                                            if (jtask.get("data-payload").getClass() != String.class) {
                                                String s = this.dpljo.toString();
                                                s = s.replace("\\", "");
//                                                cwr.write(this.dpljo.toString());
                                                cwr.write(s);
//                                                System.out.println("Full payload = " + this.dpljo.toString());
//                                                System.out.println("Full JSON payload = " + s);
                                            } else {
                                                String s = jtask.get("data-payload").toString();
                                                s = s.replace("\\", "");
//                                                cwr.write(jtask.get("data-payload").toString());
//                                                System.out.println("Full String payload = " + s);
                                                cwr.write(s);
                                            }
                                            cwr.close();
                                        }
                                    }
                                    BufferedReader reader = null;
                                    try {
                                        reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                                char[] buffer = new char[1024];
//                                int charsRead = 0;
                                        StringBuilder incoming = new StringBuilder();
//                                while ((charsRead = reader.read(buffer, 0, 1024)) != -1) {
                                        String rl = null;
                                        while ((rl = reader.readLine()) != null) {
                                            incoming.append(rl);
//                                            System.out.println("sb = " + rl);
                                        }
                                        reader.close();
                                        JSONParser jp = new JSONParser();
                                        this.response[j] = (JSONObject) jp.parse(incoming.toString());
//                                System.out.println("token=" + response[j].get("tokenId"));
                                    } catch (SocketTimeoutException ex) {
                                        worked = false;
                                        withinthreshold = false;
                                        Logger.getLogger(Workers.class.getName()).log(Level.SEVERE, null, ex);
                                        System.err.println("Socket time out");
                                        if (reader != null) {
                                            try {
                                                reader.close();
                                            } catch (IOException ioe) {
                                                Logger.getLogger(Workers.class.getName()).log(Level.SEVERE, null, ioe);
                                            }
                                        }
                                    } catch (IOException ex) {
                                        worked = false;
                                        System.err.println("IOException");
                                        Logger.getLogger(Workers.class.getName()).log(Level.SEVERE, null, ex);
                                    } catch (ParseException ex) {
                                        Logger.getLogger(Workers.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    conn.disconnect();
                                } else {
                                    Long slt = (long) this.jtask.get("url-payload");
                                    sleeptime = (slt.intValue() + task[j].getThreshold());
//                                    sleeptime = task[j].getThreshold() - 10;
                                    Thread.sleep(sleeptime);
                                }
                            } catch (URISyntaxException | MalformedURLException ex) {
                                Logger.getLogger(Workers.class.getName()).log(Level.SEVERE, null, ex);
                                worked = false;
                            } catch (IOException ex) {
                                Logger.getLogger(Workers.class.getName()).log(Level.SEVERE, null, ex);
                                worked = false;
                            } catch (InterruptedException ex) {
                                Logger.getLogger(Workers.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }// end of if worked
                        long taskstop = (long) new Date().getTime();
                        long lapsedtime = (taskstop - taskstart);
                        if (((lapsedtime <= task[j].getThreshold()) && (worked)) || (uep.contains("$SLEEP"))) {
                            results.put(task[j].getUid() + "-passed", results.get(task[j].getUid() + "-passed") + 1);
                            results.put(task[j].getUid() + "-lapsedtime", results.get(task[j].getUid() + "-lapsedtime") + lapsedtime);
//                            System.out.println("Threadid = " + threadid + " PASSED = " + lapsedtime + " Total " + task[j].getUid() + "= " + results.get(task[j].getUid() + "-passed"));
                        } else {
                            if (lapsedtime > task[j].getThreshold() || (!withinthreshold)) {
//                                System.err.println("EXCEEDED = " + lapsedtime + " target = " + task[j].getThreshold());
                                worked = false;
                                withinthreshold = true;
                                results.put(task[j].getUid() + "-exceeded", results.get(task[j].getUid() + "-exceeded") + 1);
                                results.put(task[j].getUid() + "-exceededtime", results.get(task[j].getUid() + "-exceededtime") + lapsedtime);
                            } else {
//                                System.err.println("FAILED = " + lapsedtime + " target = " + task[j].getThreshold());
                                worked = false;
                                results.put(task[j].getUid() + "-failed", results.get(task[j].getUid() + "-failed") + 1);
                                results.put(task[j].getUid() + "-failedtime", results.get(task[j].getUid() + "-failedtime") + lapsedtime);
                            }
                        }
                    }
                    innerloopcnt++;
                }
            }
            loopcnt++;
        }
    }
}
