package com.patrickzedler.grocy.feature.login.impl

import com.patrickzedler.grocy.core.data.auth.AuthRepository

/** What the login feature needs from the app graph. */
interface LoginDependencies {
    val authRepository: AuthRepository
}
