package com.example.demotest;

import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static java.lang.Math.abs;
import static java.util.Objects.hash;

@WebServlet("/message")
public class MessageServlet extends HttpServlet {

    private Logger logger = Logger.getLogger(MessageServlet.class.getName());
    FilePayloadManager filePayloadManager = new FilePayloadManager();
    //generate the hascode based on userid

    MessageDAO msgdao = new MessageDAO();
    UserDAO dao = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //get the userid to perform has and do in which table do we need ot insert it


        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        int user_id = dao.getUserID((String) session.getAttribute("username"));




        String msg = (filePayloadManager.savePayload(req.getParameter("Content")));

        boolean isPayload = false;
        String filePath = null;
        if(msg != null) {
            isPayload = (msg != null) ? true : false;
            filePath = (msg != null) ? msg : null;
            msg = null;
        }else {
            msg = req.getParameter("Content");
        }



        String Type = req.getParameter("Type");
        String priority = req.getParameter("priority");



        // type will be like INFO,WARN so table is INFO1
        String tableName = msgdao.getOrAssingTable(user_id , Type);

        //calling the dao with dynamic table name and insert the data into it

        logger.info("Storing data into the db Table Name is --- " + tableName +" and does this content is stored in file if yes then file path: "+ filePath);
        msgdao.storeUserMsg(tableName, user_id, msg, Type, priority, isPayload, filePath);
        resp.sendRedirect(req.getContextPath() + "/message.jsp?posted=true");
    }
}