package services

import scala.concurrent._
import scala.concurrent.duration._
//import scala.concurrent.Future._

import scala.language.postfixOps

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsValue, Json}

import models.WS._
import services._

/**
 * WebSocket通信のイベントを処理する
 */
class ChatRoomServiceImpl {
  private[this] implicit val timeout = Timeout(1 second)

  /**
   * ユーザーのルームへの参加、イベントの処理を行う
   * @param chatRoom チャットルーム
   * @param steamId SteamID
   * @return {Iteratee, Enumerator}
   */
  def join(chatRoom: ActorRef, steamId: String) = (chatRoom ? Join(steamId)).map {
    case Joined(enumerator) => (in(chatRoom, steamId), enumerator)
    case _                  => error(chatRoom)
  }

  /**
   * イベントの処理を行い、レスポンスを生成する
   * @param chatRoom チャットルーム
   * @param steamId SteamID
   * @return Iteratee
   */
  private def in(chatRoom: ActorRef, steamId: String) = {
    Iteratee.foreach[JsValue]{ data =>
      //イベントの種類
      val event = (data \ "event").as[String]

      //イベントの種類で分岐させ、レスポンスを生成
      event match {
        case "message" => chatRoom ! Talk(steamId, (data \ "text").as[String])
      }

    } map { _ =>
      //ルームから切断させる
      chatRoom ! Leave(steamId)
    }
  }

  /**
   * ルームへの接続失敗時のレスポンスを生成
   * @param chatRoom チャットルーム
   * @return {Iteratee, Enumerator}
   */
  private def error(chatRoom: ActorRef) = {
    val response = ChatResponse(userName = "system", text = "ルームへの接続に失敗しました").toJson
    (Iteratee.ignore[JsValue], Enumerator[JsValue](response))
  }
}

trait ChatRoomService {
  def chatRoomService = new ChatRoomServiceImpl
}