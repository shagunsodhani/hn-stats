package app

object Main {
  def main(args: Array[String]): Unit = {
    println("Started at " + new java.util.Date)
    var user = new User("shagunsodhani");
    user.updateStats;
    println("Completed at " + new java.util.Date + " \n\n\n");
  }
}