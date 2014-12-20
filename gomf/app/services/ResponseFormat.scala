package services

import play.api.libs.json.{JsString, JsArray, JsObject, Json}
import collection._

/**
 * チャット時のレスポンスを生成
 * @param userName ユーザー名
 * @param text チャット内容
 */
case class ChatResponse(userName: String, text: String) {
  def toJson = {
    Json.toJson(immutable.Map("event" -> "talk", "userName" -> userName, "text" -> text))
  }
}

/**
 * エラー発生時のレスポンスを生成
 * @param text エラー内容
 */
case class ErrorResponse(text: String) {
  def toJson = {
    Json.toJson(immutable.Map("event" -> "error", "reason" -> text))
  }
}

/**
 * 強制切断要求レスポンスを生成
 * @param steamId 切断対象ユーザーのSteamID
 * @param reason 理由
 */
case class AbortResponse(steamId: String, reason: String) {
  def toJson = {
    Json.toJson(immutable.Map("event" -> "abort", "steamId" -> steamId, "reason" -> reason))
  }
}

/**
 * ユーザー切断時のレスポンスを生成
 * @param userName ユーザー名
 */
case class NotifyLeftResponse(userName: String) {
  def toJson = {
    Json.toJson(immutable.Map("event" -> "disconnect", "userName" -> userName))
  }
}

/**
 * ユーザー接続時のレスポンスを生成
 * @param userName ユーザー名
 */
case class NotifyJoinResponse(userName: String) {
  def toJson = {
    Json.toJson(immutable.Map("event" -> "joined", "userName" -> userName))
  }
}

/**
 * マップ一覧のレスポンスを生成
 * @param maps マップ一覧
 */
case class NotifyMapListResponse(maps: JsArray) {
  def toJson = {
    JsObject(Seq(
      "event" -> JsString("mapChange"),
      "maps"  -> maps
    ))
  }
}

/**
 * メンバー一覧のレスポンスを生成
 * @param memberList メンバー一覧
 */
case class MemberListResponse(memberList: mutable.LinkedList[JsObject]) {
  def toJson = {
    Json.obj(
      "event" -> "memberModified",
      "user" -> Json.toJson(memberList)
    )
  }
}

/**
 * キューへの参加失敗時のレスポンスを生成
 * @param reason 理由
 */
case class IgnoreQueueResponse(reason: String) {
  def toJson = {
    Json.toJson(immutable.Map("event" -> "ignoreQueue", "reason" -> reason))
  }
}