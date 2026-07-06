package elieoko.app.mcoresystem.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mcore_settings")

class ExchangeRateRepository(private val context: Context) {

    companion object {
        val USD_TO_CDF_RATE = doublePreferencesKey("usd_to_cdf_rate")
        const val DEFAULT_RATE = 2800.0
        const val CURRENCY_CDF_CODE = "CDF"
        const val CURRENCY_USD_CODE = "USD"
        const val CURRENCY_CDF_ID = 1
        const val CURRENCY_USD_ID = 2
    }

    val usdToCdfRate: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[USD_TO_CDF_RATE] ?: DEFAULT_RATE
    }

    suspend fun setUsdToCdfRate(rate: Double) {
        context.dataStore.edit { preferences ->
            preferences[USD_TO_CDF_RATE] = rate
        }
    }
}
