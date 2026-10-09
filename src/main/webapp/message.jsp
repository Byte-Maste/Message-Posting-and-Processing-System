<%--
  Created by IntelliJ IDEA.
  User: krishna-pt8304
  Date: 10/8/2026
  Time: 4:30 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <title>Message Dashboard</title>
  </head>
  <body>
     <form action="${pageContext.request.contextPath}/message" method="post" >
     <label>UserContent</label>
     <input type="text" name="Content"><br><br>
     <label>Type</label>
     <input type="text" name="Type">
     <label>Priority</label>
     <input type="text" name="priority">
     <button type="submit">Submit</button>
     </form>
  </body>
</html>
