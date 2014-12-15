package models

/**
 * SteamAPIから取得したユーザーデータのフォーマット
 * @param steamid SteamID
 * @param communityvisibilitystate コミュニティの表示レベル
 * @param profilestate プロフィールの表示レベル
 * @param personname ユーザー名
 * @param profileurl プロフィールURL
 * @param avatar アバター画像
 * @param avatarmedium アバター画像(普通サイズ)
 * @param avatarfull アバター画像(最大サイズ)
 * @param personastate ユーザーのステータス
 */
case class Player(steamid: String,
                  communityvisibilitystate: Int,
                  profilestate: Int,
                  personname: String,
                  profileurl: String,
                  avatar: String,
                  avatarmedium: String,
                  avatarfull: String,
                  personastate: Int)

/**
 * Lobby内で使うユーザーデータのフォーマット
 * @param steamId SteamID
 * @param name ユーザー名
 * @param profileUrl プロフィールURL
 * @param avatar アバター画像
 */
case class PlayerExpression(steamId: String,
                            name: String,
                            profileUrl: String,
                            avatar: String)