package com.example.demotest;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;

@WebListener
public class ListenerClass implements ServletContextListener {
    private static Logger logger = Logger.getLogger(ListenerClass.class.getName());
    private ScheduledThreadPoolExecutor samplePostingScheduler;

    private  final HashMap<String, Consumer> workers = new HashMap<>();
    private final  HashMap<String , Thread> workerThreads = new HashMap<>();

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        // configureing the logger manager before starting the servlet
        InputStream ins = getClass().getClassLoader().getResourceAsStream("/application.properties");
        try {
            if (ins != null) {
                LogManager.getLogManager().readConfiguration(ins);
                logger.info("Logmanager configuration successfully");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MessageDAO messageDAO = new MessageDAO();


        List<String> subtables = List.of("info1", "info2", "info3","info4", "info5","warn1", "warn2","warn3","warn4","warn5","fine1","fine2","fine3","fine4","fine5","severe1","severe2","severe3","severe4","severe5");

        // create individual thread for all the subtables
        for (String subtable : subtables) {
            Consumer consumer = new Consumer(subtable, messageDAO);

            Thread thread = new Thread(consumer,"Worker THread of Subtable Name :"+ subtable);
            thread.setDaemon(true);
            thread.start();
            workers.put(subtable, consumer);
            workerThreads.put(subtable, thread);
        }

        logger.info("Scheduler has started with inital dealy 5s");

        // Scheduled TimeTask running every 5 minutes to post sample messages for each user and type
//        samplePostingScheduler = new ScheduledThreadPoolExecutor(1);
//        samplePostingScheduler.scheduleAtFixedRate(new ScheduledPostTask(), 1, 5, TimeUnit.MINUTES);


    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        workers.forEach((subtable , worker) -> {
            worker.stopWorker();
        logger.info("Sending the stop signal to worker " + subtable);
        });

        workerThreads.forEach((subtable, thread)->{
            thread.interrupt();
            try {
                thread.join(2000);
                if(thread.isAlive()) {
                    logger.info("The worker thread of" + subtable + "did not terminated after 2 seconds");
                }else {
                    logger.info("The worker thread of" + subtable + "terminated successfully after 2 seconds");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}