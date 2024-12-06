package ru.hh.school.unittesting.example;

import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderProcessorTest {

  @Mock
  private PaymentService paymentService;

  @InjectMocks
  private OrderProcessor orderProcessor;

  @BeforeEach
  void setUp() {
    orderProcessor.setInventory(Map.of(
        "item1", 10,
        "item2", 5,
        "item3", 0
    ));
  }

  @ParameterizedTest
  @CsvSource({
      "item1, 2, 50, 100, 8",
      "item1, 3, 40, 108, 7",
      "item2, 5, 10, 50, 0"
  })
  void testProcessOrder(
      String itemId,
      int quantity,
      double pricePerUnit,
      double expectedTotalPrice,
      int expectedStock
  ) {
    double totalPrice = orderProcessor.processOrder(itemId, quantity, pricePerUnit);
    int stock = orderProcessor.getStock(itemId);

    assertEquals(expectedTotalPrice, totalPrice);
    assertEquals(expectedStock, stock);
  }

  @Test
  void processOrderShouldThrowExceptionIfItemNotExists() {
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> orderProcessor.processOrder("item4", 1, 10)
    );
    assertEquals("Item item4 not found in inventory.", exception.getMessage());
  }

  @Test
  void processOrderShouldThrowExceptionIfInsufficientStock() {
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> orderProcessor.processOrder("item2", 6, 20)
    );
    assertEquals("Insufficient stock for item: item2", exception.getMessage());
  }

  @Test
  void processOrderShouldThrowExceptionIfItemOutOfStock() {
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> orderProcessor.processOrder("item3", 1, 20)
    );
    assertEquals("Insufficient stock for item: item3", exception.getMessage());
  }

  @Test
  void testProcessOrderWithPaymentSuccess() {
    when(paymentService.processPayment(100)).thenReturn(true);

    boolean paymentResult = orderProcessor.processOrderWithPayment("item1", 2, 50);

    assertTrue(paymentResult);
    assertEquals(8, orderProcessor.getStock("item1"));
    verify(paymentService, times(1)).processPayment(100);
  }

  @Test
  void testProcessOrderWithPaymentFailure() {
    when(paymentService.processPayment(108)).thenReturn(false);

    boolean paymentResult = orderProcessor.processOrderWithPayment("item1", 3, 40);

    assertFalse(paymentResult);
    assertEquals(7, orderProcessor.getStock("item1"));
    verify(paymentService, times(1)).processPayment(108);
  }
}
