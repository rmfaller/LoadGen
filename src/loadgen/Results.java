package loadgen;

import java.util.HashMap;
import java.util.Iterator;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author rmfaller
 */
public class Results extends HashMap {
    
    private final HashMap results;
    private int uid = 0;
    private String resulttype = null;
    
    public Results() {
        results = new HashMap();
    }
    
    public void put(int threadid, String attr, long value) {
        results.put(attr,value);
    }
    
    public void put(String attr, long value) {
        results.put(attr, value);
    }
    
    public void putUid(int uid) {
        this.uid = uid;
    }
    
    public void putResultType(String rt) {
        this.resulttype = rt;
    }
    
    public int getUid() {
        return(this.uid);
    }
    
    public String getResultType() {
        return(this.resulttype);
    }
    
    public String[] getAttributes() {
        String[] attrs = new String[results.size()];
        int i = 0;
         Iterator<String> iter = results.keySet().iterator();
            while (iter.hasNext()) {
                attrs[i] = iter.next();
                i++;
            }
        return (attrs);
    }
    
    public long get(String attr) {
        long value = 0;
        if (results.get(attr) != null) {
            value = (long) results.get(attr);
        }
        return(value);
    }
    
}
