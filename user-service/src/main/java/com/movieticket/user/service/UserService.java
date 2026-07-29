package com.movieticket.user.service;

import com.movieticket.user.entity.User;
import org.apache.catalina.realm.UserDatabaseRealm;

public interface UserService {

    User register(String name,String password,String nickname);

    String login(String name, String password);

    User getUserById(Long userid);
}