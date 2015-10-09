package app

import scala.io.Source.fromURL
import spray.json._
import DefaultJsonProtocol._
import com.github.mauricio.async.db.{ RowData, QueryResult, Connection }
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import scala.collection.mutable.Map

class User(uid: String, numberOfOldSubmissionsToCheck: Int) {

  private val id: String = uid;
  val connection: com.github.mauricio.async.db.mysql.MySQLConnection = new Mysql().connect;
  val countOfOldSubmissionsToCheck: Int = numberOfOldSubmissionsToCheck;
  Await.result(connection.connect, Duration.Inf);

  def getData: JsObject = {
    //    get data from hackernewsAPI
    val endpoint: String = "https://hacker-news.firebaseio.com/v0/user/" + id + ".json?print=pretty";
    fromURL(endpoint).mkString.parseJson.asJsObject;
  }

  def getKarmaFromDb: Int = {
    //    Returns user's karma from database. If user is not found then return -1
    //    So this method doubles up to check if a user exists in database
    val query = "SELECT karma FROM user where id = ?";
    val future: Future[QueryResult] = connection.sendPreparedStatement(query, Array(id));
    val result = Await.result(future, Duration.Inf).rows.get;
    val count = result.count { x => !x.isEmpty };
    if (count == 1)
      result.head.mkString.toInt;
    else
      -1;
  }

  def getSubmissionsFromDb: Map[String, Int] = {
    //    Returns user's submission from database in form of a Map with key as submission id and value as score of the submission.
    val query = "SELECT sid, score FROM submission where uid = ? ORDER BY created_at ASC";
    val future: Future[QueryResult] = connection.sendPreparedStatement(query, Array(id));
    val result = Await.result(future, Duration.Inf).rows.get;
    var submissionScoreMap: Map[String, Int] = Map[String, Int]();
    result.foreach { x => submissionScoreMap(x.head.toString) = x.tail.head.toString.toInt };
    submissionScoreMap;
  }

  def updateStats: Any = {

    def getSubmissionArray(seq: Seq[JsValue]): Array[String] = {
      seq.head.toString().split("\\[")(1).split("\\]")(0).split(",");
    }

    val result = getData;
    val karma: Int = result.getFields("karma").mkString.toInt;
    val createdAt: Int = result.getFields("created").mkString.toInt;
    val submissions: Array[String] = getSubmissionArray(result.getFields("submitted"));
    val oldKarma = getKarmaFromDb;
    val oldSubmissions = getSubmissionsFromDb;
    val oldSubmissionsList = oldSubmissions.keys.toList.sortWith(_.toInt > _.toInt);
    val timestamp: Long = System.currentTimeMillis / 1000;

    def updateKarma = {

      def insertNewUser = {
        val query = "INSERT INTO user (karma, inserted_at, updated_at, id) VALUES (?, ?, ?, ?)";
        val future = connection.sendPreparedStatement(query, Array(karma, timestamp, timestamp, id));
        Await.result(future, Duration.Inf);
      }

      def updateOldUser = {
        val query = "UPDATE user SET karma = ?, updated_at = ? WHERE id = ?";
        val future = connection.sendPreparedStatement(query, Array(karma, timestamp, id));
        Await.result(future, Duration.Inf);
      }

      if (oldKarma != karma) {
        val query = "INSERT INTO user_score (uid, karma, updated_at) VALUES (?, ?, ?)";
        val future = connection.sendPreparedStatement(query, Array(id, karma, timestamp));
        Await.result(future, Duration.Inf);
        if (oldKarma > -1) {
          //          user already in db
          updateOldUser;
        } else {
          //          user not in db
          insertNewUser;
        }
      } else {
        updateOldUser;
      }
    }

    def findNewSubmissions(submissionArray: Array[String], counter: Int): List[String] = {
      if (counter > 0) {
        val sid = submissionArray.head;
        if (oldSubmissions contains sid) {
          Nil;
        } else {
          sid :: findNewSubmissions(submissionArray.tail, counter - 1);
        }
      } else {
        Nil;
      }
    }

    val newSubmissions: List[String] = findNewSubmissions(submissions, submissions.length);

    def updateSubmissions(submissionList: List[String], counter: Int): Unit = {
      if (counter > 0) {
        val sid = submissionList.head;
        val oldScore: Int = oldSubmissions.getOrElse(sid, -1);
        new Submission(sid, oldScore, connection).updateStats;
        updateSubmissions(submissionList.tail, counter - 1);
        //        tail recursion
      }
    }

    val counterOldSubmissionsToCheck: Int = {
      var counterOldSubmissions: Int = 0;
      oldSubmissions.foreach(_ => counterOldSubmissions += 1);
      if (countOfOldSubmissionsToCheck == -1) {
        counterOldSubmissions;
      } else {
        counterOldSubmissions min countOfOldSubmissionsToCheck;
      }
    }

    val listToUpdate = newSubmissions ::: oldSubmissionsList.take(counterOldSubmissionsToCheck);
    updateSubmissions(listToUpdate, listToUpdate.length);
    updateKarma;
  }
};