## 修改java_home环境变量jdk版本没变化问题

C:\Windows\System32文件夹下删除以下文件java javac javaw



## javac命令编译`.java`文件

```shell
C:\Users\Administrator>javac C:/Users/Administrator/Desktop/java/Demo1.java
```

设置`classpath`路径

```shell
C:\Users\Administrator>set classpath=C:/Users/Administrator/Desktop/java
```

设置依赖包所在路径并执行(有时会出问题)

```shell
java -Djava.ext.dirs=.\lib com.he_zhw.JDBC.JDBCDemo
```

建议使用

```shell
java -cp .;./lib/*  com.he_zhw.JDBC.C3P0Demo
```

# Idea导出依赖包

```shell
dependency:copy-dependencies -DoutputDirectory=lib（备注：lib目录是指导出的文件夹名称）
```



# jdbc连接数据库步骤

1. 加载驱动(使用静态代码块加载驱动)

   ```java
       static {
           try {
               Class.forName("com.mysql.jdbc.Driver");
           } catch (ClassNotFoundException e) {
               e.printStackTrace();
           }
       }
   ```

   

2. 建立连接

   ```java
   connection = DriverManager.getConnection("jdbc:mysql://192.168.19.130:3306/bookstores", "root", "root");
   ```

   

3. 获得statement对象并执行语句

   ```java
   statement = connection.createStatement();
   resultSet = statement.executeQuery("SELECT *FROM student");
   while (resultSet.next()) {
       System.out.println(resultSet.getObject(1));
   }
   ```

   使用prepareStatement防止sql注入

   ```java
   statement=connection.prepareStatement("SELECT *FROM student where id=?");
   statement.setInt(1,3);
   resultSet=statement.executeQuery();
   while (resultSet.next()) {
       System.out.println(resultSet.getObject(1));
   }
   ```

   

4. 释放资源

```java
finally {
    if (resultSet!=null) {
                try {
                    resultSet.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if (statement!=null) {
                try {
                    statement.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
                if (connection!=null) {
                try {
                    connection.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
```

# Xml与Tomcat

XML：eXtensible Markup Language 可扩展标记语言 version="1.0"
	* 可扩展：所有的标签都是自定义的。  
	* 功能：数据存储
		* 配置文件
		* 数据传输

	* html与xml区别：
		* html语法松散，xml语法严格
		* html做页面展示，xml做数据存储
		* html所有标签都是预定义的，xml所有标签都是自定义的
	
	W3C:word wide web consortiem  万维网联盟
	xml语法：


		* 文档声明：
			* 必须写在xml文档的第一行。
			* 写法：<?xml version="1.0" ?>
			* 属性：	
				* version：版本号 固定值 1.0
				* encoding:指定文档的码表。默认值为 iso-8859-1
				* standalone：指定文档是否独立  yes 或 no
	
		* 元素：xml文档中的标签
			** 文档中必须有且只能有一个根元素
			* 元素需要正确闭合。<body></body> <br/>
			* 元素需要正确嵌套
			* 元素名称要遵守：
				* 元素名称区分大小写
				* 数字不能开头
		
		* 文本：
			* 转义字符：&gt;
			* CDATA: 里边的数据会原样显示
				*  <![CDATA[ 数据内容 ]]>
				
		* 属性：
			* 属性值必须用引号引起来。单双引号都行
		* 注释：
			<!-- -->
		* 处理指令：现在基本不用
			<?xml-stylesheet type="text/css" href="1.css"?>
			
	xml约束：
		* 约束就是xml的书写规则
		* 约束的分类：
			dtd：
				dtd分类：
					* 内部dtd：在xml内部定义dtd
					* 外部dtd：在外部文件中定义dtd
						* 本地dtd文件：<!DOCTYPE students SYSTEM  "student.dtd">
						* 网络dtd文件：<!DOCTYPE students PUBLIC "名称空间"  "student.dtd">
			schema：
				导入xsd约束文档：
						1、编写根标签
						2、引入实例名称空间 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
						3、引入名称空间 xsi:schemaLocation="http://www.itcast.cn/xml student.xsd"	
						4、引入默认的名称空间

 [Tomcat服务器&http笔记.doc](doc\Tomcat服务器&http笔记.doc) 

## Dom4J读写Xml文件

导入jar包`dom4j-1.6.1.jar`

```java
package com.he_zhw.Dom4J;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.util.List;

public class Dom4JDemo {
    public static void main(String[] args) {
        SAXReader saxReader = new SAXReader();
        try {
            Document read = saxReader.read("student.xml");
            Element rootElement = read.getRootElement();//获取根节点
            List<Attribute> attributes = rootElement.attributes();//获取根节点下的所有属性
            //遍历属性
            for (Attribute attribute : attributes) {
                System.out.println(attribute.getValue());
            }
            List<Element> elements = rootElement.elements();
            for (Element element : elements) {
                System.out.println(element.getName());
            }
        } catch (DocumentException e) {

            e.printStackTrace();
        }
    }
}

```

