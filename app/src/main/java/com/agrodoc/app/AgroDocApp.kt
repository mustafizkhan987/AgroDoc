package com.agrodoc.app

import dagger.hilt.android.HiltAndroidApp

/**
 * Application class wired to Hilt's component hierarchy.
 * No network calls, analytics or crash-reporting initialised here by design.
 */
@HiltAndroidApp
class AgroDocApp : android.app.Application()
