<img src="assets/header.png" />

# Grocy KMP

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)  [![Release](https://img.shields.io/github/v/release/patzly/grocy-kmp?label=Release)](https://github.com/patzly/grocy-kmp/releases)  [![APK Downloads](https://img.shields.io/github/downloads/patzly/grocy-kmp/total.svg?label=APK%20Downloads)](https://github.com/patzly/grocy-kmp/releases)

> **🤖 This project is developed with the help of AI.** Grocy KMP is a very large undertaking: a complete client for every part of Grocy on phones, tablets, watches and the web. I wouldn't have the time to build it alone, and without AI it would have cost at least several years of my spare time. Every change is still planned, reviewed and tested by me.

Grocy KMP is an open-source client for [Grocy](https://grocy.info/) ([source code](https://github.com/grocy/grocy)), a self-hosted grocery and household management solution for your home.

It is the successor of [Grocy Android](https://github.com/patzly/grocy-android), rebuilt from the ground up with Kotlin Multiplatform and Compose Multiplatform. It brings all the features Grocy Android was missing, uses the most modern technologies and follows current best practices: Material 3 Expressive, layouts for every screen size, offline-first data and thorough tests. The same code powers the Android app, the Wear OS app and the web app.

Grocy KMP uses Grocy's official API. Its pages, forms and lists match Grocy's web frontend, so you find your way immediately, with a native interface, powerful barcode scanning and as few taps as possible.

**This app requires a running self-hosted instance of the [Grocy server application](https://grocy.info/).**  
It is a **companion** app, therefore it **cannot run standalone** or manage products itself.  
You can try it using the demo option available on the login screen.

## Downloads

<a href='https://play.google.com/store/apps/details?id=com.patrickzedler.grocy'><img alt='Get it on Google Play' height="80" src='assets/badge_playstore.png'/></a><a href='https://github.com/patzly/grocy-kmp/releases'><img alt='Get it on GitHub' height="80" src='assets/badge_github.png'/></a>

If you want to try new features right after development, you can download a [nightly build APK](https://github.com/patzly/grocy-kmp/releases/tag/nightly) for that.

Grocy KMP is a new app with its own package name, so it does not update Grocy Android. Install it next to it, log in once and uninstall Grocy Android when everything works.

The Wear OS app is installed from Google Play on your watch; it takes the login from the Google Play version of the phone app by itself.

### Web app

The web app runs in every current browser, without installing anything: open **[patzly.github.io/grocy-kmp](https://patzly.github.io/grocy-kmp/)** and log in.

To use it like an app, install it from the browser: in Chrome or Edge with "Install" in the address bar or the menu, on Android with "Add to home screen", in Safari on iPhone or iPad with "Share" › "Add to Home Screen". It then opens in its own window, starts without loading the app again and updates itself after asking you.

Your Grocy server must be reachable with `https`, because browsers block plain `http` requests from a secure page. Logging in through Home Assistant ingress is not possible in browsers; assign a port to the Grocy add-on instead.

## Why Grocy KMP?

Grocy's web frontend works in any browser and covers everything. Grocy KMP shows the same pages, forms and lists, made for your phone, tablet and watch:

* **Barcode scanning** with a native camera scanner right in every form; with the unlock app also the due dates printed on packages
* **Offline shopping**: check off items without a connection, they are sent when you are back online without overwriting what others changed
* **Everything at a glance** on the start screen: what is due, what is missing, what is planned for today
* **Notifications** every day about due products, chores, tasks and batteries
* **Home screen widgets** and a **Wear OS app** with a tile for the shopping list
* **NFC tags and QR codes** with `grocy://` links, e.g. one at the pantry that opens Consume for a product, or one at the washing machine that tracks a chore
* **New products filled in** from Open Food Facts and its sister databases
* **Several accounts and servers**, switched with one swipe
* **Material 3 Expressive** design with dynamic colors, dark mode and layouts for every screen size, from phones to desktops
* **Accessibility** in every screen: labels for screen readers, headings, announced errors, large touch targets and full keyboard and mouse support

## Features

* Start screen with an overview of everything, in the order you choose
* Stock overview with reports, stock entries and stock journal
* Purchase, consume, transfer and inventory
* Shopping lists with offline support and in-store shopping mode
* Recipes with their cards and the meal plan
* Chores, tasks, batteries, equipment and calendar
* Editing of all master data, including userfields and userentities
* Fast barcode scanning, also of Grocycodes
* Open Food Facts and its sister databases fill in new products
* Daily notifications about what is due
* Home screen widgets for the shopping list, tasks and calendar
* Wear OS app with the shopping list and a tile
* Web app that works like the Android app
* Several accounts and servers, quick switching between them
* Layouts for phones, foldables, tablets and desktops
* Dark mode and dynamic color support
* Accessible with screen readers, keyboard and mouse
* No ads or analytics

## Unlock app

The [unlock app](https://play.google.com/store/apps/details?id=xyz.zedler.patrick.grocy.unlock) is a small purchase on Google Play that supports the development. It is the same one as for Grocy Android, so if you already bought it, it works here too. Once installed, the Google Play version of Grocy KMP can use the ML Kit barcode scanner and scan the due dates printed on packages.

## Compatibility

Grocy KMP requires at least Android 7.0 Nougat on phones and tablets and Wear OS 3 on watches. The GitHub version works properly on devices without any Google service installed.

It is also possible to use the Grocy add-on of a [Home Assistant](https://www.home-assistant.io/getting-started/) server; the login supports its ingress directly.

This app supports `https` encryption
- if your server uses a certificate which is signed by a public and trusted [certificate authority (CA)](https://en.wikipedia.org/wiki/Certificate_authority). To meet this requirement, you can use a free certificate from [letsencrypt.org](https://letsencrypt.org/) for your server.
- if your server uses a self-signed certificate. In this case the certificate must be stored in the Android user certificate store.
- if your server requires a client certificate, which you can add in the server settings of the app.

Connections can also go through an HTTP proxy or Tor.

## Contribution

If you run into a bug or miss a feature, please [open an issue](https://github.com/patzly/grocy-kmp/issues) in this repository. If you have a good knowledge of Kotlin and Compose, pull requests are highly appreciated!

Like the grocy project, Grocy KMP can be translated, too. The main language is English, but I also maintain the German translation because it's my mother tongue.
You can help translate this project at [Transifex](https://app.transifex.com/patzly/grocy-kmp), if your language is incomplete, contains typos or is not available yet.

Thanks for your contributions!

## About

Grocy KMP is developed by me, [Patrick Zedler](https://github.com/patzly). It continues Grocy Android, which my brother [Dominic Zedler](https://github.com/dominiczedler) and I started in 2020.

I'd like to thank the developer of Grocy, [Bernd Bestel](https://berrnd.de/), without whose great work this app would never have been possible.

## License

Copyright &copy; 2026 Patrick Zedler. All rights reserved.

[GNU General Public License version 3](https://www.gnu.org/licenses/gpl.txt)

> Grocy KMP is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
>
> Grocy KMP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
