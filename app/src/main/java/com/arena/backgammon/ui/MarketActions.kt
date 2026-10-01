package com.arena.backgammon.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.arena.backgammon.BuildConfig

object MarketActions {
    private fun open(context: Context, uri: String, action: String = Intent.ACTION_VIEW) {
        val intent = Intent(action, Uri.parse(uri)).apply {
            setPackage(BuildConfig.STORE_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { Toast.makeText(context, "برای انجام این کار، ${BuildConfig.STORE_NAME} باید نصب باشد.", Toast.LENGTH_LONG).show() }
    }

    fun checkUpdate(context: Context) = open(context, BuildConfig.UPDATE_URI)
    fun openStorePage(context: Context) = open(context, BuildConfig.DETAILS_URI)
    fun rate(context: Context) = open(
        context,
        BuildConfig.RATE_URI,
        if (BuildConfig.FLAVOR == "bazaar") Intent.ACTION_EDIT else Intent.ACTION_VIEW
    )

    fun support(context: Context) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:bahramiuan@gmail.com")).apply {
            putExtra(Intent.EXTRA_SUBJECT, "پشتیبانی تخته نرد هوشمند")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { Toast.makeText(context, "برنامه‌ای برای ارسال پیام پشتیبانی پیدا نشد.", Toast.LENGTH_LONG).show() }
    }
}
