package com.web3auth.core.types

class UserCancelledException : Exception("User cancelled.")

class UnKnownException(errorStr: String) : Exception(errorStr)
