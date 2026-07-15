package com.jpablodrexler.photomanager.domain.port.out;

public interface UserService {

    void register(String username, String password);

    String authenticate(String username, String password);
}
