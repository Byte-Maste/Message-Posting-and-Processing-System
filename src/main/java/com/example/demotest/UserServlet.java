package com.example.demotest;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/application/*")
public class UserServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        UserDAO dao = new UserDAO();
        String path = req.getPathInfo();

        System.out.println(path + "------------");

        if (path == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        String[] paths = path.split("/");

        System.out.println(paths[1] + "---------");
        if (paths[1].equals("login")) {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (dao.validateUser(username, password)) {
                //login successfully
                HttpSession session = req.getSession(true);

                // Synchronized session key name with MessageServlet
                session.setAttribute("username", username);
                session.setAttribute("Name", username);
                session.setAttribute("user_id", dao.getUserID(username));

                resp.sendRedirect(req.getContextPath() + "/message.jsp");
            } else {
                resp.sendRedirect(req.getContextPath() + "/login.jsp");
            }
        } else if (paths[1].equals("logout")) {
            HttpSession session = req.getSession(false);

            if (session != null && session.getAttribute("Name") != null) {
                session.invalidate();
            }

            resp.sendRedirect(req.getContextPath() + "/login.jsp");
        } else if (paths[1].equals("register")) {
//            http://localhost:8080/DemoTest_war_exploded/application/register?username=krish&password=test@123
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            if (!dao.userExist(username)) {
                if (dao.registerUser(username, password)) {
                    System.out.print("Registration Successfully");
                    resp.sendRedirect(req.getContextPath() + "/login.jsp");
                } else {
                    System.out.print("Registration failed");
                    resp.sendRedirect(req.getContextPath() + "/register.jsp");
                }
            } else {
                System.out.print("Registration failed due to user exists");
                resp.sendRedirect(req.getContextPath() + "/register.jsp");
            }
        }

    }
}