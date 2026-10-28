package com.secondhandmarket.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore(name = "second_hand_market_auth")

data class StoredProfile(
    val fullName: String?,
    val state: String?,
    val lga: String?,
    val address: String?
)

/**
 * Small key-value store for the JWT and the user's profile fields.
 * Acts as the shared state carrier between navigation destinations because
 * each screen owns its own AuthViewModel instance.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val TOKEN = stringPreferencesKey("jwt_token")
        val PHONE = stringPreferencesKey("phone_number")
        val FULL_NAME = stringPreferencesKey("full_name")
        val STATE = stringPreferencesKey("state")
        val LGA = stringPreferencesKey("lga")
        val ADDRESS = stringPreferencesKey("address")
        val PROFILE_COMPLETE = booleanPreferencesKey("profile_complete")
    }

    suspend fun saveAuth(token: String, phoneNumber: String) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.PHONE] = phoneNumber
        }
    }

    suspend fun authToken(): String? =
        context.authDataStore.data.first()[Keys.TOKEN]

    suspend fun savedPhoneNumber(): String? =
        context.authDataStore.data.first()[Keys.PHONE]

    suspend fun saveProfile(fullName: String, state: String, lga: String, address: String) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.FULL_NAME] = fullName
            prefs[Keys.STATE] = state
            prefs[Keys.LGA] = lga
            prefs[Keys.ADDRESS] = address
            prefs[Keys.PROFILE_COMPLETE] = true
        }
    }

    suspend fun isProfileComplete(): Boolean =
        context.authDataStore.data.first()[Keys.PROFILE_COMPLETE] ?: false

    /** Observable profile for screens that want live updates. */
    fun profileFlow(): Flow<StoredProfile> = context.authDataStore.data.map { prefs ->
        StoredProfile(
            fullName = prefs[Keys.FULL_NAME],
            state = prefs[Keys.STATE],
            lga = prefs[Keys.LGA],
            address = prefs[Keys.ADDRESS]
        )
    }

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }
}
