/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package loadgen;


import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rmfaller
 */
class Spinner extends Thread {

    boolean continueToRun = true;
    int sleeptime = 100;

    Spinner(int i) {
        this.sleeptime = i;
    }

    @Override
    public void run() {
        String anim = "|/-\\";
//        String anim = ".oO@*";
//        String anim = "⠁⠂⠄⡀⢀⠠⠐⠈";
        long starttime = (long) new Date().getTime();
        int x = 0;
        while (continueToRun) {
            System.out.print("\r Processing " + anim.charAt(x++ % anim.length()) + " for " + (((long) new Date().getTime()) - starttime) + "ms");
            try {
                Thread.sleep(this.sleeptime);
            } catch (InterruptedException ex) {
                Logger.getLogger(Spinner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}