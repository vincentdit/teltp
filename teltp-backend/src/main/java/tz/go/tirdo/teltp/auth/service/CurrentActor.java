package tz.go.tirdo.teltp.auth.service;

import tz.go.tirdo.teltp.security.CurrentUser;

final class CurrentActor {
    private CurrentActor() {}

    static String orAnonymous() {
        return CurrentUser.username().orElse("anonymous");
    }
}