如果要使用`xpath`还需要引用`jaxen-1.1-beta-6.jar`

```java
package com.he_zhw.Dom4J;

import org.dom4j.*;
import org.dom4j.io.SAXReader;

import java.util.ArrayList;
import java.util.List;

public class Dom4JDemo {
    public static void main(String[] args) {
        SAXReader saxReader = new SAXReader();
        try {
            Document read = saxReader.read("./student.xml");
            Node node = read.selectSingleNode("/student/hobbies");
            Element nodeElement = (Element) node;
            getElement(nodeElement);

        } catch (DocumentException e) {

            e.printStackTrace();
        }
    }


    /**
     * 输出该节点的子节点和子孙节点
     *
     * @param element
     */
    private static void getElement(Element element) {
        List<Element> elements = element.elements();
        for (Element element1 : elements) {
            getElement(element1);
        }
        System.out.println(element.getName());
    }
}

```

写入xml文件

```java
package com.he_zhw.Dom4J;

import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Dom4JDemo {
    public static void main(String[] args) {
        FileWriter fileWriter=null;
        try {
            fileWriter= new FileWriter("Person.xml");
            Document document = DocumentHelper.createDocument();
            Element personRoot = document.addElement("Person");
            Element idNo = personRoot.addElement("IdNo");
            idNo.addText("452133199503192112");
            document.write(fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (fileWriter!=null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }



```

格式化写入的xml文件

```java
package com.he_zhw.Dom4J;

import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Dom4JDemo {
    public static void main(String[] args) {
        XMLWriter xmlWriter=null;
        try {
            OutputFormat format = OutputFormat.createPrettyPrint();
            xmlWriter=new XMLWriter(new FileOutputStream("Person.xml"),format);
            Document document = DocumentHelper.createDocument();
            Element personRoot = document.addElement("Person");
            Element idNo = personRoot.addElement("IdNo");
            idNo.addText("452133199503192112");
            xmlWriter.write(document);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (xmlWriter!=null) {
                try {
                    xmlWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
```

## 重新设置标准打印流,让信息往指定地方输出

```java
package com.he_zhw.IODemo;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StreamDemo {
    public static void main(String[] args) {
        PrintStream printStream = null;
        try {
            printStream = new PrintStream("text.txt");
            System.setOut(printStream);//重新设置标准打印流,让信息往指定地方输出
            int i = 1 / 0;
        } catch (FileNotFoundException e) {
            e.printStackTrace(System.out);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        } finally {
            if (printStream != null) {
                printStream.close();
            }
        }
    }

}
```

# 转换流

将字节流转换成字符流

# Servlet映射配置

（后续可通过注解映射，不用配置web.xml）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">
    <servlet>
        <servlet-name>myServerlet</servlet-name>
        <!-- Servlet所在的类名 -->
        <servlet-class>com.example.javaEEDemo.Servlet</servlet-class>
    </servlet>

    <!-- 添加映射 -->
    <servlet-mapping>
        <servlet-name>myServerlet</servlet-name>
        <!-- uri -->
        <url-pattern>/demo1</url-pattern>
    </servlet-mapping>
</web-app>
```

# Servlet生命周期

##  实例化-->初始化（init）-->服务（service）->销毁（destroy）

1. 实例化和初始化默认情况下第一次访问Servlet就调用，可以配置成应用一启动就创建实例
2. 每次访问都会调用service方法
3. 应用卸载调用destroy方法

# ServletContext域对象

一个应用就一个ServletContext对象

```java
ServletContext servletContext = this.getServletContext();
servletContext.getInitParameter("Encode");//获取全局参数
servletContext.setAttribute("","");//设置域数据
servletContext.getAttribute("");//获取域数据
servletContext.getRealPath("/Servlet");//获取应用下资源的真实路径,参数一定要以/开头
servletContext.getRequestDispatcher("路径").forward(request,response);//请求转发
```

# HttpServletResponse对象

- 同一个Servlet不能同时使用getOutputStream()和getWriter(),否则会抛异常
- Servlet中不用手动关io流，Servlet在调用完之后会检查流是否有关闭,如果没有关闭，则调用Close()方法

```java
        //ServletOutputStream outputStream = response.getOutputStream();//获得输出字节流
        response.setContentType(“text/html;charset=UTF-8”);//对setCharacterEncoding和的封装
        response.setCharacterEncoding("UTF-8");//设置服务端用什么编码解析,tomcat服务器默认ISO-8859-1编码,不支持中文
        response.setHeader("content-type", "text/html;charset=UTF-8");//设置客户端编码
        PrintWriter writer = response.getWriter();//获得字符输出流
        response.sendRedirect("路径");//请求重定向
        //重定向原始代码
        /*response.setStatus(302);
		response.setHeader("location", "/day09_00_HttpServletResponse/servlet/demo8");*/
