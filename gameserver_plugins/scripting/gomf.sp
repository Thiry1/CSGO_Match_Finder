#include<sourcemod>
#include<cstrike>

#define PLUGIN_VERSION "0.0.1"

public Plugin:myinfo = {
    name = "CS:GO Match Finder",
    author = "Thiry",
    description = "CS:GO Match Finder server side plugin",
    version = PLUGIN_VERSION,
    url = "http://blog.five-seven.net/"
};

//サーバーの予約ステータスが格納される
new bool: reserveStatus = false;

//予約されたプレイヤーのSTEAMIDを格納
new Handle: steamIds;

/**
 * プラグインロード時にコールされる
 */
public OnPluginStart() {
    RegServerCmd("gomf_get_reserve_status", CmdGomfGetReserveStatus);
    RegServerCmd("gomf_reserve", CmdGomfReserve);
    RegServerCmd("gomf_free", CmdGomfFree);

    HookEvent("player_disconnect", evPlayerDisconnect);
    HookEvent("player_activate", evPlayerActivate);

    steamIds = CreateArray(10);
}

/**
 * プレイヤーのロードが終わってゲームに接続完了した際にコールされる
 */
public evPlayerActivate(Handle: event, const String: name[], bool: dontBroadcast) {
    new client = GetClientOfUserId(GetEventInt(event, "userid"));

    if( !IsFakeClient(client) ) {
        new index = SteamIdInListAt(client);

        //予約プレイヤーリストにいなければキックする
        if( index == -1 ) {
            if( !IsFakeClient(client) ) {
                KickClient(client, "You are not in GOMF player list");
            }
            return;
        }

        //indexが4以下ならCTに参加、5以上ならTに参加させる
        if( index <= 4 ){
            ChangeClientTeam(client, CS_TEAM_CT);
        } else {
            ChangeClientTeam(client, CS_TEAM_T);
        }
    }
}
/**
 * プレイヤー切断時にコールされる
 */
public evPlayerDisconnect(Handle: event, const String: name[], bool: dontBroadcast) {
    if( GetRealClientCount(false) == 0 ) {
        PrintToServer("[GOMF] All players are disconnected");
        //サーバーを開放
        free();
    }
}

/**
 * サーバーの予約状況を表示する
 */
public Action: CmdGomfGetReserveStatus(args) {
    //サーバーが予約状態なら
    if( reserveStatus ) {
        PrintToServer("[GOMF] Server is Reserved");
    } else {
        PrintToServer("[GOMF] Server is Free");
    }
}

/**
 * サーバーの予約する
 */
public Action: CmdGomfReserve(args) {
    if( reserveStatus ) {
        PrintToServer("[GOMF] reserve failed");
    } else {
        //SteamIDの予約を行う
        if( setSteamIds(args) ) {
            reserveStatus = true;
            PrintToServer("[GOMF] reserve successful");
        } else {
            PrintToServer("[GOMF] reserve failed");
        }
    }
}
/**
 * SteamIDを配列に登録
 */
public bool: setSteamIds(const any: args) {
    if( GetCmdArgs() != 10 ) {
        PrintToServer("[GOMF] Args are invalid. count:%d", GetCmdArgs());
        return false;
    }

    for(new i = 1; i <= 10; i++) {
        new String: steamId[64];
        GetCmdArg(i, steamId, sizeof(steamId));
        //配列に格納
        PushArrayString(steamIds, steamId);
    }
    return true;
}

/**
 * サーバーの予約を開放する
 */
public Action: CmdGomfFree(args) {
    free();
    PrintToServer("[GOMF] free successful");
}

/**
 * 予約済みプレイヤーのリストに該当プレーヤーが入っているかチェックする
 * @param client プレイヤーのUserID
 * @return 配列のindexを返す。なければ-1を返す
 */
stock SteamIdInListAt(client) {
    //ゲーム内SteamIDフォーマット
    new String: userSteamId[32];
    GetClientAuthString(client, userSteamId, sizeof(userSteamId));
    //64bit SteamID
    decl String: steamId64bit[18];
    GetCommunityIDString(userSteamId, steamId64bit, sizeof(steamId64bit));

    //予約済みSteamIDのリストの中に該当プレイヤーがいるかチェック
    for(new i = 0; i < GetArraySize(steamIds); i++) {
        decl String: steamId[18];
        GetArrayString(steamIds, i, steamId, sizeof(steamId));
        if( StrEqual(steamId, steamId64bit) ) {
            return i;
        }
    }
    return -1;
}

/**
 * SteamIDを64bitSteamIDに変換する
 * https://forums.alliedmods.net/showthread.php?t=183443
 */
stock bool:GetCommunityIDString(const String: SteamID[], String: CommunityID[], const CommunityIDSize)
{
    decl String:SteamIDParts[3][11];
    new const String:Identifier[] = "76561197960265728";

    if ((CommunityIDSize < 1) || (ExplodeString(SteamID, ":", SteamIDParts, sizeof(SteamIDParts), sizeof(SteamIDParts[])) != 3))
    {
        CommunityID[0] = '\0';
        return false;
    }

    new Current, CarryOver = (SteamIDParts[1][0] == '1');
    for (new i = (CommunityIDSize - 2), j = (strlen(SteamIDParts[2]) - 1), k = (strlen(Identifier) - 1); i >= 0; i--, j--, k--)
    {
        Current = (j >= 0 ? (2 * (SteamIDParts[2][j] - '0')) : 0) + CarryOver + (k >= 0 ? ((Identifier[k] - '0') * 1) : 0);
        CarryOver = Current / 10;
        CommunityID[i] = (Current % 10) + '0';
    }

    CommunityID[CommunityIDSize - 1] = '\0';
    return true;
}

/**
 * 予約済みサーバーを開放する
 */
stock free() {
    //プレイヤーリストの初期化
    ClearArray(steamIds);
    //予約ステータスを無効化
    reserveStatus = false;
}
/**
 * BOTを除いたクライアントの接続数を返す
 */
stock GetRealClientCount(bool: inGameOnly = true) {
    new clients = 0;
    for( new i = 1; i <= GetMaxClients(); i++ ) {
        if( ( ( inGameOnly ) ? IsClientInGame( i ) : IsClientConnected( i ) ) && !IsFakeClient( i ) ) {
            clients++;
        }
    }
    return clients
}