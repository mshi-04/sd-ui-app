package com.example.sdui.domain.model

import com.example.sdui.domain.model.value.AccountId
import com.example.sdui.domain.model.value.AccountName

data class AuthAccount(
  val id: AccountId,
  val name: AccountName,
)
