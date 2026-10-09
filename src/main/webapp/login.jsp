<%--
  Created by IntelliJ IDEA.
  User: krishna-pt8304
  Date: 10/9/2026
  Time: 3:30 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <title>Login JSP</title>
  </head>
  <body>
     <form action="${pageContext.request.contextPath}/application/login" method="post" >
          <label>Name:</label>
          <input type="text" name="username"><br><br>
          <label>Password</label>
          <input type="text" name="password"><br><br>
          <button type="submit">Submit</button>
     </form>
  </body>
</html>
