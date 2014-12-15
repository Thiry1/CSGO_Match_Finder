@(implicit request: RequestHeader, roomId: String)

/**
 * ロビーのwebsocket関連
 */
$(function() {
    var socket = new WebSocket('@routes.Application.room(roomId).webSocketURL()');


    var lobbyWS = {
        /**
         * プレイヤー一覧表示エリア
         */
        playerDisplayArea: $('ul#players').children('li'),
        /**
         * テキスト表示エリア
         */
        textDisplayArea: $('#view'),
        /**
         * テキスト入力用input form
         */
        textInputForm: $('#lobbyChatInput'),
        /**
         * テキスト入力用input
         */
        textInput: $('#lobbyChatInput').children('input'),
        /**
         * メッセージの送信
         */
        sendMessage: function(msg) {
            socket.send(JSON.stringify({
                event: "message",
                text: msg
            }));
        },
        /**
         * エラーメッセージを表示する
         * @@param reason
         */
        onError: function(reason) {
            var el = $('<p class="error"></p>');
            el.text(reason);
            //表示領域にエラーを出力
            this.textDisplayArea.append(el);
        },
        /**
         * サーバーからのレスポンス時にコールされる
         * @@param res レスポンスデータ
         */
        onReceive: function(res) {
            var data = JSON.parse(res.data);
            console.dir(data);
            //エラーの場合
            if( data.error ) {
                lobbyWS.onError(data.error);
                return;
            }

            switch( data.event ) {
                case 'talk':
                    lobbyWS.onMessage(data.userName, data.text);
                break;

                case 'joined':
                    lobbyWS.onJoined(data);
                break;

                case 'disconnect':
                    lobbyWS.onDisconnect(data);
                break;

                case 'memberModified':
                    lobbyWS.onMemberModified(data);
                break;

                default:
                    lobbyWS.onError("undefined event: " + data.event);
                break;
            }
        },
        /**
         * ルームへ接続しているユーザーが増減した場合の処理
         * @@param data
         */
        onMemberModified: function(data) {
            //ユーザー表示領域をリセット
            this.playerDisplayArea.each(function() {
                var li = $(this);
                li.children('.thumb').html('');
                li.children('.name').html('');
            });
            //ルームに接続しているユーザー一覧
            var users = data.user;

            //ユーザー一覧をバインド
            for(var i = 0, len = users.length; i < len ; i++) {
              var li = this.playerDisplayArea.eq(i),
                  //ユーザー情報
                  user = users[i],
                  thumb = $('<img src="#" width="80" height="80">');

              //SteamID、プロフィールURLをバインド
              li.attr('data-steamId', user.steamId).attr('data-profileUrl', user.profileUrl);

              //アバター画像をバインド
              thumb.attr('src', user.avatar);
              li.children('.thumb').append(thumb);

              //ユーザー名をバインド
              li.children('.name').text(user.userName);
            }
        },
        /**
         * ルーム内の誰かが接続時の処理
         * @@param data
         */
        onJoined: function(data) {
            lobbyWS.notify(data.userName + " さんが接続しました");
        },
        /**
         * ルーム内の誰かが切断時の処理
         * @@param data
         */
        onDisconnect: function(data) {
            lobbyWS.notify(data.userName + " さんが切断しました");
        },
        notify: function(text) {
            var textElement = $('<p class="notify"></p>');
            //メッセージを埋め込み
            textElement.text(text);

            //表示領域に出力
            this.textDisplayArea.append(textElement);
        },
        /**
         * メッセージ受信時の処理
         * @@param userName ユーザー名
         * @@param message メッセージ
         */
        onMessage: function(userName, message) {
            var textElement = $('<p></p>'),
                userNameElement = $('<span></span>');
            //ユーザー名を埋め込み
            userNameElement.text(userName + ": ");
            //メッセージを埋め込み
            textElement.text(message);
            textElement.prepend(userNameElement);

            //表示領域に出力
            this.textDisplayArea.append(textElement);
        }
    };

    lobbyWS.textInputForm.on('submit', function() {
       lobbyWS.sendMessage(lobbyWS.textInput.val());
        //入力エリアの初期化
        lobbyWS.textInput.val('');
        //フォーム送信無効化
        return false;
    });

    //websocketのレスポンス処理メソッドに紐付け
    socket.onmessage = lobbyWS.onReceive;
});