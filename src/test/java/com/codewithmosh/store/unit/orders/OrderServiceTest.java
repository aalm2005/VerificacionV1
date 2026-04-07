package com.codewithmosh.store.unit.orders;

import com.codewithmosh.store.auth.AuthService;
import com.codewithmosh.store.orders.*;
import com.codewithmosh.store.users.Role;
import com.codewithmosh.store.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private User currentUser;
    private User otherUser;
    private Order order;
    private OrderDto orderDto;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).name("Alice").email("alice@example.com").role(Role.USER).build();
        otherUser   = User.builder().id(2L).name("Bob").email("bob@example.com").role(Role.USER).build();

        order = new Order();
        order.setId(10L);
        order.setCustomer(currentUser);
        order.setStatus(PaymentStatus.PENDING);
        order.setTotalPrice(BigDecimal.valueOf(100));

        orderDto = new OrderDto();
        orderDto.setId(10L);
    }

    // -----------------------------------------------------------------------
    // getAllOrders()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getAllOrders()")
    class GetAllOrders {

        @Test
        @DisplayName("returns DTOs of all orders belonging to the current user")
        void returnsDtos_forCurrentUser() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(orderRepository.findAllByCustomer(currentUser)).thenReturn(List.of(order));
            when(orderMapper.toDto(order)).thenReturn(orderDto);

            List<OrderDto> result = orderService.getAllOrders();

            assertThat(result).containsExactly(orderDto);
        }

        @Test
        @DisplayName("returns empty list when current user has no orders")
        void returnsEmptyList_whenNoOrdersExist() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(orderRepository.findAllByCustomer(currentUser)).thenReturn(List.of());

            List<OrderDto> result = orderService.getAllOrders();

            assertThat(result).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getOneOrder()
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getOneOrder()")
    class GetOneOrder {

        @Test
        @DisplayName("returns OrderDto when order belongs to current user")
        void returnsOrderDto_whenOrderBelongsToCurrentUser() {
            when(orderRepository.findOrderWithItems(10L)).thenReturn(Optional.of(order));
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(orderMapper.toDto(order)).thenReturn(orderDto);

            OrderDto result = orderService.getOneOrder(10L);

            assertThat(result).isSameAs(orderDto);
        }

        @Test
        @DisplayName("throws OrderNotFoundException when order does not exist")
        void throwsOrderNotFoundException_whenOrderNotFound() {
            when(orderRepository.findOrderWithItems(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOneOrder(99L))
                    .isInstanceOf(OrderNotFoundException.class);
        }

        @Test
        @DisplayName("throws AccessDeniedException when order belongs to a different user")
        void throwsAccessDeniedException_whenOrderBelongsToDifferentUser() {
            when(orderRepository.findOrderWithItems(10L)).thenReturn(Optional.of(order));
            when(authService.getCurrentUser()).thenReturn(otherUser); // different user

            assertThatThrownBy(() -> orderService.getOneOrder(10L))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }
}
