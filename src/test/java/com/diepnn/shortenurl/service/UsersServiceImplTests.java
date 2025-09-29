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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UsersServiceImplTests {
    @Mock
    private UsersRepository usersRepository;

    @Mock
    private UsersMapper usersMapper;

    @InjectMocks
    private UsersServiceImpl usersService;

    private Users mockUser;
    private UserDTO mockDto;

    @BeforeEach
    void setUp() {
        mockUser = Users.builder()
                        .id(1L)
                        .username("ageha-chou")
                        .email("agehachou@gmail.com")
                        .firstName("Diep")
                        .lastName("Nguyen")
                        .build();

        mockDto = new UserDTO("ageha-chou", "agehachou@gmail.com", "Diep", "Nguyen", null, null);
    }

    @Nested
    @DisplayName("signup function tests")
    class SignUpTests {
        private UsernamePasswordSignupRequest mockRequest;

        @BeforeEach
        void setUp() {
            mockRequest = new UsernamePasswordSignupRequest();
            mockRequest.setUsername("ageha-chou");
            mockRequest.setEmail("agehachou@gmail.com");
            mockRequest.setPassword("P@ssw0rd");
            mockRequest.setFirstName("Diep");
            mockRequest.setLastName("Nguyen");
        }

        @Test
        void whenUsernameIsDuplicated_throwException() {
            DataIntegrityViolationException uniqueConstraint = new DataIntegrityViolationException("Duplicate entry 'ageha-chou' for key 'users.uidx_users_username'");
            when(usersRepository.saveAndFlush(any())).thenThrow(uniqueConstraint);
            when(usersMapper.toEntity(mockRequest)).thenReturn(mockUser);

            try (var mockedStatic = mockStatic(SqlConstraintUtils.class)) {
                mockedStatic.when(() -> SqlConstraintUtils.isUniqueConstraintViolation(uniqueConstraint, "uidx_users_username"))
                            .thenReturn(true);

                DuplicateUniqueKeyException ex = assertThrows(DuplicateUniqueKeyException.class, () -> usersService.signup(mockRequest));

                assertEquals("Username 'ageha-chou' is already in use.", ex.getMessage());
            }
        }

        @Test
        void whenEmailIsDuplicated_throwException() {
            DataIntegrityViolationException uniqueConstraint = new DataIntegrityViolationException("Duplicate entry 'agehachou@gmail.com' for key 'users.uidx_users_email'");
            when(usersRepository.saveAndFlush(any())).thenThrow(uniqueConstraint);
            when(usersMapper.toEntity(mockRequest)).thenReturn(mockUser);

            try (var mockedStatic = mockStatic(SqlConstraintUtils.class)) {
                mockedStatic.when(() -> SqlConstraintUtils.isUniqueConstraintViolation(uniqueConstraint, "uidx_users_email"))
                            .thenReturn(true);

                DuplicateUniqueKeyException ex = assertThrows(DuplicateUniqueKeyException.class, () -> usersService.signup(mockRequest));

                assertEquals("Email 'agehachou@gmail.com' is already in use.", ex.getMessage());
            }
        }

        @Test
        void whenRequestIsValid_saveUser() {
            when(usersMapper.toEntity(mockRequest)).thenReturn(mockUser);
            when(usersRepository.saveAndFlush(ArgumentMatchers.any(Users.class))).thenReturn(mockUser);
            when(usersMapper.toDto(any())).thenReturn(mockDto);

            UserDTO result = usersService.signup(mockRequest);
            assertEquals(mockDto, result);

            verify(usersMapper).toEntity(mockRequest);
            verify(usersRepository).saveAndFlush(mockUser);
            verify(usersMapper).toDto(mockUser);
        }
    }

    @Nested
    @DisplayName("findByUsername function tests")
    class FindByUsernameTests {
        @Test
        void whenUsernameNotFound_throwException() {
            when(usersRepository.findByUsername(any())).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> usersService.findByUsername("ageha-chou"));

            verify(usersMapper, times(0)).toDto(mockUser);
        }

        @Test
        void whenUsernameFound_returnUserDTO() {
            when(usersRepository.findByUsername(any())).thenReturn(Optional.ofNullable(mockUser));
            when(usersMapper.toDto(any())).thenReturn(mockDto);

            UserDTO result = usersService.findByUsername("ageha-chou");
            assertEquals(mockDto, result);
        }
    }

    @Nested
    @DisplayName("findByEmail function tests")
    class FindByEmailTests {
        @Test
        void whenEmailNotFound_throwException() {
            when(usersRepository.findByEmail(any())).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> usersService.findByEmail("ageha-chou"));

            verify(usersMapper, times(0)).toDto(mockUser);
        }

        @Test
        void whenEmailFound_returnUserDTO() {
            when(usersRepository.findByEmail(any())).thenReturn(Optional.ofNullable(mockUser));
            when(usersMapper.toDto(any())).thenReturn(mockDto);

            UserDTO result = usersService.findByEmail("ageha-chou");
            assertEquals(mockDto, result);
        }
    }

    @Nested
    @DisplayName("update function tests")
    class UpdateTests {
        UserUpdateRequest mockRequest;

        @BeforeEach
        void setUp() {
            mockRequest = new UserUpdateRequest();
            mockRequest.setFirstName("Delwyn");
            mockRequest.setLastName("Nguyenz");
        }

        @Test
        void whenUsernameAndEmailNotFound_throwException() {
            when(usersRepository.findByUsername(any())).thenReturn(Optional.empty());
            when(usersRepository.findByEmail(any())).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> usersService.update(mockRequest, "ageha-chou"));
            verify(usersMapper, times(0)).updateEntity(any(), any());
            verify(usersRepository, times(0)).save(any());
            verify(usersMapper, times(0)).toDto(mockUser);
        }

        @Test
        void whenEmailFound_updateUser() {
            when(usersRepository.findByUsername(any())).thenReturn(Optional.empty());
            when(usersRepository.findByEmail(any())).thenReturn(Optional.of(mockUser));
            doNothing().when(usersMapper).updateEntity(mockUser, mockRequest);
            when(usersRepository.save(mockUser)).thenAnswer(invocation -> invocation.getArgument(0));

            UserDTO dto = new UserDTO("ageha-chou", "", "Delwyn", "Nguyenz", null, null);
            when(usersMapper.toDto(eq(mockUser))).thenReturn(dto);

            UserDTO result = usersService.update(mockRequest, "ageha-chou");

            verify(usersMapper, times(1)).updateEntity(any(), any());
            verify(usersRepository, times(1)).save(any());
            verify(usersMapper, times(1)).toDto(mockUser);
            assertEquals(dto, result);
        }

        @Test
        void whenUsernameFound_updateUser() {
            when(usersRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            doNothing().when(usersMapper).updateEntity(mockUser, mockRequest);
            when(usersRepository.save(mockUser)).thenAnswer(invocation -> invocation.getArgument(0));

            UserDTO dto = new UserDTO("ageha-chou", "", "Delwyn", "Nguyenz", null, null);
            when(usersMapper.toDto(eq(mockUser))).thenReturn(dto);

            UserDTO result = usersService.update(mockRequest, "ageha-chou");

            verify(usersMapper, times(1)).updateEntity(any(), any());
            verify(usersRepository, times(1)).save(any());
            verify(usersMapper, times(1)).toDto(mockUser);
            assertEquals(dto, result);
        }
    }
}
