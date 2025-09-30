package com.diepnn.shortenurl.controller;

import com.diepnn.shortenurl.config.JacksonConfig;
import com.diepnn.shortenurl.dto.UserDTO;
import com.diepnn.shortenurl.dto.request.UserUpdateRequest;
import com.diepnn.shortenurl.dto.request.UsernamePasswordSignupRequest;
import com.diepnn.shortenurl.exception.DuplicateUniqueKeyException;
import com.diepnn.shortenurl.helper.BaseControllerTest;
import com.diepnn.shortenurl.service.UsersService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = UsersController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(JacksonConfig.class)
public class UsersControllerTests extends BaseControllerTest {
    private static final String SIGNUP_ENDPOINT = "/api/v1/users/signup";
    private static final String UPDATE_ENDPOINT = "/api/v1/users/update";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsersService usersService;

    private UsernamePasswordSignupRequest mockSignupRequest;
    private UserDTO mockUserDTO;
    private UserUpdateRequest mockUpdateRequest;

    @BeforeEach
    void setUp() {
        mockSignupRequest = new UsernamePasswordSignupRequest();
        mockSignupRequest.setUsername("ageha-chou");
        mockSignupRequest.setEmail(null);
        mockSignupRequest.setPassword("P@ssw0rd");
        mockSignupRequest.setFirstName("Diep");
        mockSignupRequest.setLastName("Nguyen");

        mockUserDTO = new UserDTO("ageha-chou", null, "Diep", "Nguyen",
                                  null, null);

        mockUpdateRequest = new UserUpdateRequest();
        mockUpdateRequest.setFirstName("Delwyn");
        mockUpdateRequest.setLastName("Nguyenz");
    }

    @Nested
    @DisplayName("UsernamePasswordSignupRequest validation")
    class UsernamePasswordSignupRequestValidationTests {
        @ParameterizedTest
        @NullAndEmptySource
        void whenRequestBodyIsEmpty_return400(String input) throws Exception {
            mockMvc.perform(post(SIGNUP_ENDPOINT)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(input)))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")));
        }

