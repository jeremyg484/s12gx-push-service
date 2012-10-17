define(function(){
    var empty = {};
    return {
        hasOwnProperty: function (obj, field) {
            return empty.hasOwnProperty.call(obj, field);
        },

        shallowCopy: function (src, dest) {
            var keys = Object.keys(src),
                i;
            for (i = 0; i < keys.length; i += 1) {
                dest[keys[i]] = src[keys[i]];
            }
        },

        liftFunctions: function (src, dest, fields) {
            var i, field;
            for (i = 0; i < fields.length; i += 1) {
                field = fields[i];
                if (src[field] !== undefined &&
                    typeof src[field] === 'function') {
                    dest[field] = src[field].bind(src);
                }
            }
        }
    };
});