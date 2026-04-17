package com.vsrna.backend.domain.user;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User create(User user);

    Optional<User> find(UserQuery query);

    User get(UserQuery query);

    List<User> list(UserQuery query);

    void delete(UserQuery query);
}
