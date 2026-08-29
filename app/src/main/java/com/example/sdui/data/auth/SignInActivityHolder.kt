package com.example.sdui.data.auth

import android.app.Activity
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignInActivityHolder @Inject constructor() {
  @Volatile
  private var attachedActivity: WeakReference<Activity>? = null

  fun attach(activity: Activity) {
    attachedActivity = WeakReference(activity)
  }

  fun detach(activity: Activity) {
    if (attachedActivity?.get() === activity) attachedActivity = null
  }

  fun currentActivityOrNull(): Activity? = attachedActivity?.get()
}
