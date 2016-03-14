@(implicit request: RequestHeader, roomId: String)

/**
 * ロビーのwebsocket関連
 */
$(function() {
    var socket = new WebSocket('@routes.Application.queue(roomId).webSocketURL()');

    var queueWS = {
        roomId: '@roomId',
        /**
         * queueを開始する
         * @@param playerCount プレイヤー人数
         * @@param maps マップ一覧
         * @@param steamIds ロビーに参加しているプレイヤーのSteamID一覧
         */
        start: function(playerCount, maps, steamIds) {
            if( maps.length === 0 ) {
                window.lobbyWS.onError("Map has not selected.");
                //ボタンを初期状態に戻す
                window.queue.toggleBtn.text('GO');
                return;
            }

            //マッチング開始
            socket.send(JSON.stringify({
                event: "startQueue",
                playerCount: playerCount,
                maps: maps,
                steamIds: steamIds
            }));


        },
        /**
         * queueを停止する
         */
        stop: function() {
            if( socket.readyState !== 1 || socket === null ) return;

            socket.send(JSON.stringify({
                event: 'stopQueue'
            }));

        },
        /**
         * サーバーからのレスポンス時にコールされる
         * @@param res レスポンスデータ
         */
        onReceive: function(res) {
            var data = JSON.parse(res.data);
            console.dir(data);
            switch(data.event) {
                case 'matchingStart':
                    queueWS.onMatchingStart(data);
                break;
                case 'anyoneQuitQueue':
                    queueWS.onDisconnect(data);
                break;
                case 'forceQuitQueue':
                    queueWS.onForceQuit(data);
                    break;
                case 'matchFound':
                    queueWS.onMatchFound(data);
                break;
            }
        },
        /**
         * WebSocket通信切断時にコールされる
         */
        onConnectionClosed: function() {
            window.lobbyWS.onError("Disconnect from server. Please refresh the page to reconnect");
        },
        /**
         * ルームが切断された時にコールされる
         * @@param data
         */
        onDisconnect: function(data) {
            //切断したのがこのルームなら
            if( ( typeof data === 'undefined' || data.roomId === this.roomId )
                && window.queue.toggleBtn.text() === 'CANCEL' ) {
                window.lobbyWS.onError("Matching has stopped.");
                //マップ変更ボタン有効化
                lobbyWS.mapSelectBtn.attr('disabled', false).removeAttr('disabled').removeClass('disabled');
                //ボタンを初期状態に戻す
                window.queue.toggleBtn.text('GO');
            }
        },
        onForceQuit: function(data) {
            //切断したのがこのルームなら
            if( typeof data === 'undefined' || data.roomId === this.roomId ) {
                window.lobbyWS.onError("Matching has stopped.");
                window.lobbyWS.onError(data.reason);
                //マップ変更ボタン有効化
                lobbyWS.mapSelectBtn.attr('disabled', false).removeAttr('disabled').removeClass('disabled');
                //ボタンを初期状態に戻す
                window.queue.toggleBtn.text('GO');
            }
        },
        /**
         * マッチング開始時にコールされる
         * @@param data JSONレスポンス
         */
        onMatchingStart: function(data) {
            if( data.roomId === this.roomId ) {
                window.lobbyWS.notify("Matching has started.");
                //マップ変更ボタン無効化
                lobbyWS.mapSelectBtn.attr('disabled', true).addClass('disabled');
                //ボタンを初期状態に戻す
                window.queue.toggleBtn.text('CANCEL');
            }
        },
        /**
         * マッチが見つかった場合にコールされ、検証処理用の値をサーバーに返す
         * @@param data
         */
        onMatchFound: function(data) {
            //マッチングしたルーム一覧に自身のルームIDが存在する場合
            if( data.members.indexOf(this.roomId) != -1 ) {
                window.lobbyWS.notify('MATCH FOUND! CONSOLE: connect ' + data.serverAddress + ':' + data.serverPort + ';password ' + data.serverPassword);
                //ボタンを初期状態に戻す
                window.queue.toggleBtn.text('GO');
                //マップ変更ボタン有効化
                lobbyWS.mapSelectBtn.attr('disabled', false).removeAttr('disabled').removeClass('disabled');
                //サーバーコネクトを送信
                location.href = 'steam://connect/' + data.serverAddress + ':' + data.serverPort + '/' + data.serverPassword;
            }
        }
    };
    //websocketのレスポンス処理メソッドに紐付け
    socket.onmessage = queueWS.onReceive;
    socket.onclose = queueWS.onConnectionClosed;
    //export
    window.queueWS = queueWS;

    //一部ブラウザで、ページ更新時にボタンの状況が維持されてしまうので強制初期化
    lobbyWS.mapSelectBtn.attr('disabled', false).removeAttr('disabled').removeClass('disabled');
});