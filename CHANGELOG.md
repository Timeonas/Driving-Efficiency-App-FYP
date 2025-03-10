
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

## 4/2/25
- Implemented profile picture functionality, so users can add a profile picture to the profile screen
- Stored profile picture in Firebase Storage, and retrieve it when a user signs in

## 5/2/25
- Optimised the profile picture loading and fixed bugs associated with it
- Removed firebase api key from the project repo
- Removed files and folders that were not being used
- Added ability to delete trips
- Made the tracking trip notification not cancelable

## 6/2/25
- Developed the README.md.
- Start made on the OBD2 implementation activity

## 7/2/25
- Refactored obdconnectactivity to only pick up broadcasting BT devices
- Implemented way to insert bt paring code to connect to bt device.
- Layout for insert paring code created 

## 17/2/25
- Removed multiple instances of the same bt device appearing in the list
- Removed the pairing code functionality, as it was not needed, done automatically by the app
- Simplified the connection process, only the specific obd device is shown in the list
- Simplified OBD screen
- OBD finally working, parses the engine rpm, speed and coolant temp in real time.
- Now it parses constantly, and updates the UI with the new data
- Fixed the logic associated with the obdactivity screen
- Start made to gear logic

## 18/2/25
- Fix to final gear 2 logic
- Implemented UI changes to add trip summary info (fuel related) and more obd data
- Removed scanning for bt devices, unnecessary if OBD needs to be paired to the phone.(Needs to be tested)

## 24/2/25
- Started implementing fuel logic
- Fuel flow working
- Removed instant fuel consumption, as its highly inaccurate using MAF calculations.
- Start made on integrating obd activity into the main project
- Moved instant obd data to the start drive activity

## 2/3/25
- Create the trip summary screen for the drive activity
- Implemented overview trip data
- Foundation set for driving efficiency score
- Extended Trip information now saved to Firestore
- Detailed view of trip data now available
- Added final todo's to the project

## 3/3/25
- Added RPM stats to the trip summary screen
- Added proper efficiency score calculation based on the speed,fuel and rpm
- Added the score to the trip card screens
- Added rpm info on the cards too

## 4/3/25
- Improved efficiency score calculation and fixed the post trip dialog screen
- Score is now stored in the db

## 10/3/25
- Created a classification neural network model to classify a history of driving behaviour into specific categories 