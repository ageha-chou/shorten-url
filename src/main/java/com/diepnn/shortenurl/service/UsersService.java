package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.dto.UserDTO;
import com.diepnn.shortenurl.dto.request.UserUpdateRequest;
import com.diepnn.shortenurl.dto.request.UsernamePasswordSignupRequest;

public interface UsersService {
    /**
     * Signup a new user with username and password
     *
     * @param userRequest contains the username and password and their additional information.
     * @return the created user.
     */
    UserDTO signup(UsernamePasswordSignupRequest userRequest);

    /**
     * Find user by username.
     *
     * @param username the username to search for.
     * @return user if found, throw exception otherwise.
     */
    UserDTO findByUsername(String username);

    /**
     * Find user by email.
     *
     * @param email email to search for.
     * @return user if found, throw exception otherwise.
     */
    UserDTO findByEmail(String email);

    /**
     * Update user information.
     *
     * @param userRequest contains the new information.
     * @return the updated user.
     */
    UserDTO update(UserUpdateRequest userRequest, String username);
}
