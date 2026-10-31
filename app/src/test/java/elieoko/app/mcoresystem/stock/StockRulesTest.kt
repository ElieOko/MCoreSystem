package elieoko.app.mcoresystem.stock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StockRulesTest {
    @Test
    fun parseAmountAllowsZeroAndRejectsNegative() {
        assertEquals(0.0, StockRules.parseAmount("")!!, 0.0)
        assertEquals(0.0, StockRules.parseAmount("0")!!, 0.0)
        assertEquals(12.5, StockRules.parseAmount("12,5")!!, 0.0001)
        assertNull(StockRules.parseAmount("-1"))
        assertNull(StockRules.parseAmount("abc"))
    }

    @Test
    fun parseQuantityAcceptsCommaAndDot() {
        assertEquals(2.5, StockRules.parseQuantity("2,5")!!, 0.0001)
        assertEquals(10.0, StockRules.parseQuantity("10")!!, 0.0001)
        assertNull(StockRules.parseQuantity("0"))
        assertNull(StockRules.parseQuantity("-3"))
        assertNull(StockRules.parseQuantity("abc"))
    }

    @Test
    fun cannotGiveMoreThanCentralStock() {
        assertNull(StockRules.giveError(central = 10.0, requested = 4.0))
        assertEquals(
            "Stock central insuffisant (3 disponible)",
            StockRules.giveError(central = 3.0, requested = 5.0)
        )
    }

    @Test
    fun cannotTakeMoreThanMiniStock() {
        assertNull(StockRules.takeFromMiniError(available = 2.0, requested = 2.0))
        assertEquals(
            "Mini-stock insuffisant (1 disponible)",
            StockRules.takeFromMiniError(available = 1.0, requested = 4.0)
        )
    }

    @Test
    fun subtractClearsDust() {
        assertEquals(0.0, StockRules.subtract(5.0, 5.0), 0.0)
        assertEquals(1.5, StockRules.subtract(4.0, 2.5), 0.0001)
    }

    @Test
    fun formatWholeNumbersWithoutDecimals() {
        assertEquals("12", StockRules.formatQty(12.0))
        assertEquals("1,50", StockRules.formatQty(1.5))
    }

    @Test
    fun usernameRejectsShortOrSpacedValues() {
        assertNull(StockRules.normalizeUsername("ab"))
        assertNull(StockRules.normalizeUsername("jean paul"))
        assertEquals("jean", StockRules.normalizeUsername(" jean "))
    }

    @Test
    fun orderBecomesDeliveredOnlyWhenEveryLineIsCovered() {
        assertEquals(
            "EN_LIVRAISON",
            StockRules.nextOrderStatus(listOf(5.0 to 2.0, 1.0 to 1.0))
        )
        assertEquals(
            "LIVREE",
            StockRules.nextOrderStatus(listOf(5.0 to 5.0, 1.0 to 2.0))
        )
    }
}
