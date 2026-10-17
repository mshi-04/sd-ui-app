package com.example.sdui.data.auth

interface AccessTokenProvider {
  fun accessTokenOrNull(): String?
}
