package ai.ocs.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.blade.kit.base.Config;

public class DBHelper {
  private String url = "";// "jdbc:mysql://192.168.1.72/bill";
  private String name = "com.mysql.jdbc.Driver";
  private String user = "";
  private String password = "";

  public static Connection conn = null;

  public DBHelper() {
    if (conn == null)
      try {
        Config config = Config.load("classpath:config.properties");
        url = config.get("db_url");
        name = config.get("db_driver");
        user = config.get("db_user");
        password = config.get("db_password");
        Class.forName(name);// 指定连接类型
        conn = DriverManager.getConnection(url, user, password);// 获取连接
      } catch (Exception e) {
        e.printStackTrace();
      }
  }

  public static void close() {
    if (conn != null)
      try {
        conn.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
  }

  public Connection getConn() {
    return conn;
  }

}
