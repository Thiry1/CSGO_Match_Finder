/**
 * 配列の中から指定した値を削除する拡張。
 * http://memo.devjam.net/clip/1095
 * @returns {Number}
 */
Array.prototype.remove = function() {
    var i, j, l, m;
    l = arguments.length;
    i = 0;
    while (i < l) {
        m = this.length;
        j = 0;
        while (j < m) {
            if (arguments[i] === this[j]) {
                this.splice(j, 1);
                m--;
            } else {
                j++;
            }
        }
        i++;
    }
    return this.length;
};


