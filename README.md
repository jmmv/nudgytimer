# Nudgy Timer

Nudgy Timer is a simple time tracker for Android that helps you gain
insight on where your precious time is being spent.

When time tracking is enabled, Nudgy Timer will subtly poke you ever few
minutes and ask you to record what you have been doing since the last poke.
You can later inspect your time records to discover where your time has
gone.

Nudgy Timer is released under the [Apache 2.0 license](LICENSE) and is
brought to you by [Julio Merino](CONTRIBUTORS).  This is not an official
Google product.

## The theory

Countless productivity courses begin with the following exercise:

1. Write down your priorities in life.
1. Over the course of a few days, write down what you are doing in
   15-minute intervals.
1. Aggregate the data you have captured to see where your time is actually
   going.
1. Compare your recording to your list of priorities and, if things don't
   match up, adjust the way you spend your time accordingly!

Nudgy Timer is, simply put, a tool that aids in implementing steps 2 and 3
in a non-intrusive Android application.  Remembering to write down what
you are ding every few minutes on paper is hard to get right without an
external system reminding you to do so.

## Download and install

At the time of this writing (2015-12-17), this application is not *yet*
available in the Google Play store and there are no [readily available
binary packages](NEWS.md).  Worry not, this will soon be fixed.

If you want to install Nudgy Timer, you will have to follow these steps
for the time being:

1. Check out the code from GitHub
1. Run `./gradlew assembleDebug` on Linux or `gradlew.bat assembleDebug`
   on Windows to build an APK.
1. Plug your phone to the computer.
1. Run `adb install -r app/build/outputs/apk/app-debug.apk` to install
   Nudgy Timer into the connected device.

Alternatively, use Android Studio to perform all the steps above.
