/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;
import java.util.*;
import java.util.concurrent.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.xml.UtilReader;
/**
 * Generic MMBase Thread Pools
 *
 * @since MMBase 1.8
 * @author Michiel Meeuwissen
 * @version $Id: ThreadPools.java,v 1.16 2008-08-01 21:04:52 michiel Exp $
 */
public abstract class ThreadPools {
    private static final Logger log = Logging.getLoggerInstance(ThreadPools.class);

    private static Map<Future, String> identifiers = Collections.synchronizedMap(new WeakHashMap<Future, String>());

    /**
     * There is no way to identify the FutureTask objects returned in
     * the getQueue methods of the executors.  This works around that.
     * Used by admin pages. 
     * @since MMBase-1.9
     */
    public static String identify(Future r, String s) {
        return identifiers.put(r, s);
    }
    /**
     * returns a identifier string for the given task.
     * @since MMBase-1.9
     */
    public static String getString(Future r) {
        String s = identifiers.get(r);
        if (s == null) return "" + r;
        return s;
    }

    /**
     * Generic Thread Pools which can be used by 'filters'. Filters
     * are short living tasks. This is mainly used by {@link
     * org.mmbase.util.transformers.ChainedCharTransformer} (and only
     * when transforming a Reader).
     * 
     * Code performing a similar task could also use this thread pool.
     */
    public static final ExecutorService filterExecutor = Executors.newCachedThreadPool();


    private static Thread newThread(Runnable r, String id) {
        Thread t = new Thread(org.mmbase.module.core.MMBaseContext.getThreadGroup(), r, id) {
                /**
                 * Overrides run of Thread to catch and log all exceptions. Otherwise they go through to app-server.
                 */
                public void run() {
                    try {
                        super.run();
                    } catch (Throwable t) {
                        log.error("Error during job: " + t.getClass().getName() + " " + t.getMessage(), t);
                    }
                }
            };
        t.setDaemon(true);
        return t;
    }

    /**
     * All kind of jobs that should happend in a seperat Thread can be
     * executed by this executor. E.g. sending mail could be done by a
     * jobs of this type.
     * 
     */
    public static final ExecutorService jobsExecutor = new ThreadPoolExecutor(2, 10, 5 * 60 , TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(200), new ThreadFactory() {

            public Thread newThread(Runnable r) {
                return ThreadPools.newThread(r, "JOBTHREAD");
            }
        });

    /**
     * This executor is for repeating tasks. E.g. every running
     * {@link org.mmbase.module.Module}  has a  {@link
     * org.mmbase.module.Module#maintainance} which is scheduled to
     * run every hour.
     *
     * @since MMBase-1.9
     */
    public static final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(2, new ThreadFactory() {
            public Thread newThread(Runnable r) {
                return ThreadPools.newThread(r, "SCHEDULERTHREAD");
            }
        });



    public static final UtilReader properties = new UtilReader("threadpools.xml", new Runnable() { public void run() { configure(); }});

    /**
     * @since MMBase-1.9
     */
    public static void configure() {

        Map<String,String> props = properties.getProperties();
        String max = props.get("jobs.maxsize");
        if (max != null) {
            log.info("Setting max pool size from " + ((ThreadPoolExecutor) jobsExecutor).getMaximumPoolSize() + " to " + max);
            ((ThreadPoolExecutor) jobsExecutor).setMaximumPoolSize(Integer.parseInt(max));
        }
        String core = props.get("jobs.coresize");
        if (core != null) {
            log.info("Setting core pool size from " + ((ThreadPoolExecutor) jobsExecutor).getCorePoolSize() + " to " + core);
            ((ThreadPoolExecutor) jobsExecutor).setCorePoolSize(Integer.parseInt(core));
        }

        String schedSize = props.get("scheduler.coresize");
        if (schedSize != null) {
            log.info("Setting scheduler pool size from " + ((ThreadPoolExecutor) scheduler).getCorePoolSize() + " to " + schedSize);
            ((ThreadPoolExecutor) scheduler).setCorePoolSize(Integer.parseInt(schedSize));
        }
    }

    /**
     * @since MMBase-1.8.4
     */
    public static void shutdown() {
        {
            List<Runnable> run = scheduler.shutdownNow();
            if (run.size() > 0) {
                log.info("Interrupted " + run);
            }
        }
        {

            List<Runnable> run = filterExecutor.shutdownNow();
            if (run.size() > 0) {
                log.info("Interrupted " + run);
            }
            
        }
        {
            List<Runnable> run = jobsExecutor.shutdownNow();
            if (run.size() > 0) {
                log.info("Interrupted " + run);
            }
        }
    }

}
