package app

import scala.io.Source.fromURL
import spray.json._
import DefaultJsonProtocol._
import com.github.mauricio.async.db.{RowData, QueryResult, Connection}
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

class User(uid:String) {
  
  private val id:String = uid;
  val connection = new Mysql().connect;
  Await.result(connection.connect, Duration(5, SECONDS) );
  
  def getData:JsObject = {
//    get data from hackernewsAPI
    val endpoint:String = "https://hacker-news.firebaseio.com/v0/user/"+id+".json?print=pretty";
    fromURL(endpoint).mkString.parseJson.asJsObject;
  }
  
  def getKarmaFromDb = {
//    Returns user's kerma from databaseb. If user is not found then return -1
//    So this method doubles up to check if a user exists in database
    val query = "SELECT karma FROM user where id = ?";
    val future: Future[QueryResult] = connection.sendPreparedStatement(query, Array(id))
    val result = Await.result(future, Duration(10, SECONDS)).rows.get;
    val count = result.count { x => !x.isEmpty };
    if (count==1)
      result.head.mkString.toInt;
    else
      -1
  }
  
  def updateStats = {
    val result = getData;
    val karma:Int =  result.getFields("karma").mkString.toInt;
    val createdAt:Int = result.getFields("created").mkString.toInt;
    val submission = result.getFields("submitted");
    val oldKarma = getKarmaFromDb;
    
    def updateKarma = {
      if (oldKarma != karma){
//      updateSubmissionScore(karma-oldKarma);
        var future: Future[QueryResult] = null;
        val timestamp: Long = System.currentTimeMillis / 1000;
        var query = "INSERT INTO user_score (uid, karma, updated_at) VALUES (?, ?, ?)";
        future = connection.sendPreparedStatement(query, Array(id, karma, timestamp))
        Await.result(future, Duration(10, SECONDS));
        if(oldKarma > -1){
//          user already in db
          query = "UPDATE user SET karma = ?, updated_at = ? WHERE id = ?";
          future = connection.sendPreparedStatement(query, Array(karma, timestamp, id))
        }
        else{
//          user not in db
          query = "INSERT INTO user (karma, inserted_at, updated_at, id) VALUES (?, ?, ?)";
          future = connection.sendPreparedStatement(query, Array(karma, timestamp, timestamp, id))
        }
        Await.result(future, Duration(10, SECONDS));
      }
    }
    
    if (karma!=oldKarma){
      print("yay");
      updateKarma;
    }
  }
};

object Test{
  def main(args: Array[String]): Unit = {
    var user = new User("shagunsodhani");
//    user.updateStats;
  }
}

