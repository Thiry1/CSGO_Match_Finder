/**
 * ロビーページのアクション関連
 */
$(function(){
    /**
     * ユーザーメニュー
     * @type {{element: (*|jQuery|HTMLElement), show: show}}
     */
    var Menu = {
        /**
         * フレンドメニュー
         */
        Friend: {
            //表示するメニューのエレメント
            element: $('#userMenu'),
            /**
             * メニューを表示する
             * @param target メニューを表示したい位置のエレメント
             */
            show: function(target) {
                Menu.show(target, this.element);
            }
        },
        /**
         * ロビー内プレーヤーメニュー
         */
        Lobby: {
            //表示するメニューのエレメント
            element: $('#lobbyMenu'),
            /**
             * メニューを表示する
             * @param target メニューを表示したい位置のエレメント
             */
            show: function(target) {
                Menu.show(target, this.element);
            }
        },
        /**
         * メニューを表示する
         * @param target 表示する位置
         * @param menu 表示するメニュー
         */
        show: function(target, menu) {
            /**
             * @var targetの位置
             */
            var pos = target.position();

            var self = this;

            //メニューの表示位置を設定し表示する
            menu.css({
                'top': pos.top + (target.height() / 2),
                'left': pos.left + (target.width() / 2)
            }).show(1, function () {
                //メニュー以外をクリックした際にメニューを閉じる
                $('html').one('click', function () {
                    menu.hide();
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
        //選択されているマップの一覧を表示するエレメント
        shownElement: $('#selectedMapsList'),
        //選択状態のマップ一覧
        queueList: [],
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
        },
        /**
         * マップの選択状態を切り替える
         */
        toggleSelect: function(elem) {
            /**
             * @var object 選択されたマップのチェックボックスエレメント
             */
            var map = elem.find('input.mapCheckBox');

            /**
             * @var String マップ名
             */
            var mapName = map.val();

            //マップ選択状態を切り替える
            map.prop('checked', !map.prop('checked'));

            /**
             * @var boolean マップが選択状態であればtrueが入る
             */
            var isSelected = map.prop('checked');

            //選択されている状態ならqueueリストにマップ名を追加、選択されていなければqueueリストからマップ名を削除する
            if( isSelected ) {
                this.queueList.push(mapName);
                //figureにselectedクラスを付与
                elem.children('.mapBlock').children('figure').addClass('selected');
            } else {
                this.queueList.remove(mapName);
                //figureからselectedクラスを削除
                elem.children('.mapBlock').children('figure').removeClass('selected');
            }

            //選択マップ一覧表示を更新
            this.shownElement.text(this.queueList.join(" "));
        },
        /**
         * マップの選択状態をリセットする
         */
        reset: function() {
            //キューリストを空にする
            this.queueList = [];

            //選択マップ一覧表示を更新
            this.shownElement.text("");

            //チェックボックスを未チェック状態にする
            this.menuElement.find('input.mapCheckBox').prop('checked', false);
        }
    };

    /**
     * フレンドリストクリック時にコールされる
     */
    $('ul#friendsList li').on('click', function() {
        Menu.Friend.show($(this));
    });

    /**
     * ロビーのプレーヤー一覧クリック時にコールされる
     */
    $('#lobby ul#players li').on('click', function() {
        Menu.Lobby.show($(this));
    });

    /**
     * MAP設定ボタンクリック時にコールされる
     */
    $('#mapSettings').on('click', function() {
        MapMenu.show();
    });

    /**
     * MAP選択時にコールされる
     */
    $('#mapList li').on('click', function() {
        //マップの選択状態を切り替え
        MapMenu.toggleSelect($(this));
    });

    //ブラウザキャッシュによってチェックボックスのチェック状態が保存されている場合があるので初期化
    MapMenu.reset();
});