```

设置浏览器不使用缓存

```java
response.setHeader("pragma", "no-cache");
response.setHeader("cache-control", "no-cache");
response.setDateHeader("expires", 0);
```

文件下载

```java
ServletContext servletContext = this.getServletContext();
        String realPath = servletContext.getRealPath("/img.png");
        FileInputStream fileInputStream = new FileInputStream(realPath);
        ServletOutputStream outputStream = response.getOutputStream();
        byte[] bytes = new byte[1024 * 5];
        response.setHeader("content-Disposition", "attachment; filename="
                + "img.png");
        response.setHeader("content-type", "image/jpeg");
        int len=0;
        while ((len=fileInputStream.read(bytes)) != -1) {
            outputStream.write(bytes,0,len);
        }
```

自动刷新跳转

```java
package com.example.javaEEDemo;

import java.io.*;
import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

@WebServlet(name = "helloServlet", value = "/hello-servlet")
public class HelloServlet extends HttpServlet {
    private String message;

    public void init() {
        message = "Hello World!";
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        // Hello
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>" + message + "</h1>");
        out.println("</body></html>");
        out.write("注册成功！3秒钟跳到主页");
        response.setHeader("content-type", "text/html;charset=UTF-8");//设置客户端编码
        //设置3秒钟跳转
        String contextPath = this.getServletContext().getContextPath();
        String url=contextPath+"/Servlet";
        response.setHeader("refresh", "3;url="+url);
    }

    public void destroy() {
    }
}
```



## 生成验证码示例

添加jar包`ValidateCode.jar`

```java
        ValidateCode validateCode = new ValidateCode(115,25,4,10);
        validateCode.write(response.getOutputStream());
```

# HttpServletRequest对象(域对象)

常用方法

```java
response.setCharacterEncoding("UTF-8");//设置服务端用什么编码解析,tomcat服务器默认ISO-8859-1编码,不支持中文
PrintWriter writer = response.getWriter();
response.setHeader("content-type", "text/html;charset=UTF-8");//设置客户端编码
writer.println("<h1>" +request.getMethod()+ "</h1>");//得到请求方法
writer.println("<h1>" +request.getContextPath()+ "</h1>");//获取应用路径，前面带/
writer.println("<h1>" +request.getRequestURI()+ "</h1>");//获取uri
writer.println("<h1>" +request.getRequestURL()+ "</h1>");//获取url
writer.println("<h1>" +request.getQueryString()+ "</h1>");//得到get参数部分
writer.println("<h1>" +request.getParameter("name")+ "</h1>");//根据参数名获得参数值
```

\* String  getHeader(String name)  根据头名称得到头信息值

 Enumeration  getHeaderNames()  得到所有头信息name

 Enumeration  getHeaders(String name)  根据头名称得到相同名称头信息值

*** getParameter(name) 根据表单中name属性的名，获取value属性的值方法 

*** getParameterValues（String name）专业为复选框取取提供的方法

​        getParameterNames() 得到表单提交的所有name的方法 

*** getParameterMap 到表单提交的所有值的方法  //做框架用，非常实用

getInputStream  以字节流的方式得到所有表单数据

## getParameterMap 封装javabean

导入jar包`commons-beanutils-1.8.3.jar`和`commons-logging-1.1.1.jar`

```html
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title>Title</title>
</head>
<body>
<form action="/javaEEDemo_war/RequestServlet" method="post">
用户名:<input name="username" type="text"><br>
密码:<input name="password" type="password"><br>
性别:<input type="radio"  name="sex" value="male">男
    <input type="radio"  name="sex" value="female">女<br>
    爱好:<input name="hobbies" type="checkbox" value="篮球">篮球
    <input name="hobbies" type="checkbox" value="唱歌">唱歌
    <input name="hobbies" type="checkbox" value="代码"><br>
    所在城市:<select name="city">
    <option>北京</option>
    <option>上海</option>
    <option>南宁</option>
</select>
    <input name="register" value="注册" type="submit">
</form>
</body>
</html>
```



```java
package com.he_zhw.javabean;

public class userbean {
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private String username;



    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String[] getHobbies() {
        return hobbies;
    }

