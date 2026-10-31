package elieoko.app.mcoresystem.stock.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PostgREST remplit par NULL toute colonne absente d'une ligne lors d'un upsert groupé.
 * Le sérialiseur Supabase n'écrit pas les valeurs par défaut, donc « pièce » et 0
 * disparaissaient et violaient la contrainte NOT NULL de stock_products.unit.
 */
class StockSyncPayloadTest {
    private val json = Json { encodeDefaults = false }

    @Test
    fun defaultUnitAndPriceStayInThePayload() {
        val oil = RemoteStockProduct(
            uuid = "2d339c6a-5444-4d56-872e-03f458ee74f8",
            organismUuid = "6083f5fb-c581-49f2-ba91-1bd74ea482f5",
            name = "Huile vegetal",
            centralQuantity = 2.0,
            updatedAt = "2026-10-31T16:17:34Z"
        )
        val encoded = json.encodeToString(RemoteStockProduct.serializer(), oil)
        assertTrue(encoded.contains("\"unit\""))
        assertTrue(encoded.contains("\"unit_price\""))
        assertTrue(encoded.contains("\"central_quantity\":2"))
        assertFalse(encoded.contains("null"))
    }

    @Test
    fun nullableLivreurLinkStaysOmittedWhenMissing() {
        val livreur = RemoteStockLivreur(
            uuid = "livreur",
            organismUuid = "org",
            name = "mars"
        )
        val encoded = json.encodeToString(RemoteStockLivreur.serializer(), livreur)
        assertTrue(encoded.contains("\"name\":\"mars\""))
        assertTrue(encoded.contains("\"phone\""))
        assertFalse(encoded.contains("user_uuid"))
    }
}
