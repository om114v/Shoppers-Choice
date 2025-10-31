package com.shopper.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.shopper.models.StickerData;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for StickerData class. Tests cover constructors,
 * getters/setters, toString method, validation method, and various input
 * scenarios including edge cases.
 */
@DisplayName("StickerData Unit Tests")
public class StickerDataTest {

	@Nested
	@DisplayName("Constructor Tests")
	class ConstructorTests {

		@Test
		@DisplayName("Should create instance with default constructor")
		void testDefaultConstructor() {
			StickerData data = new StickerData();

			assertNotNull(data);
			assertNull(data.getItemName());
			assertNull(data.getSupplierName());
			assertNull(data.getPrice());
			assertEquals(0, data.getNumberOfStickers());
		}

		@Test
		@DisplayName("Should create instance with parameterized constructor")
		void testParameterizedConstructor() {
			StickerData data = new StickerData("Test Item", "Test Supplier", new BigDecimal("10.50"), 5);

			assertNotNull(data);
			assertEquals("Test Item", data.getItemName());
			assertEquals("Test Supplier", data.getSupplierName());
			assertEquals(new BigDecimal("10.50"), data.getPrice());
			assertEquals(5, data.getNumberOfStickers());
		}

		@Test
		@DisplayName("Should handle null values in parameterized constructor")
		void testConstructorWithNullValues() {
			StickerData data = new StickerData(null, null, null, 0);

			assertNull(data.getItemName());
			assertNull(data.getSupplierName());
			assertNull(data.getPrice());
			assertEquals(0, data.getNumberOfStickers());
		}

		@Test
		@DisplayName("Should handle zero price and zero stickers")
		void testConstructorWithZeroValues() {
			StickerData data = new StickerData("", "", BigDecimal.ZERO, 0);

			assertEquals("", data.getItemName());
			assertEquals("", data.getSupplierName());
			assertEquals(BigDecimal.ZERO, data.getPrice());
			assertEquals(0, data.getNumberOfStickers());
		}

		@Test
		@DisplayName("Should handle negative number of stickers")
		void testConstructorWithNegativeStickers() {
			StickerData data = new StickerData("Item", "Supplier", new BigDecimal("5.00"), -1);

			assertEquals("Item", data.getItemName());
			assertEquals("Supplier", data.getSupplierName());
			assertEquals(new BigDecimal("5.00"), data.getPrice());
			assertEquals(-1, data.getNumberOfStickers());
		}
	}

	@Nested
	@DisplayName("Getter and Setter Tests")
	class GetterSetterTests {

		@Test
		@DisplayName("Should set and get item name")
		void testItemNameGetterSetter() {
			StickerData data = new StickerData();

			data.setItemName("New Item");
			assertEquals("New Item", data.getItemName());

			data.setItemName(null);
			assertNull(data.getItemName());

			data.setItemName("");
			assertEquals("", data.getItemName());
		}

		@Test
		@DisplayName("Should set and get supplier name")
		void testSupplierNameGetterSetter() {
			StickerData data = new StickerData();

			data.setSupplierName("New Supplier");
			assertEquals("New Supplier", data.getSupplierName());

			data.setSupplierName(null);
			assertNull(data.getSupplierName());

			data.setSupplierName("");
			assertEquals("", data.getSupplierName());
		}

		@Test
		@DisplayName("Should set and get price")
		void testPriceGetterSetter() {
			StickerData data = new StickerData();

			BigDecimal price = new BigDecimal("25.99");
			data.setPrice(price);
			assertEquals(price, data.getPrice());

			data.setPrice(null);
			assertNull(data.getPrice());

			data.setPrice(BigDecimal.ZERO);
			assertEquals(BigDecimal.ZERO, data.getPrice());

			data.setPrice(new BigDecimal("-10.00"));
			assertEquals(new BigDecimal("-10.00"), data.getPrice());
		}

		@Test
		@DisplayName("Should set and get number of stickers")
		void testNumberOfStickersGetterSetter() {
			StickerData data = new StickerData();

			data.setNumberOfStickers(100);
			assertEquals(100, data.getNumberOfStickers());

			data.setNumberOfStickers(0);
			assertEquals(0, data.getNumberOfStickers());

			data.setNumberOfStickers(-50);
			assertEquals(-50, data.getNumberOfStickers());
		}
	}

	@Nested
	@DisplayName("Validation Tests")
	class ValidationTests {

