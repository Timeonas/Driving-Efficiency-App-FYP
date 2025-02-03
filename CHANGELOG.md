
## 14/1/25
- Added a new basic app

## 15/1/25
- Created CHANGELOG.md
- Created an intro splash screen to the app, to be improved later

## 16/1/25
- Created the main menu, featuring two buttons, one to start a trip and one to view past trips.
- Added a new app icon
- Added a basic start trip screen, with a button to start the trip and a elapsed time counter

## 17/1/25
- Added a basic trip summary screen, with sample trips. Documentation and comments added to all Kotlin files.
- Added additional functionality to the start trip, now trips are saved in memory to the view trips screen
- Linting fixes
- Added Firebase to the project, with a basic implementation of Firebase Authentication
- Added a login screen, with a text fields to login with email and password
- Converted to Android Jetpack ViewModels
- TODO: ~~Implement Register screen~~
- Added a profile screen, so users can sign out, with a default profile picture
- Fixed a bug where the duration of the trip was not being saved

## 21/1/25
- Added a register screen that allows users to register with email and password on Firebase
- Added a TODO file, to keep track of future features and improvements
- Implemented Google Maps API functionality, ready to implement maps in the app
- Added reset password functionality to the app.
- Sorted the Kotlin files into different files and folders to make it more organized
- Added a basic map view to the app, showing the user's current location when a trip starts

## 22/1/25
- Implemented map tilting and fixed app crash on consecutive trip starts.

## 26/1/25
- Improved map view orientation, so that it is always facing the direction the user is driving

## 3/2/25
- Reformatted Changelog
- Implemented Firestore to store trips
- Created TripRepository to handle Firestore operations
- Fixed bug where location was not working on some devices
- Offline functionality added to the app, so that trips are cached locally if the user is offline
- Added constant notification when a trip starts
- Refactored StartDriveActivity.kt to reduce code repetition
- Fixed bug where first trip was not being saved