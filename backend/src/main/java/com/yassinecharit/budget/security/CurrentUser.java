package com.yassinecharit.budget.security;

import com.yassinecharit.budget.common.NotFoundException;
import com.yassinecharit.budget.user.User;
import com.yassinecharit.budget.user.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Gives services access to the authenticated user. */
@Component
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public User get() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return users.findByEmailIgnoreCase(email).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
