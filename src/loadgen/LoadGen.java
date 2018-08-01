/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package loadgen;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
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
public class LoadGen extends Thread {

    public LoadGen() {
    }

    public final static void help() {
        System.out.println("Usage: java -jar ./dist/LoadGen.jar --config ./jobs/your-job-file.json ");
    }

    private static int maxWorkload(JSONArray wa) {
        int max = 0;
        int x = 0;
        for (int i = 0; i < wa.size(); i++) {
            JSONObject jo = (JSONObject) wa.get(i);
            Iterator<Long> iter = jo.values().iterator();
            while (iter.hasNext()) {
                x = (iter.next()).intValue();
            }
            if (x > max) {
                max = x;
            }
        }
        return (max);
    }

    private static int minWorkload(JSONArray wa, int min) {
        int x = 0;
        for (int i = 0; i < wa.size(); i++) {
            JSONObject jo = (JSONObject) wa.get(i);
            Iterator<Long> iter = jo.values().iterator();
            while (iter.hasNext()) {
                x = (iter.next()).intValue();
            }
            if (x < min && x != 0) {
                min = x;
            }
        }
        return (min);
    }

    private static Workload[] createWorkload(JSONObject configdata) throws IOException, FileNotFoundException, ParseException {
        JSONArray ja = (JSONArray) configdata.get("workloads");
        Workload[] workload = new Workload[ja.size()];
        for (int i = 0; i < ja.size(); i++) {
            workload[i] = new Workload();
            JSONObject jo = (JSONObject) ja.get(i);
            Iterator<String> iter = jo.keySet().iterator();
            while (iter.hasNext()) {
                String uid = (iter.next());
                Long x = (Long) jo.get(uid);
                workload[i] = new Workload(uid, x.intValue());
            }
        }
        return (workload);
    }

    private static Workers[] createWorkers(int wcnt, Results[] threadresults, int minwkld, JSONObject configdata, Workload[] workload) {
        Workers[] workers = new Workers[wcnt];
        for (int i = 0; i < wcnt; i++) {
            threadresults[i] = new Results();
            threadresults[i].putUid(i);
            threadresults[i].putResultType("results-per-thread");
            workers[i] = new Workers(i, threadresults[i], minwkld, configdata, workload);
        }
        return (workers);
    }

