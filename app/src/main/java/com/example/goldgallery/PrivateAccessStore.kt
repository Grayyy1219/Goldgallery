package com.example.goldgallery

import android.content.Context

object PrivateAccessStore {
    private const val PREFS_NAME = "private_access_prefs"
    private const val KEY_PIN = "key_pin"
    private const val KEY_SECURITY_QUESTION = "key_security_question"
    private const val KEY_SECURITY_ANSWER = "key_security_answer"

    fun hasPin(context: Context): Boolean {
        return prefs(context).getString(KEY_PIN, null) != null
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        return pin == prefs(context).getString(KEY_PIN, null)
    }

    fun savePinAndRecovery(context: Context, pin: String, question: String, answer: String) {
        prefs(context).edit()
            .putString(KEY_PIN, pin)
            .putString(KEY_SECURITY_QUESTION, question)
            .putString(KEY_SECURITY_ANSWER, answer.trim())
            .apply()
    }

    fun resetPin(context: Context, pin: String) {
        prefs(context).edit()
            .putString(KEY_PIN, pin)
            .apply()
    }

    fun getSecurityQuestion(context: Context): String {
        return prefs(context).getString(KEY_SECURITY_QUESTION, "") ?: ""
    }

    fun verifySecurityAnswer(context: Context, answer: String): Boolean {
        val expected = prefs(context).getString(KEY_SECURITY_ANSWER, "") ?: ""
        return expected.equals(answer.trim(), ignoreCase = true)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
