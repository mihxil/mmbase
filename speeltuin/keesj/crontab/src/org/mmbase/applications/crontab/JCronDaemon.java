package org.mmbase.applications.crontab;

import java.util.*;
import org.mmbase.util.logging.*;

/**
 * JCronDaemonn is a "crontab" clone written in java.
 * The daemon starts a thread that wakes up every minute
 *(it keeps sync by calculating the time to sleep)
 * @author Kees JongenBurger
 * @author Michiel Meeuwissen
 **/
public class JCronDaemon implements Runnable {

    private static final Logger log = Logging.getLoggerInstance(JCronDaemon.class);
    
    private static JCronDaemon jCronDaemon;
    private Thread cronThread;
    private Set jCronEntries;
    
    private JCronDaemon() {
        jCronEntries = Collections.synchronizedSet(new HashSet());
        start();
    }
    
    public void add(JCronEntry entry){
        if (jCronEntries.contains(entry)) {
            throw new RuntimeException("There is an entry  " + entry + " already");
        }
        jCronEntries.add(entry);
        log.info("Added to JCronDaemon " + entry);
    }

    public void remove(JCronEntry entry){
        jCronEntries.remove(entry);
        log.info("Removed from JCronDaemon " + entry);
    }
    
    public void start(){
        log.info("Starting JCronDaemon");
        cronThread = new Thread(this, "JCronDaemon");
        //cronThread.setDaemon(true);
        cronThread.start();
    }
    
    public void stop(){
        log.info("Stopping JCronDaemon");
        cronThread.interrupt();
        cronThread=  null;
    }
    
    public static synchronized JCronDaemon getInstance(){
        if (jCronDaemon == null){
            jCronDaemon = new JCronDaemon();
        }
        return jCronDaemon;
    }
    
    
    public void run() {
        Thread thisThread = Thread.currentThread();

        while(thisThread == cronThread){
                    
            long now  = System.currentTimeMillis();
            long next = (now + 60 * 1000 ) / 60000 * 60000; // next minute, rounded to minute

            try {
                Thread.sleep(next - now); // sleep until  next minute
            } catch (InterruptedException ie) {
                log.info("Interrupted: " + ie.getMessage());
            }
            Date currentMinute = new Date(next);

            Iterator z = jCronEntries.iterator();
            while (z.hasNext()) {
                JCronEntry entry = (JCronEntry) z.next();
                if (entry.mustRun(currentMinute)) {
                    if (entry.kick()) {
                        log.debug("started " + entry);
                    } else {
                        log.warn("Job " + entry + " still running, so not restarting it again.");
                    }
                } else {
                    log.trace("skipped " + entry);
                }
            }
        }
    }
    
    public static void main(String[] argv) throws Exception{
        JCronDaemon d = JCronDaemon.getInstance();
        
        //entries.add(new JCronEntry("20 10 31 8 *","happy birthday"));
        //entries.add(new JCronEntry("* * * * 1","monday"));
        //entries.add(new JCronEntry("* * * * 2","tuesday"));
        
        //entries.add(new JCronEntry("* * 19 * *","the 19'st  day of the month"));
        
        //entries.add(new JCronEntry("* * * 1 *","the first month of the year"));
        //entries.add(new JCronEntry("*/2 * * * *","every 2 minutes stating from 0"));
        //entries.add(new JCronEntry("1-59/2 * * * *","every 2 minutes stating from 1"));
        d.add(new JCronEntry("1","*/2 5-23 * * *", "every 2 minute from 5 till 11 pm", "org.mmbase.applications.crontab.TestCronJob"));
        //entries.add(new JCronEntry("40-45,50-59 * * * *","test 40-45,50-60","Dummy"));
        
        try {Thread.currentThread().sleep(240 * 1000 * 60); } catch (Exception e){};
        d.stop();
    }
}