		@Test
		@DisplayName("Should return true for valid data")
		void testValidateValidData() {
			StickerData data = new StickerData("Valid Item", "Valid Supplier", new BigDecimal("10.00"), 5);

			assertTrue(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should return false for null item name")
		void testValidateNullItemName() {
			StickerData data = new StickerData(null, "Supplier", new BigDecimal("10.00"), 5);

			assertFalse(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should return false for empty item name")
		void testValidateEmptyItemName() {
			StickerData data = new StickerData("", "Supplier", new BigDecimal("10.00"), 5);

			assertFalse(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should return false for whitespace-only item name")
		void testValidateWhitespaceItemName() {
			StickerData data = new StickerData("   ", "Supplier", new BigDecimal("10.00"), 5);

			assertFalse(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should return false for null supplier name")
		void testValidateNullSupplierName() {
			StickerData data = new StickerData("Item", null, new BigDecimal("10.00"), 5);

			assertFalse(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should return false for empty supplier name")
		void testValidateEmptySupplierName() {
			StickerData data = new StickerData("Item", "", new BigDecimal("10.00"), 5);

			assertFalse(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should return false for whitespace-only supplier name")
		void testValidateWhitespaceSupplierName() {
			StickerData data = new StickerData("Item", "   ", new BigDecimal("10.00"), 5);

			assertFalse(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should return false for null price")
		void testValidateNullPrice() {
			StickerData data = new StickerData("Item", "Supplier", null, 5);

			assertFalse(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should return false for zero price")
		void testValidateZeroPrice() {
			StickerData data = new StickerData("Item", "Supplier", BigDecimal.ZERO, 5);

			assertFalse(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should return false for negative price")
		void testValidateNegativePrice() {
			StickerData data = new StickerData("Item", "Supplier", new BigDecimal("-5.00"), 5);

			assertFalse(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should return false for zero stickers")
		void testValidateZeroStickers() {
			StickerData data = new StickerData("Item", "Supplier", new BigDecimal("10.00"), 0);

			assertFalse(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should return false for negative stickers")
		void testValidateNegativeStickers() {
			StickerData data = new StickerData("Item", "Supplier", new BigDecimal("10.00"), -1);

			assertFalse(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should return true for minimum valid values")
		void testValidateMinimumValidValues() {
			StickerData data = new StickerData("A", "B", new BigDecimal("0.01"), 1);

			assertTrue(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should return true for large values")
		void testValidateLargeValues() {
			StickerData data = new StickerData("Item", "Supplier", new BigDecimal("999999.99"), Integer.MAX_VALUE);

			assertTrue(data.validate().isEmpty());
		}
	}

	@Nested
	@DisplayName("toString Tests")
	class ToStringTests {

		@Test
		@DisplayName("Should return correct string representation")
		void testToString() {
			StickerData data = new StickerData("Test Item", "Test Supplier", new BigDecimal("10.50"), 5);
			String expected = "StickerData{itemName='Test Item', supplierName='Test Supplier', price=10.50, numberOfStickers=5}";

			assertEquals(expected, data.toString());
		}

		@Test
		@DisplayName("Should handle null values in toString")
		void testToStringWithNullValues() {
			StickerData data = new StickerData(null, null, null, 0);
			String expected = "StickerData{itemName='null', supplierName='null', price=null, numberOfStickers=0}";

			assertEquals(expected, data.toString());
		}

		@Test
		@DisplayName("Should handle zero values in toString")
		void testToStringWithZeroValues() {
			StickerData data = new StickerData("", "", BigDecimal.ZERO, 0);
			String expected = "StickerData{itemName='', supplierName='', price=0, numberOfStickers=0}";

			assertEquals(expected, data.toString());
		}

		@Test
		@DisplayName("Should handle negative values in toString")
		void testToStringWithNegativeValues() {
			StickerData data = new StickerData("Item", "Supplier", new BigDecimal("-5.00"), -1);
			String expected = "StickerData{itemName='Item', supplierName='Supplier', price=-5.00, numberOfStickers=-1}";

			assertEquals(expected, data.toString());
		}

		@Test
		@DisplayName("Should handle large decimal values in toString")
		void testToStringWithLargeDecimal() {
			BigDecimal largePrice = new BigDecimal("123456789.123456789");
			StickerData data = new StickerData("Item", "Supplier", largePrice, 1);

			assertTrue(data.toString().contains(largePrice.toString()));
		}
	}

	@Nested
	@DisplayName("Parameterized Tests")
	class ParameterizedTests {

		@ParameterizedTest
		@CsvSource({ "'Item A', 'Supplier A', 10.50, 5", "'Item B', 'Supplier B', 0.01, 1", "'', '', 999.99, 100",
				"'Special@#$%', 'Supplier!@#', 123.45, 50" })
		@DisplayName("Should handle various constructor parameters")
		void testConstructorParameters(String itemName, String supplierName, BigDecimal price, int numberOfStickers) {
			StickerData data = new StickerData(itemName, supplierName, price, numberOfStickers);

			assertEquals(itemName, data.getItemName());
			assertEquals(supplierName, data.getSupplierName());
			assertEquals(price, data.getPrice());
			assertEquals(numberOfStickers, data.getNumberOfStickers());
		}

		@ParameterizedTest
		@CsvSource({ "'Valid Item', 'Valid Supplier', 10.00, 5, true", "'', 'Supplier', 10.00, 5, false",
				"'Item', '', 10.00, 5, false", "'Item', 'Supplier', 0.00, 5, false",
				"'Item', 'Supplier', 10.00, 0, false", "'Item', 'Supplier', -5.00, 5, false",
				"'Item', 'Supplier', 10.00, -1, false" })
		@DisplayName("Should validate data correctly")
		void testValidationParameterized(String itemName, String supplierName, BigDecimal price, int numberOfStickers,
				boolean expectedValid) {
			StickerData data = new StickerData(itemName, supplierName, price, numberOfStickers);

			assertEquals(expectedValid, data.validate().isEmpty());
		}

		@ParameterizedTest
		@CsvSource({
				"'Item1', 'Supplier1', 10.50, 5, 'StickerData{itemName=''Item1'', supplierName=''Supplier1'', price=10.50, numberOfStickers=5}'",
				"'', '', 0, 0, 'StickerData{itemName='''', supplierName='''', price=0, numberOfStickers=0}'",
				"'null', 'null', , 0, 'StickerData{itemName=''null'', supplierName=''null'', price=null, numberOfStickers=0}'" })
		@DisplayName("Should generate correct toString for various inputs")
		void testToStringParameterized(String itemName, String supplierName, BigDecimal price, int numberOfStickers,
				String expected) {
			StickerData data = new StickerData(itemName, supplierName, price, numberOfStickers);

			assertEquals(expected, data.toString());
		}
	}

	@Nested
	@DisplayName("Edge Cases and Boundary Tests")
	class EdgeCasesTests {

		@Test
		@DisplayName("Should handle maximum integer stickers")
		void testMaximumIntegerStickers() {
			StickerData data = new StickerData("Item", "Supplier", new BigDecimal("1.00"), Integer.MAX_VALUE);

			assertEquals(Integer.MAX_VALUE, data.getNumberOfStickers());
			assertTrue(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should handle minimum integer stickers")
		void testMinimumIntegerStickers() {
			StickerData data = new StickerData("Item", "Supplier", new BigDecimal("1.00"), Integer.MIN_VALUE);

			assertEquals(Integer.MIN_VALUE, data.getNumberOfStickers());
			assertFalse(data.validate().isEmpty()); // Because negative stickers
		}

		@Test
		@DisplayName("Should handle very large BigDecimal prices")
		void testVeryLargeBigDecimalPrice() {
			BigDecimal largePrice = new BigDecimal("999999999999999999999999999999999999999.999999999");
			StickerData data = new StickerData("Item", "Supplier", largePrice, 1);

			assertEquals(largePrice, data.getPrice());
			assertTrue(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should handle very small BigDecimal prices")
		void testVerySmallBigDecimalPrice() {
			BigDecimal smallPrice = new BigDecimal("0.000000001");
			StickerData data = new StickerData("Item", "Supplier", smallPrice, 1);

			assertEquals(smallPrice, data.getPrice());
			assertTrue(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should handle very long strings")
		void testVeryLongStrings() {
			String longString = "A".repeat(10000);
			StickerData data = new StickerData(longString, longString, new BigDecimal("1.00"), 1);

			assertEquals(longString, data.getItemName());
			assertEquals(longString, data.getSupplierName());
			assertTrue(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should handle special characters in strings")
		void testSpecialCharactersInStrings() {
			String special = "!@#$%^&*()_+-=[]{}|;:,.<>?";
			StickerData data = new StickerData(special, special, new BigDecimal("10.00"), 5);

			assertEquals(special, data.getItemName());
			assertEquals(special, data.getSupplierName());
			assertTrue(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should handle Unicode characters")
		void testUnicodeCharacters() {
			String unicode = "测试商品名称🚀";
			StickerData data = new StickerData(unicode, unicode, new BigDecimal("100.00"), 10);

			assertEquals(unicode, data.getItemName());
			assertEquals(unicode, data.getSupplierName());
			assertTrue(data.validate().isEmpty());
		}

		@Test
		@DisplayName("Should handle BigDecimal with different scales")
		void testBigDecimalDifferentScales() {
			BigDecimal price1 = new BigDecimal("10");
			BigDecimal price2 = new BigDecimal("10.0");
			BigDecimal price3 = new BigDecimal("10.00");

			StickerData data1 = new StickerData("Item", "Supplier", price1, 1);
			StickerData data2 = new StickerData("Item", "Supplier", price2, 1);
			StickerData data3 = new StickerData("Item", "Supplier", price3, 1);

			assertTrue(data1.validate().isEmpty());
			assertTrue(data2.validate().isEmpty());
			assertTrue(data3.validate().isEmpty());

			assertEquals(new BigDecimal("10"), data1.getPrice());
			assertEquals(new BigDecimal("10.0"), data2.getPrice());
			assertEquals(new BigDecimal("10.00"), data3.getPrice());
		}
	}
}