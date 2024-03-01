[![Release](https://jitpack.io/v/Sublimis/SteadyService.svg)](https://jitpack.io/#Sublimis/SteadyService)

# ⛵ [Stilly](https://play.google.com/store/apps/details?id=com.sublimis.steadyscreen) screen stabilizer service for Android and Wear 🏝️


## More info

Please see the [SteadyScreen](https://github.com/Sublimis/SteadyScreen) project for more details.


## Project components

- [Stilly app](https://play.google.com/store/apps/details?id=com.sublimis.steadyscreen): The engine behind the scenes.
- SteadyService library (this): If you want to implement your own screen stabilizer service that won't need Stilly.
- [SteadyViews library](https://github.com/Sublimis/SteadyViews): Ready-to-use "Steady…" implementations of most common Android layouts (like e.g. LinearLayout or ConstraintLayout).
- [SteadyView library](https://github.com/Sublimis/SteadyView): Core classes and methods. To be used for custom View or ViewGroup implementations.


## About the service

This service uses the [AccessibilityService API](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService) to retrieve interactive windows on the screen, in order to find compatible ones. The service then sends multiple "move window" accessibility actions to such windows, as needed, to perform the intended function. The data accessed during the process, using Android's AccessibilityService API, can be of personal and confidential nature (i.e. sensitive information). The service never collects, stores nor shares that data in any way.

⚡ The service has been crafted very meticulously, in order to minimize resource usage and maximize performance. It uses only the accelerometer sensor to achieve the goal.
