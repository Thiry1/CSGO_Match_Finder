package models.WS

import akka.actor._
import akka.pattern.ask

/**
 * ルーム生成
 */
trait RoomFactory { this: Actor =>
  /**
   * ルームの生成を行う
   * @param roomId ルームID
   */
  def createChatRoom(roomId: String) = context.actorOf(Props(classOf[Room], roomId), roomId)

}
