var Ignore = (function () {
    function check (hostmask, mapping) {
        hostmask = hostmask.toLowerCase();
        var hostParts = hostmask.split('@');
        var userParts = hostParts[0].split('!');
        
        var user, host;
        if (hostParts[1]) {
            host = hostParts[1];
            user = userParts[1].replace(/^~/, '');
        } else {
            // No username
            host = userParts[1];
            user = '*';
        }
        var nick = userParts[0];
        
        
        mapping = _.clone(mapping);
        var hostMap = mapping[host] || checkWildcard(mapping, host) || mapping['*'];
        var userMap = hostMap[user] || checkWildcard(hostMap, user) || hostMap['*'];
        
        if (userMap) {
            var match = userMap[nick] || checkWildcard(userMap, nick) || userMap['*'];
            return !!match;
        } else {
            return false;
        }
    }
    
    function checkWildcard (mapping, match) {
        return _.find(mapping, function (value, key) {
            if (key.length > 1 && key.indexOf('*') != -1) {
                // Swap * wildcards for .+ after escaping everything else for regex
                return match.match('^' + key.replace(/[-[\]{}()+?.,\\^$|#\s]/g, "\\$&").replace(/\*/g, '.*') + '$');
            }
        });
    }
    
    function parse (list) {
        var mapping = {
            '*': {
                '*': {}
            }
        };
        
        function set (host, user, nick) {
            mapping[host][user][nick] = true;
        }
        
        _.each(list, function (ignore) {
            ignore = ignore.toLowerCase();
            var host = '*';
            var user = '*';
            var nick = '*';
            var ignoreParts = ignore.split('@');
            var usernickParts;
            if (ignoreParts.length == 2) {
                host = ignoreParts[1];
                usernickParts = ignoreParts[0].split('!');
                if (usernickParts.length == 2) {
                    nick = usernickParts[0];
                    user = usernickParts[1];
                } else {
                    user = ignoreParts[0];
                }
            } else {
                usernickParts = ignore.split('!');
                if (usernickParts.length == 2) {
                    nick = usernickParts[0];
                    host = usernickParts[1];
                } else {
                    nick = ignore;
                }
            }
            user = user.replace(/^~/, '');
            
            if (host == '*') {
                if (user == '*') {
                    if (nick == '*') {
                        // *!*@*
                        // unsupported
                    } else {
                        // nick!*@*
                        set('*', '*', nick);
                    }
                } else {
                    if (!mapping['*'][user]) {
                        mapping['*'][user] = {};
                    }
                    if (nick == '*') {
                        // *!user@*
                        set('*', user, '*');
                    } else {
                        // nick!user@*
                        set('*', user, nick);
                    }
                }
            } else {
                if (!mapping[host]) {
                    mapping[host] = {
                        '*': {}
                    };
                }
                if (user == '*') {
                    if (nick == '*') {
                        // *!*@host
                        set(host, '*', '*');
                    } else {
                        // nick!*@host
                        set(host, '*', nick);
                    }
                } else {
                    if (!mapping[host][user]) {
                        mapping[host][user] = {};
                    }
                    if (nick == '*') {
                        // *!user@host
                        set(host, user, '*');
                    } else {
                        // nick!user@host
                        set(host, user, nick);
                    }
                }
            }
        });
        return mapping;
    }
    return {
        check: check,
        parse: parse
    };
})();