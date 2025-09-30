package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.dto.UserDTO;
import com.diepnn.shortenurl.dto.request.UserUpdateRequest;
import com.diepnn.shortenurl.dto.request.UsernamePasswordSignupRequest;
import com.diepnn.shortenurl.entity.Users;
import com.diepnn.shortenurl.exception.DuplicateUniqueKeyException;
import com.diepnn.shortenurl.exception.NotFoundException;
import com.diepnn.shortenurl.mapper.UsersMapper;
import com.diepnn.shortenurl.repository.UsersRepository;
import com.diepnn.shortenurl.utils.SqlConstraintUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database-backed implementation of {@link UsersService}
 */
@Service
@RequiredArgsConstructor
public class UsersServiceImpl implements UsersService {
    private final UsersRepository usersRepository;
    private final UsersMapper usersMapper;

    @Transactional
    @Override
    public UserDTO signup(UsernamePasswordSignupRequest userRequest) {
        Users user = usersMapper.toEntity(userRequest);

        try {
            return usersMapper.toDto(usersRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            if (SqlConstraintUtils.isUniqueConstraintViolation(e, "uidx_users_username")) {
                throw new DuplicateUniqueKeyException("Username '" + user.getUsername() + "' is already in use.");
            }

            if (SqlConstraintUtils.isUniqueConstraintViolation(e, "uidx_users_email")) {
                throw new DuplicateUniqueKeyException("Email '" + user.getEmail() + "' is already in use.");
            }

            throw e;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public UserDTO findByUsername(String username) {
        Users user = usersRepository.findByUsername(username)
                                    .orElseThrow(() -> new NotFoundException("User not found"));
        return usersMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    @Override
    public UserDTO findByEmail(String email) {
        Users users = usersRepository.findByEmail(email)
                                     .orElseThrow(() -> new NotFoundException("User not found"));
        return usersMapper.toDto(users);
    }

    @Transactional
    @Override
    public UserDTO update(UserUpdateRequest userRequest, String username) {
        Users user = usersRepository.findByUsername(username)
                                    .or(() -> usersRepository.findByEmail(username))
                                    .orElseThrow(() -> new NotFoundException("User not found"));
        usersMapper.updateEntity(user, userRequest);
        return usersMapper.toDto(usersRepository.save(user));
    }
}
