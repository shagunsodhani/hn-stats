package app

import scala.io.Source;
import java.io.FileWriter

object Main {
  def main(args: Array[String]): Unit = {

    val timestamp: Long = System.currentTimeMillis / 1000
    var filename: String = timestamp.toString + ".txt";
    val file = new FileWriter("logging.txt", true)
    val timer = new java.util.Timer()
    def update: Unit = {
      file.write("Started at " + new java.util.Date + " \n");
      file.flush();
      var user = new User("shagunsodhani");
      user.updateStats;
      file.write("Completed at " + new java.util.Date + " \n\n\n");
      file.flush();
    }

    val task = new java.util.TimerTask {
      override def run = update
    }
    timer.schedule(task, 0, 180000)
  }
}