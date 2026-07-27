package com.example.fintrack.repository

import android.content.Context
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler
import com.amazonaws.regions.Regions
import com.amazonaws.services.cognitoidentityprovider.model.SignUpResult
import com.example.fintrack.config.AwsConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AuthRepository(context: Context) {

    private val userPool = CognitoUserPool(
        context,
        AwsConfig.USER_POOL_ID,
        AwsConfig.CLIENT_ID,
        null,
        Regions.fromName(AwsConfig.REGION)
    )

    // Signup Function
    suspend fun signUp(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        suspendCoroutine { continuation ->
            val userAttributes = CognitoUserAttributes()
            userAttributes.addAttribute("email", email)

            userPool.signUp(email, password, userAttributes, null, object : SignUpHandler {
                override fun onSuccess(user: CognitoUser?, signUpResult: SignUpResult?) {
                    val confirmationRequired = signUpResult?.userConfirmed?.not() ?: true
                    if (confirmationRequired) {
                        continuation.resume(Result.success("Signup Successful! Please check your email for the verification code."))
                    } else {
                        continuation.resume(Result.success("Signup Successful!"))
                    }
                }

                override fun onFailure(exception: Exception?) {
                    continuation.resume(Result.failure(exception ?: Exception("Signup failed")))
                }
            })
        }
    }

    // Login Function (Standard SDK v2.x Flow)
    suspend fun login(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        suspendCoroutine { continuation ->
            val user = userPool.getUser(email)

            user.getSessionInBackground(object : AuthenticationHandler {
                override fun onSuccess(
                    userSession: CognitoUserSession?,
                    newDevice: CognitoDevice?
                ) {
                    val idToken = userSession?.idToken?.jwtToken ?: ""
                    continuation.resume(Result.success(idToken))
                }

                override fun getAuthenticationDetails(
                    authenticationContinuation: AuthenticationContinuation?,
                    userId: String?
                ) {
                    val authDetails = AuthenticationDetails(userId, password, null)
                    authenticationContinuation?.setAuthenticationDetails(authDetails)
                    authenticationContinuation?.continueTask()
                }

                override fun getMFACode(multiFactorAuthenticationContinuation: MultiFactorAuthenticationContinuation?) {
                    // Not required for standard flow
                }

                override fun authenticationChallenge(challengeContinuation: ChallengeContinuation?) {
                    // Not required for standard flow
                }

                override fun onFailure(exception: Exception?) {
                    continuation.resume(Result.failure(exception ?: Exception("Login failed")))
                }
            })
        }
    }

    suspend fun confirmSignUp(email: String, code: String): Result<String> = withContext(Dispatchers.IO) {
        suspendCoroutine { continuation ->
            val user = userPool.getUser(email)

            user.confirmSignUpInBackground(code, false, object : com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler {
                override fun onSuccess() {
                    continuation.resume(Result.success("Account verified successfully! You can now log in."))
                }

                override fun onFailure(exception: Exception?) {
                    continuation.resume(Result.failure(exception ?: Exception("Verification failed")))
                }
            })
        }
    }
}