    public void setHobbies(String[] hobbies) {
        this.hobbies = hobbies;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    private String sex;
    private String[] hobbies;
    private String city;
}

```

```java
package com.example.javaEEDemo;

import com.he_zhw.javabean.userbean;
import org.apache.commons.beanutils.BeanUtils;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

@WebServlet(name = "RequestServlet", value = "/RequestServlet")
public class RequestServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        userbean userbean = new userbean();
        try {
            BeanUtils.populate(userbean,request.getParameterMap());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }
}
```



## 解决请求编码问题

***\*//解决post方式编码\****

*****request.setCharacterEncoding("UTF-8"); //告诉服务器客户端什么编码,只能处理post请求方式

 

//解决get方式编码

String name = new String(name.getBytes(“iso-8859-1”),”UTF-8”);

# Cookie

- name：名称不能唯一确定一个Cookie。路径可能不同。

- value：不能存中文。

- path：默认值是写Cookie的那个程序的访问路径

比如：http://localhost:8080/day10_00_cookie/servlet/ck1写的Cookie

path就是：/day10_00_cookie/servlet 看当前创建cookie的资源（servlet）文件路径 

客户端在访问服务器另外资源时，根据访问的路径来决定是否带着Cookie到服务器

当前访问的路径如果是以cookie的path开头的路径，浏览器就带。否则不带。

 

- MaxAge()：cookie的缓存时间。默认是-1（默认存在浏览器的内存中）。单位是秒。

负数：cookie的数据存在浏览器缓存中

0：删除。路径要保持一致，否则可能删错人。

正数：缓存（持久化到磁盘上）的时间

```java
package com.itheima.demo;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CookieDemo1 extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();
			//获取客户端保存的最后访问时间
			Cookie[] cookies = request.getCookies();//获取客户端的所有Cookie对象
			for (int i = 0;cookies!=null && i < cookies.length; i++) {
				if("lastAccessTime".equals(cookies[i].getName())){//判断当前Cookie中的name是否是想要的cookie
					long l = Long.parseLong(cookies[i].getValue());//如果是想要的Cookie，则把Cookie中的value取出
					out.write("你的最后访问时间为："+new Date(l).toLocaleString());//yyyy-MM-dd
				}
			}
		
			out.print("<a href='"+request.getContextPath()+"/servlet/clear'>clear</a>");
			//创建cookie，
			Cookie ck = new Cookie("lastAccessTime",System.currentTimeMillis()+"");
			//设置cookie的有效时间,单位是秒
			ck.setMaxAge(60*5);//保存时间为5分钟
			//设置cookie的path
			//ck.setPath("/day10_00_cookie");
			//ck.setPath(request.getContextPath());//  /day10_00_cookie
			ck.setPath("/");//  /day10_00_cookie
			//把cookie信息写回到客户端
			response.addCookie(ck);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}

```

# HttpSession

## ***\*2、常用方法\****

把数据保存在HttpSession对象中，该对象也是一个域对象。

void setAttribute(String name,Object value);

Object getAttribute(String name);

void removeAttribute(String name);

HttpSession.getId():

 

setMaxInactiveInterval(int interval)  设置session的存活时间

### invalidate() 使此会话无效

### ***\*3、getSession():内部执行原理\****

HttpSession request.getSession():内部执行原理

1、获取名称为JSESSIONID的cookie的值。

2、没有这样的cookie，创建一个新的Ht***\*t\****pSession对象，分配一个唯一的SessionID，并且向客户端写了一个名字为JSESSIONID=sessionID的cookie

3、有这样的Cookie，获取cookie的值（即HttpSession对象的值），从服务器的内存中根据ID找那个HttpSession对象：

找到了：取出继续为你服务。

找不到：从2开始。

 

 

HttpSession request.getSession(boolean create):

参数：

true：和getSession()功能一样。

false：根据客户端JSESSIONID的cookie的值，找对应的HttpSession对象，找不到返回null（不会创建新的，只是查询）。

## 服务器关闭时服务器会将数据存到磁盘中

`E:\apache-tomcat-7.0.52\work\Catalina\localhost\javaEEDemo_war`

## ***\*3、客户端禁用Cookie后的会话数据保存问题\****

客户端禁用cookie：浏览器永远不会向服务器发送cookie的请求消息头

 

解决方案：

方案一：在主页上给出提示：请不要禁用您的cookie

方案二：URL重写。必须对网站的所有地址都重写。

 

http://url--->http://url;JSESSIONID=111

 quest.getSession();必须写

response.encodeURL(String url);

看浏览器有没有发送cookie请求消息头，没有就重写URL，有就不重写。



  

# JSP



 [JSP笔记.doc](doc\JSP笔记.doc) 

# DBCP连接池连接MySql

导入jar包`commons-dbcp-1.4.jar`、`commons-pool-1.5.6.jar`

将配置文件dbcpconfig.properties放到src文件夹底下

```java
package com.he_zhw.JDBC;

import org.apache.commons.dbcp.BasicDataSourceFactory;

