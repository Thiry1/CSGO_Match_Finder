package models.WS

import models.User
import play.api.libs.json.{JsArray, JsObject, JsValue, JsString}

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
case class MapChange(maps: JsArray)
case class GetMap()

/**
 * ルーム内のWebSocketイベント制御
 * @param roomId ルームID
 */
case class Room(roomId: String) extends Actor {
  private[this] val (enumerator, channel) = Concurrent.broadcast[JsValue]

  //ルームのメンバー一覧
  private[this] var memberList = mutable.LinkedList.empty[JsObject]

  //マップ一覧
  private[this] var mapList = JsArray()

  /**
   * WebSocket受信時にルーティングする
   * @return {Iteratee, Enumerator}
   */
  def receive = {
    case Join(steamId)       => join(steamId)
    case Leave(steamId)      => removeMember(steamId)
    case NewMember(steamId)  => appendMember(steamId)
    case Talk(steamId, text) => sendChat(User.name(steamId), text)
    case MapChange(maps)     => mapChange(maps)
    case GetMap()            =>  notifyMap()
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
    val userObj = JsObject(Seq(
      "steamId"    -> JsString(steamId),
      "userName"   -> JsString(user.name),
      "profileUrl" -> JsString(user.profileUrl),
      "avatar"     -> JsString(user.avatar))
    )
    //ルームが満員でないかチェック
    Logger.debug("this.memberList.length :: " + this.memberList.length)
    this.memberList.length > 4 match {//これ以降の処理で値を追加するため、5人ルームなのに4で比較している
      case true => self ! abort(steamId, "ルームが満員です")

      //ルームに空きがあれば
      case false => {
        this.memberList.contains(userObj) match {
          //ルームにすでに参加している場合
          case true => {
            self ! abort(steamId, "すでにルームに参加しているため切断されました")
          }

          case false => {
            //ユーザー情報をメンバーリストに追加
            this.memberList = this.memberList :+ userObj

            Logger.debug("append:: " + this.memberList.toString())

            //メンバーリストを通知
            self ! notifyMemberList()
            //ユーザー接続を通知
            self ! notifyJoin(user.name.toString())
          }
        }
      }
    }
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

  private def mapChange(maps: JsArray) = {
    this.mapList = maps

    self ! notifyMap
  }

  /**
   * マップ一覧をルームに通知する
   */
  private def notifyMap() = {
    val response = NotifyMapListResponse(this.mapList).toJson
    channel.push(response)
  }

  /**
   * メンバーリストをルームに通知する
   */
  private def notifyMemberList() = {
    val response = MemberListResponse(this.memberList).toJson
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

  /**
   * ユーザーに切断要求を出す
   * @param steamId 切断対象ユーザーのSteamID
   * @param reason 理由
   */
  private def abort(steamId: String, reason: String) = {
    Logger.debug("ABORT")
    val response = AbortResponse(steamId, reason).toJson
    channel.push(response)
  }
}
