package com.shounakmulay.flutter_sms.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import com.shounakmulay.flutter_sms.utils.Constants.ACTION_SMS_DELIVERED
import com.shounakmulay.flutter_sms.utils.Constants.ACTION_SMS_SENT
import com.shounakmulay.flutter_sms.utils.Constants.SMS_BODY
import com.shounakmulay.flutter_sms.utils.Constants.SMS_DELIVERED_BROADCAST_REQUEST_CODE
import com.shounakmulay.flutter_sms.utils.Constants.SMS_SENT_BROADCAST_REQUEST_CODE
import com.shounakmulay.flutter_sms.utils.Constants.SMS_TO
import com.shounakmulay.flutter_sms.utils.ContentUri


class SmsController(private val context: Context) {

  // FETCH SMS
  fun getConversations(
      contentUri: ContentUri,
      projection: List<String>,
      selection: String?,
      selectionArgs: List<String>?,
      sortOrder: String?
  ): List<HashMap<String, String?>> {
    return getQueryResult(contentUri, projection, selection, selectionArgs, sortOrder)
  }

  fun getMessages(
      contentUri: ContentUri,
      projection: List<String>,
      selection: String?,
      selectionArgs: List<String>?,
      sortOrder: String?
  ): List<HashMap<String, String?>> {
    return getQueryResult(contentUri, projection, selection, selectionArgs, sortOrder)
  }

  private fun getQueryResult(
      contentUri: ContentUri,
      projection: List<String>,
      selection: String?,
      selectionArgs: List<String>?,
      sortOrder: String?
  ): MutableList<HashMap<String, String?>> {
    val messages = mutableListOf<HashMap<String, String?>>()

    val cursor = context.contentResolver.query(
        contentUri.uri,
        null,
        selection,
        selectionArgs?.toTypedArray(),
        sortOrder
    )

    while (cursor != null && cursor.moveToNext()) {
      val dataObject = HashMap<String, String?>(projection.size)
      for (columnName in cursor.columnNames) {
        val value = cursor.getString(cursor.getColumnIndex(columnName))
        dataObject[columnName] = value
      }
      messages.add(dataObject)
    }

    cursor?.close()

    return messages

  }

  // SEND SMS
  fun sendSms(destinationAddress: String, messageBody: String, listenStatus: Boolean) {
    val smsManager = getSmsManager()
    if (listenStatus) {
      val pendingIntents = getPendingIntents()
      smsManager.sendTextMessage(destinationAddress, null, messageBody, pendingIntents.first, pendingIntents.second)
    } else {
      smsManager.sendTextMessage(destinationAddress, null, messageBody, null, null)
    }
  }

  fun sendMultipartSms(destinationAddress: String, messageBody: String, listenStatus: Boolean) {
    val smsManager = getSmsManager()
    val messageParts = smsManager.divideMessage(messageBody)
    if (listenStatus) {
      val pendingIntents = getMultiplePendingIntents(messageParts.size)
      smsManager.sendMultipartTextMessage(destinationAddress, null, messageParts, pendingIntents.first, pendingIntents.second)
    } else {
      smsManager.sendMultipartTextMessage(destinationAddress, null, messageParts, null, null)
    }
  }

  private fun getMultiplePendingIntents(size: Int): Pair<ArrayList<PendingIntent>, ArrayList<PendingIntent>> {
    val sentPendingIntents = arrayListOf<PendingIntent>()
    val deliveredPendingIntents = arrayListOf<PendingIntent>()
    for (i in 1..size) {
      val pendingIntents = getPendingIntents()
      sentPendingIntents.add(pendingIntents.first)
      deliveredPendingIntents.add(pendingIntents.second)
    }
    return Pair(sentPendingIntents, deliveredPendingIntents)
  }

  fun sendSmsIntent(destinationAddress: String, messageBody: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
      data = Uri.parse(SMS_TO + destinationAddress)
      putExtra(SMS_BODY, messageBody)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    if (intent.resolveActivity(context.packageManager) != null) {
      context.applicationContext.startActivity(intent)
    }
  }

  private fun getPendingIntents(): Pair<PendingIntent, PendingIntent> {
    val sentIntent = Intent(ACTION_SMS_SENT).apply {
      `package` = context.applicationContext.packageName
      flags = Intent.FLAG_RECEIVER_REGISTERED_ONLY
    }
    val sentPendingIntent = PendingIntent.getBroadcast(context, SMS_SENT_BROADCAST_REQUEST_CODE, sentIntent, PendingIntent.FLAG_ONE_SHOT)

    val deliveredIntent = Intent(ACTION_SMS_DELIVERED).apply {
      `package` = context.applicationContext.packageName
      flags = Intent.FLAG_RECEIVER_REGISTERED_ONLY
    }
    val deliveredPendingIntent = PendingIntent.getBroadcast(context, SMS_DELIVERED_BROADCAST_REQUEST_CODE, deliveredIntent, PendingIntent.FLAG_ONE_SHOT)

    return Pair(sentPendingIntent, deliveredPendingIntent)
  }

  private fun getSmsManager(): SmsManager {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
      val subscriptionId = SmsManager.getDefaultSmsSubscriptionId()
      if (subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
        return SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
      }
    }
    return SmsManager.getDefault()
  }
}