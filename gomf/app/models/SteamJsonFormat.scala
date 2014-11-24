package models

import play.api.libs.json._
import play.api.libs.functional.syntax._


case class Player(steamid: String,
                  communityvisibilitystate: Int,
                  profilestate: Int,
                  personname: String,
                  lastlogoff: Int,
                  commentpermission: Int,
                  profileurl: String,
                  avatar: String,
                  avatarmedium: String,
                  avatarfull: String,
                  personastate: Int,
                  primaryclanid: String,
                  timecreated: Int,
                  personastateflags: Int,
                  loccountrycode: String,
                  locstatecode: String)
