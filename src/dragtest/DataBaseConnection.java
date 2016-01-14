/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dragtest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author Saravanababu
 */
public class DataBaseConnection {
    private static Connection con;
  public static Connection getConnected() throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException
  {
      Class.forName("org.sqlite.JDBC").newInstance();
      con = DriverManager.getConnection("jdbc:sqlite:circuit.sqlite");
      
      return con;
  }
}
