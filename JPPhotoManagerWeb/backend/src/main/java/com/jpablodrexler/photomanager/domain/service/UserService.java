package com.jpablodrexler.photomanager.domain.service;

public interface UserService {

    void register(String username, String password);

    String authenticate(String username, String password);
}
