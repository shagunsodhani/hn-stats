package app

import scala.io.Source.fromURL
import spray.json._
import DefaultJsonProtocol._
import com.github.mauricio.async.db.{RowData, QueryResult, Connection}
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

class User(uid:String) {
  
  private val id:String = uid;
  
  def getData:JsObject = {
//    get data from hackernewsAPI
    val endpoint:String = "https://hacker-news.firebaseio.com/v0/user/"+id+".json?print=pretty";
    fromURL(endpoint).mkString.parseJson.asJsObject;
  }
  
  val connection = new Mysql().connect;
  Await.result(connection.connect, Duration(5, SECONDS) );
  
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
  
  
};

object Test{
  def main(args: Array[String]): Unit = {
    var user = new User("shagunsodhani");
//    print(user.checkUserInDb);
    print(user.getKarmaFromDb);
  }
}

