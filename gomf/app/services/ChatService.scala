package services

import play.api.libs.json.JsValue

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.Future._

import scala.language.postfixOps

import akka.actor._
import akka.util.Timeout

import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
//import play.api.libs.json.Json

import models.WS._
import services._

/**
 * チャットルームへのルーティング、イベント処理を行う
 */
class ChatServiceImpl extends RoomRepository with ChatRoomService {
  /**
   * リポジトリから該当チャットルームを生成or取り出しを行いユーザを参加させる
   * @param roomId ルームID
   * @param steamId SteamID
   * @return {Iteratee, Enumerator}
   */
  def start(roomId: String, steamId: String) = repository.chatRoom(roomId).flatMap {
    case chatRoom: ActorRef => chatRoomService.join(chatRoom, steamId)
    case _                  => error
  }

  /**
   * エラー発生時のレスポンスを定義
   * @return {Iteratee, Enumerator}
   */
  private def error = future {
    val response = ErrorResponse("couldn't create room").toJson
    (Iteratee.ignore[JsValue], Enumerator[JsValue](response))
  }
}

trait ChatService {
  def chatService = new ChatServiceImpl
}