CSGO_Match_Finder
=================
CS:GO Match FinderはWEBベースの試合マッチングプラグインです。

動作要件
=================
Scala 2.11.4 or higher  
Sass  
memcached

SRCDS(CS:GOゲームサーバー側)動作要件
=================
Metamod:Source 1.10
Sourcemod 1.6.4

設定
=================
application.confの以下の項目の設定をしてください  
**steam.apiKey**には http://steamcommunity.com/dev/apikey で取得したAPI keyを記述してください  
**memcached.hosts**にはあなたのmemcachedサーバーの接続先を指定してください
**csgo.maps**にはマッチング対象のマップを記述してください  
**csgo.servers**にはマッチング対象のゲームサーバーを指定してください

SRCDSの設定
=================
PATH_TO_SRCDS/csgo/addons/sourcemod/pluginsにgameserver_plugins/plugins/gomf.smxを入れてください
