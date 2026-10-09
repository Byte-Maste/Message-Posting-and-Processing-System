package com.example.demotest;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

public class Consumer implements Runnable {
    private String subtable;
    private MessageDAO messageDAO;
    private static Logger logger = Logger.getLogger(Consumer.class.getName());

    private volatile boolean running = true;
    Consumer(String subtable, MessageDAO messageDAO) {
        this.subtable = subtable;
        this.messageDAO = messageDAO;
    }

    public void stopWorker()
    {
        this.running = false;
    }
    @Override
    public void run() {
        // this cheduler is responsible to take the first row from each table and each thread will execute that particular table

        // get all the subtable first row and store it in the  messageProcess

        // to make it dynamic

        while (running) {
            boolean processed = messageDAO.processSubtableMessage(subtable);

            if (processed) {
                logger.info("Message Has been inserted into messsage processor table and deleted from the subtable: " + subtable);
            }

            if(!processed)
            {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }
}