package com.example.demotest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Logger;

public class FilePayloadManager {

    private Logger logger = Logger.getLogger(FilePayloadManager.class.getName());
    public String savePayload(String Content) throws IOException {
        String Dir = "C:\\Users\\krishna-pt8304\\Downloads\\apache-tomcat-10.1.60-windows-x64\\apache-tomcat-10.1.60\\applicationPayload\\";
        if(Content == null)
        {
            return null;
        }

        byte[] content = Content.getBytes(StandardCharsets.UTF_8);
        logger.info("Lenght of the content : "+ content.length);
        String msg = null;
        if(content.length > 1024 * 100)
        {
            logger.warning("Length of content is too larger");
             throw new IllegalArgumentException("Size limit reached");
        }
        else if (content.length > (1024 * 20)) {
            String filepath = Dir + "_payload" + UUID.randomUUID() + ".dat";
            Files.write(Paths.get(filepath), content);
            logger.info("The Content is greater than 20kb so stored in file path is "+ filepath);
            return filepath;
        }
        return msg;

    }

    public String readPayload(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)),StandardCharsets.UTF_8);
    }
}
