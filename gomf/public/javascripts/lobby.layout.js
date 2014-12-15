/**
 * ロビーページのレイアウト関連
 */
(function(){
    /**
     * レイアウト情報
     * @type {{}}
     */
    var layout = {
        /**
         * レイアウトセパレータ
         */
        separator: 40,
        /**
         * レイアウトの値を計算する
         */
        calc: function() {
            //ブラウザの高さ
            layout.browserHeight = $(window).outerHeight();
            //friendボックスの高さ
            layout.friendsBoxHeight = layout.browserHeight - ($('#friendsList').offset().top + this.separator);
            //メインコンテンツの高さ
            layout.mainHeight = layout.browserHeight - ($('#main').offset().top + this.separator);

            return this;
        },
        /**
         * レイアウトを適用する
         */
        apply: function() {
            //フレンドリスト
            $('#friendsList').outerHeight(layout.friendsBoxHeight);
            //メイン
            $('#main').css('min-height', layout.mainHeight);
        }
    };

    $(function() {
        layout.calc().apply();
    });

    /**
     * windowリサイズ時にコールされる
     */
    $(window).resize(function() {
        layout.calc().apply();
    });

    /**
     * チャット表示領域にDOMが追加された際に一番下にスクロールする
     */
    $('#view').on('DOMNodeInserted', function() {
        var element = $(this);
        element.scrollTop(element.get(0).scrollHeight);
    });

})();