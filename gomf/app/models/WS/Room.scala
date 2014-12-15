package models.WS

import models.User
import play.api.libs.json.{JsObject, JsValue, JsString}

import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import play.api._
import play.api.Play.current
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

import services._

import scala.collection._

case class Join(userName: String)
case class Leave(userName: String)
case class NewMember(userName: String)
case class Talk(userName: String, text: String)
case class Joined(enumerator: Enumerator[JsValue])

/**
 * ルーム内のWebSocketイベント制御
 * @param roomId ルームID
 */
case class Room(roomId: String) extends Actor {
  private[this] val (enumerator, channel) = Concurrent.broadcast[JsValue]

  //ルームのメンバー一覧
  private[this] var memberList = mutable.LinkedList.empty[JsObject]

  /**
   * WebSocket受信時にルーティングする
   * @return {Iteratee, Enumerator}
   */
  def receive = {
    case Join(steamId)       => join(steamId)
    case Leave(steamId)      => removeMember(steamId)
    case NewMember(steamId)  => appendMember(steamId)
    case Talk(steamId, text) => sendChat(User.name(steamId), text)
  }

  /**
   * ユーザーのルームへの参加処理
   * @param steamId SteamID
   */
  private def join(steamId: String) = {
    sender ! Joined(enumerator)
    self ! NewMember(steamId)
  }

  /**
   * 該当ユーザーをメンバーリストに追加し、ルームに通知する
   * @param steamId SteamID
   */
  private def appendMember(steamId: String) = {
    //ユーザー情報取得
    val user = User.info(steamId)
    //ユーザー情報をメンバーリストに追加
    this.memberList = this.memberList :+ JsObject(Seq(
      "steamId"    -> JsString(steamId),
      "userName"   -> JsString(user.name),
      "profileUrl" -> JsString(user.profileUrl),
      "avatar"     -> JsString(user.avatar)
    ))

    Logger.debug("append:: " + this.memberList.toString())

    //メンバーリストを通知
    self ! notifyMemberList()
    //ユーザー接続を通知
    self ! notifyJoin(user.name.toString())
  }

  /**
   * 該当ユーザーをメンバーリストから除外し、ルームに通知する
   * @param steamId SteamID
   */
  private def removeMember(steamId: String) = {
    //ユーザー情報を取得
    val user = User.info(steamId)
    val userObj = JsObject(Seq(
      "steamId"    -> JsString(steamId),
      "userName"   -> JsString(user.name),
      "profileUrl" -> JsString(user.profileUrl),
      "avatar"     -> JsString(user.avatar))
    )
    //メンバーリストにleaveイベントを発行したユーザーが存在するかチェック
    this.memberList.contains(userObj) match {
      case true => {
        //ユーザー情報をメンバーリストから削除
        this.memberList = this.memberList diff Seq(userObj)

        Logger.debug("remove:: " + this.memberList.toString())

        //メンバーリストを通知
        self ! notifyMemberList()
        //ユーザー切断を通知
        self ! notifyLeft(User.name(steamId))
      }
      //ルームリストにいないユーザーがLeaveイベントを起こした場合
      case false => {
        val userName = user.name
        Logger.warn(s"ルームから退出しようとしたユーザー($userName: $steamId)はルームリストに存在しませんでした")
        //ユーザー切断を通知
        self ! notifyLeft(userName)
      }
    }
  }

  /**
   * メンバーリストをルームに通知する
   */
  private def notifyMemberList() = {
    val response = memberListResponse(this.memberList).toJson
    channel.push(response)
  }

  /**
   * ユーザーの切断をルームに通知する
   * @param userName ユーザー名
   */
  private def notifyLeft(userName: String) = {
    val response = NotifyLeftResponse(userName).toJson
    channel.push(response)
  }

  /**
   * ユーザーの接続をルームに通知する
   * @param userName ユーザー名
   */
  private def notifyJoin(userName: String) = {
    val response = NotifyJoinResponse(userName).toJson
    channel.push(response)
  }

  /**
   * チャットをルームに通知する
   * @param userName ユーザー名
   * @param text チャット内容
   */
  private def sendChat(userName: String, text: String) = {
    val response = ChatResponse(userName, text).toJson
    channel.push(response)
  }
}
