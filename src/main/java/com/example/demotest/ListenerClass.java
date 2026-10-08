package com.example.demotest;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;

@WebListener
public class ListenerClass implements ServletContextListener {
    private static Logger logger = Logger.getLogger(ListenerClass.class.getName());
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private ScheduledThreadPoolExecutor samplePostingScheduler;

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
        ThreadPoolExecutor workerpool = new ThreadPoolExecutor(4, 10, 0L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));

        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);

        Consumer consumer = new Consumer(workerpool, messageDAO);
        scheduledThreadPoolExecutor.scheduleAtFixedRate(consumer, 5, 30, TimeUnit.SECONDS);

        logger.info("Scheduler has started with inital dealy 5s");

        // Scheduled TimeTask running every 5 minutes to post sample messages for each user and type
        samplePostingScheduler = new ScheduledThreadPoolExecutor(1);
        samplePostingScheduler.scheduleAtFixedRate(new ScheduledPostTask(), 1, 5, TimeUnit.MINUTES);

        sce.getServletContext().setAttribute("WORKERPOOL", workerpool);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ThreadPoolExecutor workerpool = (ThreadPoolExecutor) sce.getServletContext().getAttribute("WORKERPOOL");
        if (workerpool != null) {
            workerpool.shutdown();
            try {
                if (!workerpool.awaitTermination(10, TimeUnit.SECONDS)) {
                    workerpool.shutdownNow();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (scheduledThreadPoolExecutor != null) {
            scheduledThreadPoolExecutor.shutdown();
        }
        if (samplePostingScheduler != null) {
            samplePostingScheduler.shutdown();
        }
    }
}