/**
 * ロビーページのアクション関連
 */
$(function(){
    /**
     * ユーザーメニュー
     * @type {{element: (*|jQuery|HTMLElement), show: show}}
     */
    var Menu = {
        //表示するメニューのエレメント
        element: $('#userMenu'),
        /**
         * メニューを表示する
         * @param target メニューを表示したい位置のエレメント
         */
        show: function(target) {
            /**
             * @var targetの位置
             */
            var pos = target.position();

            var self = this;

            //メニューの表示位置を設定し表示する
            this.element.css({
                'top': pos.top + (target.height() / 2),
                'left': pos.left + (target.width() / 2)
            }).show(1, function() {
                //メニュー以外をクリックした際にメニューを閉じる
                $('html').one('click', function() {
                    self.element.hide();
                });
            });
        }
    };

    /**
     * フレンドリストクリック時にコールされる
     */
    $('ul#friendsList li').on('click', function() {
        Menu.show($(this));
    });

    /**
     * ロビーのプレーヤー一覧クリック時にコールされる
     */
    $('#lobby ul#players li').on('click', function() {
        Menu.show($(this));
    });
});