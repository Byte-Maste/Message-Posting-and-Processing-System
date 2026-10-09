<%--
  Created by IntelliJ IDEA.
  User: krishna-pt8304
  Date: 10/8/2026
  Time: 4:27 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <title>Title</title>
  </head>
  <body>
  <body>
       <form action="${pageContext.request.contextPath}/application/register" method="post" >
            <label>Name:</label>
            <input type="text" name="username"><br><br>
            <label>Password</label>
            <input type="text" name="password"><br><br>
            <button type="submit">Submit</button>
       </form>
  </body>
</html>
