package com.gesturedot.app.data

fun shouldShowPermissionOnboarding(
    onboardingSeen: Boolean?,
    serviceEnabled: Boolean,
): Boolean = onboardingSeen == false && !serviceEnabled
