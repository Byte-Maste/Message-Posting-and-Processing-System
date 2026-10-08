package com.example.demotest;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

public class Consumer implements Runnable {
    private ThreadPoolExecutor workerpool;
    private MessageDAO messageDAO;
    private static Logger logger = Logger.getLogger(Consumer.class.getName());

    Consumer(ThreadPoolExecutor workerpool, MessageDAO messageDAO) {
        this.workerpool = workerpool;
        this.messageDAO = messageDAO;
    }

    @Override
    public void run() {
        // this cheduler is responsible to take the first row from each table and each thread will execute that particular table

        // get all the subtable first row and store it in the  messageProcess

        // to make it dynamic

        List<String> subtables = List.of("INFO1", "INFO2", "WARN1", "WARN2");

        for (String subtable : subtables) {
            // Process row atomically with FOR UPDATE and transaction safety
            boolean processed = messageDAO.processNextSubtableMessage(subtable);

            if (processed) {
                logger.info("Message Has been inserted into messsage processor table and deleted from the subtable: " + subtable);
            }
        }
    }
}