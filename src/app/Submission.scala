package app

import scala.io.Source.fromURL
import spray.json._
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import java.io.FileWriter

class Submission(sid: String, score: Int, mysqlConnection: com.github.mauricio.async.db.mysql.MySQLConnection) {
  private val id: String = sid;
  private val oldScore: Int = score;
  private val connection: com.github.mauricio.async.db.mysql.MySQLConnection = mysqlConnection;
  private val file = new FileWriter("logging.txt", true);

  def getData: JsObject = {
    //    get data from hackernewsAPI
    val endpoint: String = "https://hacker-news.firebaseio.com/v0/item/" + id + ".json?print=pretty";
    fromURL(endpoint).mkString.parseJson.asJsObject;
  }

  def updateStats: Int = {
    val result = getData;
    var uid: String = result.getFields("by").mkString.replace("\"", "");
    val score_string: String = result.getFields("score").mkString.replace(",", "");
    val score: Int = {
      if (score_string == "") 0;
      else score_string.toInt;
    }
    val createdAt: Int = result.getFields("time").mkString.toInt;
    val hn_type: String = result.getFields("type").mkString.replace("\"", "");

    def insertNewSubmission = {
      val query = "INSERT INTO submission (sid, uid, created_at, score, type) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE created_at=?";
      val future = connection.sendPreparedStatement(query, Array(sid, uid, createdAt, score, hn_type, createdAt));
      Await.result(future, Duration.Inf);
    }

    def updateSubmission = {
      val query = "INSERT INTO submission_score (sid, updated_at, votes) VALUES (?, ?, ?)";
      val timestamp: Long = System.currentTimeMillis / 1000;
      val future = connection.sendPreparedStatement(query, Array(sid, timestamp, score));
      Await.result(future, Duration.Inf);
      file.write("updated sid = " + sid + "\n");
      file.flush();
      file.close();
    }

    if (oldScore == -1) {
      //      new submission
      insertNewSubmission;
      score;
    }
    updateSubmission;
    score - oldScore;
  }

}