import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class DBCPDemo {
    static DataSource ds=null;
    static {
        Properties properties = new Properties();
        try {
            properties.load(DBCPDemo.class.getClassLoader().getResourceAsStream("dbcpconfig.properties"));
            ds = BasicDataSourceFactory.createDataSource(properties);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static void main(String[] args) {
        Connection connection=null;
        PreparedStatement preparedStatement=null;
        ResultSet resultSet=null;
        try {
             connection = ds.getConnection();
             preparedStatement = connection.prepareStatement("SELECT *FROM student t WHERE t.name=?");
            preparedStatement.setString(1,"张小明");
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                System.out.println(resultSet.getObject(1));
                System.out.println(resultSet.getObject(2));
                System.out.println(resultSet.getObject(3));
                System.out.println(resultSet.getObject(4));
                System.out.println(resultSet.getObject(5));
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        finally {
            if (resultSet!=null) {
                try {
                    resultSet.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if (preparedStatement!=null) {
                try {
                    preparedStatement.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if (connection!=null) {
                try {
                    connection.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
    }
}

```

# C3P0连接池连接MySql

导入jar包`c3p0-0.9.1.2.jar`

将配置文件`c3p0-config.xml`(文件名是固定的)加入到`classpath`目录下,web项目放在src文件夹下

```java
package com.he_zhw.JDBC;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.sun.org.apache.bcel.internal.generic.NEW;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class C3P0Demo {
    private  static DataSource ds= new ComboPooledDataSource();
    public static void main(String[] args) {
        Connection connection=null;
        PreparedStatement preparedStatement=null;
        ResultSet resultSet=null;
        try {
            connection = ds.getConnection();
            preparedStatement = connection.prepareStatement("select *from student t where t.name=?");
            preparedStatement.setString(1,"李进");
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                System.out.println(resultSet.getObject(1));
                System.out.println(resultSet.getObject(2));
                System.out.println(resultSet.getObject(3));
                System.out.println(resultSet.getObject(4));
                System.out.println(resultSet.getObject(5));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }finally {
            if (resultSet!=null) {
                try {
                    resultSet.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            while (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            while (connection != null) {
                try {
                    connection.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
    }
}

```

# 设置idea自动将项目部署到tomcat的webapps目录下

将以下移除重新添加

![图片7](E:\学习笔记\java\笔记\image\图片7.png)

# JNDI使用DBCP连接Mysql

- 将MySql的jar包`mysql-connector-java-5.0.8-bin.jar`放到tomcat目录的lib文件夹下

方式一:

新建`context.xml`文件放在项目的META-INF文件夹下面

 ![图片8](E:\学习笔记\java\笔记\image\图片8.png)

配置如下

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Context>
     <Resource name="My_JNDI" auth="Container" type="javax.sql.DataSource"
               maxActive="100" maxIdle="30" maxWait="10000"
               username="root" password="root" driverClassName="com.mysql.jdbc.Driver"
               url="jdbc:mysql://192.168.19.130:3306/bookstores"/>
   
</Context>
```





方式二:

tomcat根目录下的`conf`文件夹下的`context.xml`添加以下配置

![图片10](E:\学习笔记\java\笔记\image\图片10.png)

代码实现:

```java
package com.example.jndidemo;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import javax.sql.DataSource;

@WebServlet(name = "helloServlet", value = "/hello-servlet")
public class HelloServlet extends HttpServlet {
    private String message;

    public void init() {
        message = "Hello World!";
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        Connection connection=null;
        try {
            Context xt=new InitialContext();
            DataSource ds = (DataSource)xt.lookup("java:/comp/env/My_JNDI");
            try {
                connection = ds.getConnection();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            PrintWriter out = response.getWriter();
            out.println(ds);
            out.println(connection);

        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
    }
}
```

# JNDI使用C3P0连接MySql

- 将c3p0相关jar包及MySql的Jar包放入tomcat根目录的lib文件夹下
  - `c3p0-0.9.5.5.jar`
  - `mchange-commons-java-0.2.19.jar`
  - `mysql-connector-java-5.1.44.jar`

- tomcat根目录下的`conf`文件夹下的`context.xml`添加以下配置

  ```xml
    <Resource
   name="My_JNDI"
   auth="Container"
   factory="org.apache.naming.factory.BeanFactory"
   type="com.mchange.v2.c3p0.ComboPooledDataSource"
   driverClass="com.mysql.jdbc.Driver"
   idleConnectionTestPeriod="60"
   maxPoolSize="50"
   minPoolSize="2"
   acquireIncrement="2"
   user="root"
   password="root"
   jdbcUrl="jdbc:mysql://192.168.19.130:3306/bookstores"/>
  
  ```

- 代码实现

  ```java
  package com.example.jndidemo;
  
  import java.io.*;
  import java.sql.Connection;
  import java.sql.PreparedStatement;
  import java.sql.ResultSet;
  import java.sql.SQLException;
  import javax.naming.Context;
  import javax.naming.InitialContext;
  import javax.naming.NamingException;
  import javax.servlet.http.*;
  import javax.servlet.annotation.*;
  import javax.sql.DataSource;
  
  @WebServlet(name = "helloServlet", value = "/hello-servlet")
  public class HelloServlet extends HttpServlet {
      private String message;
  
      public void init() {
          message = "Hello World!";
      }
  
      public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
          /*response.setContentType("text/html");
  
          // Hello
          PrintWriter out = response.getWriter();
          out.println("<html><body>");
          out.println("<h1>" + message + "</h1>");
          out.println("</body></html>");*/
          response.setContentType("text/html");
          Connection connection=null;
          PreparedStatement preparedStatement=null;
          ResultSet resultSet=null;
          try {
              Context xt=new InitialContext();
              DataSource ds = (DataSource)xt.lookup("java:/comp/env/My_JNDI");
              try {
                  connection = ds.getConnection();
                  preparedStatement = connection.prepareStatement("SELECT *FROM student t WHERE t.id=?");
                  preparedStatement.setInt(1,1);
                  resultSet = preparedStatement.executeQuery();
                  while (resultSet.next()) {
                      System.out.println(resultSet.getObject(1));
                      System.out.println(resultSet.getObject(2));
                      System.out.println(resultSet.getObject(3));
                      System.out.println(resultSet.getObject(4));
                      System.out.println(resultSet.getObject(5));
                  }
              } catch (SQLException throwables) {
                  throwables.printStackTrace();
              }
              finally {
                  try {
                      connection.close();
                  } catch (SQLException throwables) {
                      throwables.printStackTrace();
                  }
              }
              PrintWriter out = response.getWriter();
              //out.println(ds);
              //out.println(connection);
  
          } catch (NamingException e) {
              e.printStackTrace();
          }
      }
  
      public void destroy() {
      }
  }
  ```

# DBUtils封装数据库

- maven导入相关jar包

```xml
<dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>5.1.44</version>
    </dependency>
    <dependency>
        <groupId>commons-dbutils</groupId>
        <artifactId>commons-dbutils</artifactId>
        <version>1.7</version>
    </dependency>
```

## 代码示例

```java
package com.he_zhw.DBUtilsDemo;

import com.he_zhw.domain.User;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.sun.xml.internal.bind.v2.model.core.ID;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DBUtilsDemo {
    static DataSource ds=new ComboPooledDataSource();//c3p0连接池
    public static void main(String[] args) {

        Connection connection=null;
        PreparedStatement preparedStatement=null;
        ResultSet resultSet=null;
        try {
            QueryRunner queryRunner = new QueryRunner(ds);
            /*User query = queryRunner.query("SELECT *FROM student t WHERE t.id=?", new ResultSetHandler<User>() {

                @Override
                public User handle(ResultSet resultSet) throws SQLException {
                    User user = new User();
                    while (resultSet.next()) {
                        user.setId(resultSet.getInt(1));
                        user.setName(resultSet.getString(2));
                        user.setChinese(resultSet.getFloat(3));
                        user.setEnglish(resultSet.getFloat(4));
                        user.setMath(resultSet.getFloat(5));
                    }
                    return user;
                }
            },1);*/

            /**
             *查询一行，并将每一列的值存到数组中
             */
/*            Object[] query = queryRunner.query("SELECT *FROM student t", new ArrayHandler());
            System.out.println(Arrays.toString(query));*/


/*            List<Object[]> query = queryRunner.query("SELECT *FROM student t", new ArrayListHandler());
            for (Object[] objects : query) {
                System.out.println(Arrays.toString(objects));
            }*/

            /*User query = queryRunner.query("SELECT *FROM student t WHERE t.id=?", new BeanHandler<User>(User.class),1);
            System.out.println(query.toString());*/

/*            List<User> query = queryRunner.query("SELECT *FROM student t", new BeanListHandler<User>(User.class));
            for (User user : query) {
                System.out.println(user.toString());
            }*/

/*            Map<Integer, User> id = queryRunner.query("SELECT *FROM student t", new BeanMapHandler<Integer, User>(User.class, "id"));
            for (Map.Entry<Integer, User> stringUserEntry : id.entrySet()) {
                System.out.println(stringUserEntry.getKey());
                System.out.println(stringUserEntry.getValue().toString());
            }*/

/*            List<Integer> query = queryRunner.query("SELECT *FROM student t", new ColumnListHandler<Integer>("id"));
            for (Integer integer : query) {
                System.out.println(integer.intValue());
            }*/

/*            Map<Integer, Map<String, Object>> query = queryRunner.query("SELECT *FROM student t", new KeyedHandler<Integer>(1));
            for (Integer integer : query.keySet()) {
                for (Map.Entry<String, Object> stringObjectEntry : query.get(integer.intValue()).entrySet()) {
                    System.out.println(stringObjectEntry.getValue().toString());
                }
            }*/

            //返回一行
/*            Map<String, Object> query = queryRunner.query("SELECT *FROM student t", new MapHandler());
            for (Map.Entry<String, Object> stringObjectEntry : query.entrySet()) {
                System.out.println(stringObjectEntry.getValue());
            }*/

/*            List<Map<String, Object>> query = queryRunner.query("SELECT *FROM student t", new MapListHandler());
            for (Map<String, Object> stringObjectMap : query) {
                for (Map.Entry<String, Object> stringObjectEntry : stringObjectMap.entrySet()) {
                    System.out.println(stringObjectEntry.getValue());
                }
            }*/

            /*Integer query = queryRunner.query("SELECT *FROM student t", new ScalarHandler<Integer>(1));
            System.out.println(query.intValue());*/
            Object[][] objects = new Object[3][];
            for (int i = 0; i < objects.length; i++) {
                objects[i]=new Object[]{50,i+1};
            }
            int[] batch = queryRunner.batch("UPDATE student t SET t.math=? WHERE t.id=?", objects);

        } catch (Exception throwables) {
            throwables.printStackTrace();
        }
        finally {
            if (resultSet!=null) {
                try {
                    resultSet.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
                ;
            }
            if (preparedStatement!=null) {
                try {
                    preparedStatement.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if (connection!=null) {
                try {
                    connection.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
    }
}

```



# Listener

略

# Filter

略

# 文件上传

## 文件上传先决条件

1. 请求方式为Post
2. form表单的enctype必须是multipart/form-data
3. 提供input type="file"类的上传输入域

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>
<form method="post" action="/jndidemo_war/FileUploadServlet" enctype="multipart/form-data">

    <input type="text" name="userName">
    <input type="file" name="fileupload">
    <input type="submit" value="提交">
</form>
</body>
</html>
```

## 服务端接收实现

- maven导入相应依赖包(包含`commons-fileupload-1.4.jar`和`commons-io-2.2.jar`)
- 官方文档连接http://commons.apache.org/proper/commons-fileupload/javadocs/api-release/index.html

```xml
<dependency>
    <groupId>commons-fileupload</groupId>
    <artifactId>commons-fileupload</artifactId>
    <version>1.4</version>
</dependency>
```

- 代码实现

```java
package com.example.jndidemo;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.commons.io.FilenameUtils;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@WebServlet(name = "FileUploadServlet", value = "/FileUploadServlet")
public class FileUploadServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletRequestContext servletRequestContext = new ServletRequestContext(request);
        //判断是否是文件传输
        if (FileUpload.isMultipartContent(servletRequestContext)) {
            DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
            diskFileItemFactory.setRepository(new File("E:\\"));//设置临时文件所在位置
            ServletFileUpload servletFileUpload = new ServletFileUpload(diskFileItemFactory);
            servletFileUpload.setHeaderEncoding(StandardCharsets.UTF_8.toString());//解决文件名乱码
            try {
                List<FileItem> fileItems = servletFileUpload.parseRequest(request);
                for (FileItem fileItem : fileItems) {
                    //是否是普通文本域
                    if (fileItem.isFormField()) {
                        System.out.println(fileItem.getFieldName());
                        System.out.println(fileItem.getString(StandardCharsets.UTF_8.toString()));//解决普通文本域中文乱码
                    }else{
                        saveFile(fileItem);
                    }
                }
            } catch (FileUploadException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveFile(FileItem fileItem) {
        String fieldName = fileItem.getName();
        if (fieldName!=null) {
            fieldName=FilenameUtils.getName(fieldName);
        }
        InputStream inputStream=null;
        FileOutputStream fileOutputStream=null;
        File file1=null;
        try {
            inputStream = fileItem.getInputStream();
            File file = new File(this.getServletContext().getRealPath("WEB-INF/upLoad"));
            if (!file.exists()) {
                file.mkdir();
            }
            file1 = new File(file, fieldName);
/*            fileOutputStream = new FileOutputStream(file1);
            int len=0;
            byte[] bytes = new byte[1024];
            while ((len = inputStream.read(bytes)) != -1) {
                fileOutputStream.write(bytes,0,len);
            }*/

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (fileOutputStream!=null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream!=null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                fileItem.write(file1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //fileItem.delete();//删除临时文件,要在关闭流之后执行
        }
    }
}
```

# 文件下载

```java
package com.example.jndidemo;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;

@WebServlet(name = "FileDownLoadServlet", value = "/FileDownLoadServlet")
public class FileDownLoadServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = this.getServletContext().getRealPath(
                "/WEB-INF/云影像.txt");
        File file = new File(path);
        String filename = file.getName();
        filename= URLEncoder.encode(filename, "UTF-8");//中文名字转换，否则产生乱码
        response.setHeader("content-Disposition", "attachment; filename="
                + filename);
        response.setHeader("content-type", this.getServletContext().getMimeType(filename));


        FileInputStream fs = new FileInputStream(file);

        ServletOutputStream sos = response.getOutputStream();

        int len = 1;
        byte[] bytes = new byte[1024];
        while ((len = fs.read(bytes)) != -1) {
            sos.write(bytes, 0, len);
        }
        sos.close();
        fs.close();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}
```

# 代理模式

## 静态代理

   略

## 动态代理

- ### jdk动态代理

```java
package com.he_zhwAOPDemo;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

public class AOPDemoTest {
    @Test
    public void AOPTest() {
        AOPTest aopDemo = new AOPDemo();
        AOPTest o = (AOPTest)Proxy.newProxyInstance(aopDemo.getClass().getClassLoader(), aopDemo.getClass().getInterfaces(), new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                System.out.println("执行前.....");
                method.invoke(aopDemo, null);
                System.out.println("执行后.....");
                return null;
            }
        });
        o.doSomesing();
    }
}
```



- ## cglib动态代理(Spring学到)



# 注解

## 基本注解

- @Override：检查子类确实是覆盖了父类的方法。

- @Deprecated：说明已经过时了。

- @SuppressWarnings({ "unused", "deprecation" })：抑制程序中的警告。unused警告的类型。{}数组。all抑制所有警告。



## 元注解

只能用在注解上的注解叫做元注解。（即：用于修饰注解的注解）

-  @Retention：作用。改变自定义的注解的存活范围。

RetentionPolicy:

SOURCE

CLASS

RUNTIME

- @Target:作用，指定该注解能用在什么地方。

ElementType：

TYPE：

METHOD：

FIELD：

ANNOTATION_TYPE

- @Documented：作用，使用了@MyTest的注解的类，如果@MyTest注解上面有@Documented注解，那么使用了@MyTest的注解的类的API文档中会出现@MyTest的身影。

- @Inherited：作用，说明该注解可以被继承下去。

## 自定义注解

### 语法

public @interface MyAnnotation{}

​	

注解它的本质就是一个接口，这个接口需要继承 Annotation接口。

public interface MyAnnotation extends java.lang.annotation.Annotation {

}

### 注解的属性类型可以有哪些?

​	1.基本类型

​	2.String

​	3.枚举类型

​	4.注解类型

​	5.Class类型

​	6.以上类型的一维数组类型

### 通过反射判断是否有注解

-  <T extends Annotation> T getAnnotation(Class<T> annotationType):得到指定类型的注解引用。没有返回null。

-  Annotation[] getAnnotations()：得到所有的注解，包含从父类继承下来的。

- lAnnotation[] getDeclaredAnnotations()：得到自己身上的注解。

-  boolean isAnnotationPresent(Class<? extends Annotation> annotationType)：判断指定的注解有没有。

 

- Class、Method、Field、Constructor等实现了AnnotatedElement接口.

​      如果：Class.isAnnotationPresent(MyTest.class):判断类上面有没有@MyTest注解；

​      Method.isAnnotationPresent(MyTest.class):判断方法上面有没有@MyTest注解。

### 代码示例

```java
package com.he_zhw;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AnnotationTest {

    public static void main(String[] args) {
        Class<AnnotationTest> annotationTestClass = AnnotationTest.class;
        Method[] methods = annotationTestClass.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(AnnotationDemo.class)) {
                AnnotationDemo annotation = method.getAnnotation(AnnotationDemo.class);
                String name = annotation.name();
                try {
                    method.invoke(annotationTestClass.newInstance(),name);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @AnnotationDemo
    public void  Test(String name){
        System.out.println(name);
    }
}
```

# 类加载器

- **作用：负责把磁盘上的class文件加载到JVM中，Class引用字节码**

- JVM中的类加载器：

​       **BootStrap：老大。类加载器的祖先。 打印它会得到null。**

​             负责加载JRE/lib/rt.jar(JDK中绝大部分的类)

​       **ExtClassLoader：**

​            负责加载JRE/lib/ext/*.jar

​      **AppClassLoader：**

​            负责加载在classpath环境变量中的所有类。

- 父类委托机制

![图片11](E:\学习笔记\java\笔记\image\图片11.png)

# Struts2框架





## 注:struts2标签中不能使用EL表达式,否则会报错

# Hibernate

## 注：使用getCurrentSession()需要先配置,并且必须手动开启事务,否则查询也会报错



## 使用Idea调试延迟加载时特别注意要先把自动调用to_String()的配置去掉,否则没法体现延迟加载

![图片12](E:\学习笔记\java\笔记\image\图片12.png)

# Spring

注意事项:AOP中如果实现类实现了某个接口,则默认使用jdk代理,使用getbean返回值用接口类型接收时会报错



