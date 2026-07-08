package com.masasaatim.presentation

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.masasaatim.MainActivity
import com.masasaatim.R

class MasaSaatiWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val remoteViews = RemoteViews(context.packageName, R.layout.masa_saati_widget_layout)

            val intent = Intent(context, MainActivity::class.java)

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

            remoteViews.setOnClickPendingIntent(R.id.widget_title, pendingIntent)
            remoteViews.setOnClickPendingIntent(R.id.widget_vakit_label, pendingIntent)
            remoteViews.setOnClickPendingIntent(R.id.widget_countdown, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }
}
