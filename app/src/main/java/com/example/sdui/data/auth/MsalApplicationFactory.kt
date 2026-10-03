package com.example.sdui.data.auth

import android.content.Context
import com.example.sdui.R
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MsalApplicationFactory @Inject constructor(
  @param:ApplicationContext private val context: Context,
) {
  fun create(): ISingleAccountPublicClientApplication =
    PublicClientApplication.createSingleAccountPublicClientApplication(context, R.raw.msal_config)
}
