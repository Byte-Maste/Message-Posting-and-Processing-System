package com.example.demotest;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.lang.Math.abs;
import static java.util.Objects.hash;

@WebServlet("/message")
public class MessageServlet extends HttpServlet {
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

        byte[] content = req.getParameter("Content").getBytes(StandardCharsets.UTF_8);
        String msg = null;
        if (content.length < 1024 * 20) {
            msg = req.getParameter("Content");
        } else if (content.length < 1024 * 100) {
            // store it as path
        } else {
            // store the message with "Size limit reached"
        }

        String Type = req.getParameter("Type");
        String priority = req.getParameter("priority");

        int tableNumber = (abs(hash(user_id)) % 2) + 1;

        // type will be like INFO,WARN so table is INFO1
        String tableName = Type + tableNumber;

        //calling the dao with dynamic table name and insert the data into it

        msgdao.storeUserMsg(tableName, user_id, msg, Type, priority);
        resp.sendRedirect(req.getContextPath() + "/message.jsp?posted=true");
    }
}