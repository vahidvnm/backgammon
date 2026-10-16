package com.dara.backgammon.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.dara.backgammon.BuildConfig
import com.dara.backgammon.R

object MarketActions {
    private fun open(context: Context, uri: String, action: String = Intent.ACTION_VIEW) {
        val intent = Intent(action, Uri.parse(uri)).apply {
            setPackage(BuildConfig.STORE_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { Toast.makeText(context, context.getString(R.string.store_unavailable, BuildConfig.STORE_NAME), Toast.LENGTH_LONG).show() }
    }

    fun checkUpdate(context: Context) = open(context, BuildConfig.UPDATE_URI)
    fun openStorePage(context: Context) = open(context, BuildConfig.DETAILS_URI)
    fun rate(context: Context) = open(
        context,
        BuildConfig.RATE_URI,
        if (BuildConfig.FLAVOR == "bazaar") Intent.ACTION_EDIT else Intent.ACTION_VIEW
    )

    fun support(context: Context) {
        val address = context.getString(R.string.support_email)
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")).apply {
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.support_subject))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { Toast.makeText(context, R.string.support_unavailable, Toast.LENGTH_LONG).show() }
    }
}
