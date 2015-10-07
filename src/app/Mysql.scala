package app

import com.github.mauricio.async.db.mysql.MySQLConnection
import com.github.mauricio.async.db.Configuration
import com.typesafe.config.ConfigFactory
import java.io.File
import scala.util.Properties
import java.io.File
import com.typesafe.config.{ Config, ConfigFactory }
import java.util.Properties
import java.io.FileInputStream

class Mysql {
  
  def createConfiguration: Configuration = {
  
   val prop = new Properties()
   prop.load(new FileInputStream("src/resources/config.conf"))
   val username:String = prop.getProperty("mysql.username");
   val host: String = prop.getProperty("mysql.host");
   val port: Int = (prop.getProperty("mysql.port")).toInt;
   val password: Option[String] = Some(prop.getProperty("mysql.password"));
   val database: Option[String] = Some(prop.getProperty("mysql.database"));
   new Configuration(username, host, port, password, database);
  }
  
  def connect = {
    val configuration = createConfiguration;
    new MySQLConnection(configuration);
  }
   
}