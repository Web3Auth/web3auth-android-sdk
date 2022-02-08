package com.web3auth.core.types

import java.lang.Exception

class UserCancelledException : Exception("User cancelled.")

class UnKnownException(errorStr : String) : Exception(errorStr)
