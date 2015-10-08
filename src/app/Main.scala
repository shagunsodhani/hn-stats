package app

import scala.io.Source
import java.io.FileWriter
import java.util.Properties
import java.io.FileInputStream

object Main {
  def main(args: Array[String]): Unit = {

    val prop = new Properties();
    prop.load(new FileInputStream("src/resources/config.conf"));
    val username: String = prop.getProperty("hn.username");
    val period: Int = prop.getProperty("app.period").toInt;

    //    file where updates are logged
    val file = new FileWriter("logging.txt", true);

    val timer = new java.util.Timer();
    def update: Unit = {
      file.write("Started at " + new java.util.Date + " \n");
      file.flush();
      var user = new User(username);
      user.updateStats;
      file.write("Completed at " + new java.util.Date + " \n\n\n");
      file.flush();
    }

    val task = new java.util.TimerTask {
      override def run = update;
    }
    timer.schedule(task, 0, period);
  }
}