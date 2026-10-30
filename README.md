AlertX (SafeReport)

An Android app for reporting safety incidents in a community and following up on them. Users can file a report with a location and photo, send an SOS, and track the report afterwards. Security staff get a separate side of the app to manage what comes in.

I built it in Java with Firebase as the backend. Drafts are saved locally when there's no connection and uploaded later.

 Features

Accounts
- Sign in and registration for users and admins
- Three roles: `USER`, `SECURITY`, `ADMIN`
- Option to report anonymously, so the report isn't tied to your account
- Biometric check before security staff can open the admin side

Reporting and location
- Current location is picked up with FusedLocationProviderClient
- You can also tap on a Google Map to set the incident location manually
- Photos can be attached to a report

SOS
- A red extended FAB sends an emergency alert to security staff

Tracking
- Each report gets a tracking code like `INC-83749210`, which anonymous users can use to check on their case
- Status moves through `SUBMITTED`, `UNDER_REVIEW`, `IN_PROGRESS` and `RESOLVED`, with a timeline of updates

Admin side
- Review reports, change status, assign responders and add notes to the timeline
- Pie and bar charts (MPAndroidChart) for incidents by category and by status

Offline support
- Drafts are stored in Room when the device is offline
- A WorkManager job syncs them to Firebase once the connection is back

 Tech used

- Java, Android SDK (min API 24)
- MVVM with a repository layer
- Material 3, with a dark theme
- Firebase: Firestore, Authentication, Cloud Storage, Cloud Messaging
- Room, SharedPreferences, WorkManager
- Google Maps SDK, MPAndroidChart, Glide

 Project structure

```text
com.yourname.safereport/
├── data/local/     Room database, DAOs, draft entities
├── model/          Incident, IncidentUpdate, User
├── repository/     AuthRepository, IncidentRepository
├── service/        FCM push notification service
├── ui/             Activities, fragments and adapters (user and admin)
├── utils/          Constants, permission helpers, tracking code generator
├── viewmodel/      AuthViewModel, ReportViewModel, AdminViewModel
└── worker/         UploadWorker for offline sync
```

 Running it
1. Clone the repo and open it in Android Studio.
2. Create a Firebase project and add your own `google-services.json` to the `app/` folder.
3. Add a Google Maps API key (in `local.properties` or the manifest, depending on how you set it up).
4. Build and run on a device or emulator with Google Play services.
