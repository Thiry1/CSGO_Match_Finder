package models.WS

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import akka.actor.Identify._

import scala.concurrent.duration._
import scala.util.Success
import scala.language.postfixOps

import play.api._
import play.api.Play.current
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

case class FindChatRoom(roomId: String)

/**
 * 複数のルームを管理する
 */
class RoomRepositoryActor extends Actor with RoomFactory {
  implicit val timeout = Timeout(1 second)

  /**
   * イベント受信時
   * @return {Iteratee, Enumerator}
   */
  def receive = {
    case FindChatRoom(roomId) => sender ! chatRoom(roomId)
    case x                    => println(x.toString)
  }

  /**
   * roomIdに該当するルームを返す。なければ作成する
   * @param roomId ルームID
   * @return ルーム
   */
  private def chatRoom(roomId: String) = context.child(roomId) match {
    case Some(room) => room
    case None => createChatRoom(roomId)
  }
}

object RoomRepository {
  implicit val timeout = Timeout(1 second)
  private[this] val repository = Akka.system.actorOf(Props[RoomRepositoryActor])

  def chatRoom(roomId: String) = repository ? FindChatRoom(roomId)
}

trait RoomRepository {
  val repository = RoomRepository
}
