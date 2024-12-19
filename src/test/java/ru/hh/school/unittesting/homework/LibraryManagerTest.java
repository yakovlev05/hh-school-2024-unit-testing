package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

  @Mock
  private NotificationService notificationService;
  @Mock
  private UserService userService;

  @InjectMocks
  private LibraryManager libraryManager;

  @BeforeEach
  void setUp() {
    libraryManager.addBook("123", 5);
    libraryManager.addBook("213", 1);
    libraryManager.addBook("321", 0);
  }

  @Test
  void addBook() {
    libraryManager.addBook("111", 1);
    assertEquals(1, libraryManager.getAvailableCopies("111"));
  }

  @Test
  void getAvailableCopiesNotExistingBook() {
    libraryManager.borrowBook("scsc", "scsdc");
    assertEquals(0, libraryManager.getAvailableCopies("non-exist"));
  }

  @Test
  void borrowBookByNotActiveUser() {
    when(userService.isUserActive("not_active_user")).thenReturn(false);
    assertFalse(libraryManager.borrowBook("123", "not_active_user"));
    verify(notificationService, times(1)).notifyUser("not_active_user", "Your account is not active.");
  }

  @Test
  void borrowNotExistingBook() {
    when(userService.isUserActive("active")).thenReturn(true);
    assertFalse(libraryManager.borrowBook("not exist", "active"));
  }

  @Test
  void borrowBookAndCheckAvailableCopies() {
    when(userService.isUserActive("active")).thenReturn(true);
    assertTrue(libraryManager.borrowBook("123", "active"));
    verify(notificationService, times(1)).notifyUser("active", "You have borrowed the book: 123");

    assertEquals(4, libraryManager.getAvailableCopies("123"));
  }

  @Test
  void returnNotExistingBook() {
    assertFalse(libraryManager.returnBook("123", "some"));
  }

  @Test
  void returnNotMyBook() {
    when(userService.isUserActive("first_user")).thenReturn(true);
    libraryManager.borrowBook("123", "first_user");

    assertFalse(libraryManager.returnBook("123", "another_user"));
  }

  @Test
  void returnBookAndCheckAvailableCopies() {
    when(userService.isUserActive("me")).thenReturn(true);
    libraryManager.borrowBook("123", "me");

    assertTrue(libraryManager.returnBook("123", "me"));
    verify(notificationService, times(1)).notifyUser("me", "You have returned the book: 123");

    assertEquals(5, libraryManager.getAvailableCopies("123"));

    // повторно не могу вернуть
    assertFalse(libraryManager.returnBook("123", "me"));
  }

  @Test
  void calculateDynamicLateFeeShouldThrowExceptionIfOverdueDaysIsNegative() {
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-1, false, false));
    assertEquals("Overdue days cannot be negative.", exception.getMessage());
  }

  @Test
  void calculateDynamicLateFeeNotBestsellerNotPremium() {
    assertEquals(2.5,
        libraryManager.calculateDynamicLateFee(5, false, false));
  }

  @Test
  void calculateDynamicLateFeeIsBestsellerNotPremium() {
    assertEquals(3.75,
        libraryManager.calculateDynamicLateFee(5, true, false));
  }

  @Test
  void calculateDynamicLateFeeNotBestsellerIsPremium() {
    assertEquals(2,
        libraryManager.calculateDynamicLateFee(5, false, true));
  }

  @ParameterizedTest
  @CsvSource({
      "0, 0",
      "1, 0.6",
      "2, 1.2",
      "3, 1.8",
      "13, 7.8",
      "169, 101.4"
  })
  void calculateDynamicLateFeeIsBestsellerIsPremium(int overdueDays, double fee) {
    assertEquals(
        fee,
        libraryManager.calculateDynamicLateFee(overdueDays, true, true));

  }
}
