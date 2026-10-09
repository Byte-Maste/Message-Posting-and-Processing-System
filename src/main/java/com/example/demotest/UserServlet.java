package com.example.demotest;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Logger;

@WebServlet("/application/*")
public class UserServlet extends HttpServlet {
    private Logger logger = Logger.getLogger(UserServlet.class.getName());
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.info("Receivied the post request in login");
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.info("Receivied the get request in login as called");

        UserDAO dao = new UserDAO();
        String path = req.getPathInfo();

        logger.info(path);


        System.out.println(path + "------------");

        if (path == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        String[] paths = path.split("/");

        System.out.println(paths[1] + "---------");

        logger.info("path index 1 value " + paths[1] );
        if (paths[1].equals("login")) {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            int user_id = dao.validateUser(username, password);
            if (user_id!=0) {
                //login successfully
                HttpSession session = req.getSession(true);

                // Synchronized session key name with MessageServlet
                session.setAttribute("username", username);
                session.setAttribute("Name", username);
                session.setAttribute("user_id", user_id);

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