[![Release](https://jitpack.io/v/Sublimis/SteadyService.svg)](https://jitpack.io/#Sublimis/SteadyService)

# ⛵ SteadyService library for Android and Wear 🏝️


## More info

Please see the [SteadyScreen](https://github.com/Sublimis/SteadyScreen) project for more details.


## Project components

- [Stilly app](https://play.google.com/store/apps/details?id=com.sublimis.steadyscreen): The engine behind the scenes.
- [SteadyViews library](https://github.com/Sublimis/SteadyViews): Ready-to-use "Steady…" implementations of most common Android layouts (like e.g. LinearLayout or ConstraintLayout).
- [SteadyView library](https://github.com/Sublimis/SteadyView): Core classes and methods. To be used for custom View or ViewGroup implementations.
- SteadyService library (this): Details of the service implementation.


## About the service

This service uses the [AccessibilityService API](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService) to retrieve interactive windows on the screen, in order to find compatible ones. It then sends multiple "move window" accessibility actions to such windows, as needed, to perform the intended function. The service never collects, stores nor shares any data that can be of personal and confidential nature in any way.

⚡ The service has been crafted very meticulously, in order to minimize resource usage and maximize performance.
