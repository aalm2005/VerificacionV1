package com.codewithmosh.store.unit.users;

import com.codewithmosh.store.users.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User existingUser;
    private UserDto existingUserDto;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .password("encoded-password")
                .role(Role.USER)
                .build();

        existingUserDto = new UserDto(1L, "Alice", "alice@example.com");
    }

    // -----------------------------------------------------------------------
    // getAllUsers()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getAllUsers()")
    class GetAllUsers {

        @Test
        @DisplayName("returns mapped DTOs for all users sorted by given field")
        void returnsMappedDtos_forValidSortBy() {
            when(userRepository.findAll(Sort.by("name"))).thenReturn(List.of(existingUser));
            when(userMapper.toDto(existingUser)).thenReturn(existingUserDto);

            Iterable<UserDto> result = userService.getAllUsers("name");

            assertThat(result).containsExactly(existingUserDto);
        }

        @Test
        @DisplayName("sorts by 'name' when sortBy field is invalid")
        void sortsByName_whenSortByIsInvalid() {
            when(userRepository.findAll(Sort.by("name"))).thenReturn(List.of(existingUser));
            when(userMapper.toDto(existingUser)).thenReturn(existingUserDto);

            userService.getAllUsers("invalidField");

            verify(userRepository).findAll(Sort.by("name"));
        }

        @Test
        @DisplayName("sorts by 'email' when sortBy is 'email'")
        void sortsByEmail_whenSortByIsEmail() {
            when(userRepository.findAll(Sort.by("email"))).thenReturn(List.of(existingUser));
            when(userMapper.toDto(existingUser)).thenReturn(existingUserDto);

            userService.getAllUsers("email");

            verify(userRepository).findAll(Sort.by("email"));
        }

        @Test
        @DisplayName("returns empty list when no users exist")
        void returnsEmpty_whenNoUsers() {
            when(userRepository.findAll(Sort.by("name"))).thenReturn(List.of());

            Iterable<UserDto> result = userService.getAllUsers("name");

            assertThat(result).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getUser()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getUser()")
    class GetUser {

        @Test
        @DisplayName("returns UserDto when user exists")
        void returnsUserDto_whenUserExists() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userMapper.toDto(existingUser)).thenReturn(existingUserDto);

            UserDto result = userService.getUser(1L);

            assertThat(result).isSameAs(existingUserDto);
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void throwsUserNotFoundException_whenUserDoesNotExist() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUser(99L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // registerUser()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("registerUser()")
    class RegisterUser {

        private RegisterUserRequest buildRequest(String email) {
            RegisterUserRequest req = new RegisterUserRequest();
            req.setName("Bob");
            req.setEmail(email);
            req.setPassword("password123");
            return req;
        }

        @Test
        @DisplayName("saves and returns UserDto for a new unique email")
        void savesAndReturnsDto_forNewEmail() {
            RegisterUserRequest request = buildRequest("bob@example.com");
            User newUser = User.builder().id(2L).name("Bob").email("bob@example.com").build();
            UserDto newUserDto = new UserDto(2L, "Bob", "bob@example.com");

            when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(newUser);
            when(passwordEncoder.encode(any())).thenReturn("hashed-password");
            when(userMapper.toDto(newUser)).thenReturn(newUserDto);

            UserDto result = userService.registerUser(request);

            assertThat(result).isSameAs(newUserDto);
            verify(userRepository).save(newUser);
        }

        @Test
        @DisplayName("encodes the password before saving")
        void encodesPassword_beforeSaving() {
            RegisterUserRequest request = buildRequest("bob@example.com");
            User newUser = User.builder().id(2L).name("Bob").email("bob@example.com").password("raw").build();

            when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(newUser);
            when(passwordEncoder.encode("raw")).thenReturn("encoded-raw");
            when(userMapper.toDto(any())).thenReturn(new UserDto(2L, "Bob", "bob@example.com"));

            userService.registerUser(request);

            assertThat(newUser.getPassword()).isEqualTo("encoded-raw");
        }

        @Test
        @DisplayName("sets role to USER on the new user")
        void setsRoleToUser() {
            RegisterUserRequest request = buildRequest("bob@example.com");
            User newUser = User.builder().id(2L).name("Bob").email("bob@example.com").build();

            when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(newUser);
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(userMapper.toDto(any())).thenReturn(new UserDto(2L, "Bob", "bob@example.com"));

            userService.registerUser(request);

            assertThat(newUser.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("throws DuplicateUserException when email already registered")
        void throwsDuplicateUserException_whenEmailExists() {
            RegisterUserRequest request = buildRequest("alice@example.com");
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.registerUser(request))
                    .isInstanceOf(DuplicateUserException.class);
        }
    }

    // -----------------------------------------------------------------------
    // updateUser()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("updateUser()")
    class UpdateUser {

        @Test
        @DisplayName("updates and returns UserDto for an existing user")
        void updatesAndReturnsDto_forExistingUser() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setName("Alice Updated");
            request.setEmail("alice.updated@example.com");

            UserDto updatedDto = new UserDto(1L, "Alice Updated", "alice.updated@example.com");

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userMapper.toDto(existingUser)).thenReturn(updatedDto);

            UserDto result = userService.updateUser(1L, request);

            verify(userMapper).update(request, existingUser);
            verify(userRepository).save(existingUser);
            assertThat(result).isSameAs(updatedDto);
        }

        @Test
        @DisplayName("throws UserNotFoundException for a non-existing user")
        void throwsUserNotFoundException_forNonExistingUser() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(99L, new UpdateUserRequest()))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // deleteUser()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("deletes user by ID when user exists")
        void deletesUser_whenUserExists() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

            userService.deleteUser(1L);

            verify(userRepository).deleteById(1L);
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void throwsUserNotFoundException_whenUserDoesNotExist() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(99L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // changePassword()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("updates password when old password matches stored value")
        void updatesPassword_whenOldPasswordMatches() {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("encoded-password"); // must equal user.getPassword()
            request.setNewPassword("new-password");

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

            userService.changePassword(1L, request);

            assertThat(existingUser.getPassword()).isEqualTo("new-password");
            verify(userRepository).save(existingUser);
        }

        @Test
        @DisplayName("throws AccessDeniedException when old password does not match")
        void throwsAccessDeniedException_whenOldPasswordDoesNotMatch() {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("wrong-password");
            request.setNewPassword("new-password");

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> userService.changePassword(1L, request))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void throwsUserNotFoundException_whenUserDoesNotExist() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.changePassword(99L, new ChangePasswordRequest()))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}