        @ParameterizedTest
        @ValueSource(strings = {" ", "a", "a     ", "ageh@", "ageha chou", "abcdefghijklmnopqrstuvwxyzABCDE"})
        void whenUsernameIsInvalid_return400(String username) throws Exception {
            mockSignupRequest.setUsername(username);
            String expectedMsg = "Username is required and must be 5-30 chars, letters, digits or underscores";
            mockMvc.perform(post(SIGNUP_ENDPOINT)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(mockSignupRequest)))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")))
                   .andExpect(jsonPath("$.errors").exists())
                   .andExpect(jsonPath("$.errors[?(@.field == 'username')].message").value(expectedMsg));
        }

        @ParameterizedTest
        @ValueSource(strings = {" ", "a", "a     ", "Password", "p@ss w0rd", "password", "p@word", "pass0rd"})
        void whenPasswordIsInvalid_return400(String password) throws Exception {
            mockSignupRequest.setPassword(password);
            String expectedMsg = "Password is required and must be at least 8 chars, contain at least one uppercase letter, one lowercase letter, one digit and one special character";
            mockMvc.perform(post(SIGNUP_ENDPOINT)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(mockSignupRequest)))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")))
                   .andExpect(jsonPath("$.errors").exists())
                   .andExpect(jsonPath("$.errors[?(@.field == 'password')].message").value(expectedMsg));
        }

        @ParameterizedTest
        @ValueSource(strings = {" ", "F$rstname", "first_name", "First name", "F1rstName", "abcdefghijklmnopqrstuvwxyzABCDE"})
        void whenFirstNameIsInvalid_return400(String firstName) throws Exception {
            mockSignupRequest.setFirstName(firstName);
            String expectedMsg = "First name is required and must only contain letters";
            mockMvc.perform(post(SIGNUP_ENDPOINT)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(mockSignupRequest)))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")))
                   .andExpect(jsonPath("$.errors").exists())
                   .andExpect(jsonPath("$.errors[?(@.field == 'first_name')].message").value(expectedMsg));
        }

        @ParameterizedTest
        @ValueSource(strings = {" ", "L@stname", "last_name", "Last name", "L4stName"})
        void whenLastNameIsInvalid_return400(String lastName) throws Exception {
            mockSignupRequest.setLastName(lastName);
            String expectedMsg = "Last name is required and must only contain letters";
            mockMvc.perform(post(SIGNUP_ENDPOINT)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(mockSignupRequest)))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")))
                   .andExpect(jsonPath("$.errors").exists())
                   .andExpect(jsonPath("$.errors[?(@.field == 'last_name')].message").value(expectedMsg));
        }
    }

    @Nested
    @DisplayName("UserUpdateRequest validation")
    class UserUpdateTests {
        @ParameterizedTest
        @NullAndEmptySource
        void whenRequestBodyIsEmpty_return400(String input) throws Exception {
            mockMvc.perform(patch(UPDATE_ENDPOINT)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(input)))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")));
        }

        @ParameterizedTest
        @ValueSource(strings = {" ", "F$rstname", "first_name", "First name", "F1rstName", "abcdefghijklmnopqrstuvwxyzABCDE"})
        void whenFirstNameIsInvalid_return400(String firstName) throws Exception {
            mockUpdateRequest.setFirstName(firstName);
            String expectedMsg = "First name is required and must only contain letters";
            mockMvc.perform(patch(UPDATE_ENDPOINT)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(mockUpdateRequest)))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")))
                   .andExpect(jsonPath("$.errors").exists())
                   .andExpect(jsonPath("$.errors[?(@.field == 'first_name')].message").value(expectedMsg));
        }

        @ParameterizedTest
        @ValueSource(strings = {" ", "L@stname", "last_name", "Last name", "L4stName"})
        void whenLastNameIsInvalid_return400(String lastName) throws Exception {
            mockUpdateRequest.setLastName(lastName);
            String expectedMsg = "Last name is required and must only contain letters";
            mockMvc.perform(patch(UPDATE_ENDPOINT)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(mockUpdateRequest)))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                   .andExpect(jsonPath("$.message", is("Invalid request")))
                   .andExpect(jsonPath("$.errors").exists())
                   .andExpect(jsonPath("$.errors[?(@.field == 'last_name')].message").value(expectedMsg));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/signup")
    class SignupTests {
        @Test
        void whenRequestIsValid_return201() throws Exception {
            when(usersService.signup(mockSignupRequest)).thenReturn(mockUserDTO);

            mockMvc.perform(post(SIGNUP_ENDPOINT)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(mockSignupRequest)))
                   .andExpect(status().isCreated())
                   .andExpect(jsonPath("$.status", is(HttpStatus.CREATED.value())))
                   .andExpect(jsonPath("$.message", is("User created successfully")))
                   .andExpect(jsonPath("$.data").exists())
                   .andExpect(jsonPath("$.data.username", is(mockSignupRequest.getUsername())))
                   .andExpect(jsonPath("$.data.email", is(mockSignupRequest.getEmail())))
                   .andExpect(jsonPath("$.data.first_name", is(mockSignupRequest.getFirstName())))
                   .andExpect(jsonPath("$.data.last_name", is(mockSignupRequest.getLastName())));
        }

        @Test
        void whenSignupWithExistingUsernameOrEmail_return409() throws Exception {
            String expectedMsg = "Username 'ageha-chou' is already in use.";
            when(usersService.signup(mockSignupRequest)).thenThrow(new DuplicateUniqueKeyException(expectedMsg));
            mockMvc.perform(post(SIGNUP_ENDPOINT)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(mockSignupRequest)))
                   .andExpect(status().isConflict())
                   .andExpect(jsonPath("$.status", is(HttpStatus.CONFLICT.value())))
                   .andExpect(jsonPath("$.message", is(expectedMsg)));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/update")
    class UpdateTests {
        @WithMockUser(username = "ageha-chou")
        void whenRequestIsValid_return200() throws Exception {
            UserDTO dto = new UserDTO("ageha-chou", null, "Delwyn", "Nguyenz",
                                      null, null);
            when(usersService.update(mockUpdateRequest, "ageha-chou")).thenReturn(dto);

            mockMvc.perform(post(UPDATE_ENDPOINT)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(mockUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is(HttpStatus.OK.value())))
                    .andExpect(jsonPath("$.message", is("User updated successfully")))
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data").value(dto));
        }
    }
}