    /**
     * @param args the command line arguments
     * @throws java.io.FileNotFoundException
     * @throws org.json.simple.parser.ParseException
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws FileNotFoundException, IOException, ParseException, InterruptedException {
        JSONParser jparser = new JSONParser();
        JSONObject configdata = null;
        JSONArray workloads = null;
        Workers workers[];
        Workload workload[];
        FileReader fr;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--config":
                case "-c":
                    fr = new FileReader(args[i + 1]);
                    configdata = (JSONObject) jparser.parse(fr);
                    break;
                case "--help":
                case "-h":
                    help();
                default:
                    break;
            }
        }
        int maxwkld = maxWorkload((JSONArray) configdata.get("workloads"));
        int minwkld = minWorkload((JSONArray) configdata.get("workloads"), maxwkld);
        Long wcnt = (Long) configdata.get("threads");
        int workercnt = wcnt.intValue();
        Results threadresults[] = new Results[workercnt];
        workload = createWorkload(configdata);
        workers = createWorkers(workercnt, threadresults, minwkld, configdata, workload);
        String jobstart = new Date().toString();
        System.out.println("Job Started with " + workercnt + " threads on " + jobstart + ".");
        Spinner spinner = new Spinner(128);
        spinner.start();
        long jstart = (long) new Date().getTime();
        if (workercnt > 0) {
            long gcnt = 0;
            for (int i = 0; i < workers.length; i++) {
                workers[i].start();
//                System.out.println("started thread " + i);
                if (((long) configdata.get("threadgroupsize") > 0) && ((long) configdata.get("threadinterval") > 0)) {
                    gcnt++;
                    if (gcnt >= (long) configdata.get("threadgroupsize")) {
//                        LoadGen.sleep((long) configdata.get("threadinterval"));
                        sleep((long) configdata.get("threadinterval"));
                        gcnt = 0;
                    }
                }
            }
            for (int i = 0; i < workers.length; i++) {
                workers[i].join();
            }
            spinner.continueToRun = false;
            System.out.println(". Stopped and completed.");
            System.out.print("Job Started with " + workercnt + " threads on " + jobstart + "...");
            String jobstop = new Date().toString();
            long jstop = (long) new Date().getTime();
            System.out.println("and completed " + jobstop + "; lapsed clock time = " + (jstop - jstart) / 1000 + " seconds.");
            Results ttr = new Results();
            ttr = (Results) threadresults[0].clone();
            for (int i = 1; i < threadresults.length; i++) {
//                               System.out.println("--> " + threadresults[i].getUid() + ":" + threadresults[i].getResultType());
                String[] ra = ttr.getAttributes();
                for (int j = 0; j < ra.length; j++) {
//                    System.out.println(ra[j] + " ---- " + ttr.get(ra[j]));
                    ttr.put(ra[j], threadresults[i].get(ra[j]) + ttr.get(ra[j]));
//                    System.out.println(ra[j] + " ---- " + ttr.get(ra[j]));
                }
            }
            System.out.printf("%-32s", "Operation");
            System.out.printf("%10s", " TxTotal");
            System.out.printf("%10s", "AccmTime");
            System.out.printf("%10s", "Thrshold");
            System.out.printf("%10s", "  TxPass");
            System.out.printf("%10s", "PassTime");
            System.out.printf("%10s", " TxExced");
            System.out.printf("%10s", "ExcdTime");
            System.out.printf("%10s", "  TxFail");
            System.out.printf("%10s", "Failtime");
            System.out.printf("%12s", "CbdPsOps");
            System.out.printf("%12s", " ThrdOps");
            System.out.printf("%14s", "Avrms/op");
            System.out.printf("%10s", " Success");
            System.out.printf("%10s", "  Exceed");
            System.out.printf("%10s", "    Fail");
            System.out.println();
            long jlt = 0;
            long jtx = 0;
            long jptx = 0;
            long jtxsleep = 0;
            long jltsleep = 0;
            long jftx = 0;
            long jetx = 0;
            for (int i = 0; i < workload.length; i++) {
                if (workload[i].getLoops() > 0) {
                    Task[] t = workload[i].getTask();
                    HashMap tm = new HashMap();
                    for (int j = 0; j < t.length; j++) {
                        tm.put(t[j].getUid(), true);
//                    System.out.println(t[j].getUid());
                    }
                    for (int j = 0; j < t.length; j++) {
                        if ((boolean) tm.get(t[j].getUid())) {
                            tm.put(t[j].getUid(), false);
                            long ptx = 0;
                            long etx = 0;
                            long ftx = 0;
                            long ttx = 0;
                            long plt = 0;
                            long elt = 0;
                            long flt = 0;
                            long tlt = 0;
                            long thd = 0;
                            thd = ttr.get(t[j].getUid() + "-threshold");
                            ptx = ttr.get(t[j].getUid() + "-passed");
                            etx = ttr.get(t[j].getUid() + "-exceeded");
                            ftx = ttr.get(t[j].getUid() + "-failed");
                            plt = ttr.get(t[j].getUid() + "-lapsedtime");
                            elt = ttr.get(t[j].getUid() + "-exceededtime");
                            flt = ttr.get(t[j].getUid() + "-failedtime");
                            ttx = (ptx + etx + ftx);
                            tlt = (plt + elt + flt);
                            jtxsleep = jtxsleep + ttx;
                            if (!t[j].getUid().matches("sleep")) {
                                jtx = jtx + ttx;
                                jptx = jptx + ptx;
                                jlt = jlt + plt;
                                if (ttx > 0) {
//                    System.out.print(ttx + "\t" + tlt / wcnt + "ms\t" + thd / wcnt + "ms\t" + ptx + "\t" + plt / wcnt + "ms\t" + etx + "\t" + elt / wcnt + "ms\t" + ftx + "\t" + flt / wcnt + "ms\t");
                                    System.out.format("%-32s", t[j].getUid());
                                    System.out.format("%10d", ttx);
                                    System.out.format("%9.3f%s", (tlt / (float) 1000), "s");
                                    System.out.format("%8d%s", thd / wcnt, "ms");
                                    System.out.format("%10d", ptx);
                                    System.out.format("%9.3f%s", (plt / (float) 1000), "s");
                                    System.out.format("%10d", etx);
                                    System.out.format("%9.3f%s", (elt / (float) 1000), "s");
                                    System.out.format("%10d", ftx);
                                    System.out.format("%9.3f%s", (flt / (float) 1000), "s");
//                                System.out.format("%10.3f%s", (float) (((ptx - etx - ftx) / (float) (plt / (float) wcnt))) * 1000, "/s");
                                    System.out.format("%10.3f%s", (float) (ptx / (float) (plt / (float) wcnt)) * 1000, "/s");
                                    System.out.format("%10.3f%s", (float) (ptx / (float) plt) * 1000, "/s");
                                    System.out.format("%9.3f%s", (float) (plt / (float) ptx), "ms/op");
                                    System.out.format("%9.2f%s", (ptx / (float) ttx) * 100, "%");
                                    System.out.format("%9.2f%s", (etx / (float) ttx) * 100, "%");
                                    System.out.format("%9.2f%s", (ftx / (float) ttx) * 100, "%");
                                    System.out.println();
                                }
                            } else {
                                jltsleep = jltsleep + tlt;
                            }
                        }
                    }
                }
            }
            System.out.println("Job lapsed time =\t" + (jstop - jstart) + "ms; " + ((jstop -jstart) / 1000) + "s");
            System.out.println("Passed Accum time =\t" + jlt + "ms; " + (jlt / 1000) + "s");
            System.out.println("Total Threads =\t\t" + wcnt);
            System.out.println("Thread group size =\t" + configdata.get("threadgroupsize"));
            System.out.println("Launch interval =\t" + configdata.get("threadinterval") + "ms");
            System.out.println("Transaction goal =\t" + jtx);
            System.out.println("Transaction actual =\t" + jptx);
            System.out.print("Successful ops/s =\t");
            System.out.format("%-8.3f", ((float) (jptx / (float) (jlt / (float) wcnt)) * 1000));
            System.out.print("\nOps/s during job =\t");
            System.out.format("%-8.3f", ((float) (jptx / (float) (jstop - jstart)) * 1000));
            System.out.print("\nSuccess rate =\t\t");
            System.out.format("%3.2f", (jptx / (float) jtx) * 100);
            System.out.print("%\nTotal sleep sec =\t");
            System.out.format("%-9.3f", (jltsleep / (float) 1000));
            System.out.println("\nBased on following workloads:");
            for (int i = 0; i < workload.length; i++) {
                if (workload[i].getLoops() > 0) {
                    System.out.print("\tWorkload: " + workload[i].getUid() + " with " + (workload[i].getLoops() * wcnt) + " loops;");
                    System.out.print("\tPercent load = ");
                    System.out.format("%3.2f", ((((workload[i].getLoops() * workload[i].getTask().length) * wcnt) / (float) jtxsleep) * 100));
                    System.out.println("%");
                    System.out.println("\tTasks used:");
                    Task[] t = workload[i].getTask();
                    for (int j = 0; j < t.length; j++) {
                        System.out.println("\t\t" + t[j].getUid());
                    }
                }
            }
            System.out.println("Target environment(s): " + configdata.get("service-environment"));
            System.out.println("Load generator: " + configdata.get("load-generator"));
        }
//        System.out.println("max=" + maxwkld + " min=" + minwkld + " threads = " + t); 
    }

}
