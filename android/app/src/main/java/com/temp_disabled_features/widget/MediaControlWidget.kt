package com.astralplayer.nextplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Home screen widget for quick media controls
 */
class MediaControlWidget : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, android.R.layout.activity_list_item)
        
        // Set up button intents
        views.setOnClickPendingIntent(
            android.R.id.text1,
            createAppIntent(context)
        )
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    
    private fun createPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun createAppIntent(context: Context): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    companion object {
        const val ACTION_PLAY_PAUSE = "com.astralplayer.nextplayer.WIDGET_PLAY_PAUSE"
        const val ACTION_PREVIOUS = "com.astralplayer.nextplayer.WIDGET_PREVIOUS"
        const val ACTION_NEXT = "com.astralplayer.nextplayer.WIDGET_NEXT"
    }
}