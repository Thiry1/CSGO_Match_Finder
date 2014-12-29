package controllers
import models.GameServer
import play.api.libs.json._
import play.api.mvc._
import scala.collection._

object SourceServerAPI extends Controller {
  /*def serverList: immutable.Seq[Map[String, Any]] = {
    GameServer.list()
  }*/
  def serverList(format: String) = Action { implicit request =>
    //サーバー情報取得
    lazy val servers = GameServer.list

    //指定されたフォーマットの形式でサーバー情報を返す
    format match {
      case "json" => Ok(servers.asJson)
      case _      => InternalServerError("invalid format type")
    }
  }


}
