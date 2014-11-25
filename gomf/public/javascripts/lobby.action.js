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
     * MAPメニュー
     * @type {{}}
     */
    var MapMenu = {
        //表示するメニューのエレメント
        menuElement: $('#mapSelector'),
        /**
         * マップ選択画面を表示する
         */
        show: function() {
            var self = this;
            //マップ選択画面を表示
            this.menuElement.show(1, function(){
                //OKボタンを押した時
                self.menuElement.find('#mapOK').one('click', function(){
                    //メニューを閉じる
                    self.menuElement.hide();
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

    /**
     * MAP設定ボタンクリック時にコールされる
     */
    $('#mapSettings').on('click', function() {
        MapMenu.show();
    });
});