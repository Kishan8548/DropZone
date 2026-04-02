<img width="500" height="500" alt="DropZone Logo" src="https://github.com/user-attachments/assets/8383e75a-d594-4e86-b5b5-b3dd7cf184a6" />

# DropZone

DropZone is an Android app for reporting lost and found items. Users can create posts with item details, browse recent reports, contact the poster by email, and manage their own posts. The app now uses Gemini to suggest similar found posts when someone creates a lost-item post, and uses Cloudinary for image uploads while keeping post details in Firebase Firestore.

---

## Features

### 1. Create lost and found posts
Users can add a title, description, category, optional location, and optional image for a lost or found item.

<img src="https://github.com/user-attachments/assets/72dd1ecb-d97e-469b-95f8-af68bdab8314" alt="Create post screen" width="300" height="600">

### 2. Gemini-based similar post suggestions
When a user creates a `Lost` post, the app checks recent `Found` posts with matching category and keywords, then asks Gemini to identify likely matches. If matches are found, the user sees suggested posts before continuing.

### 3. Cloudinary image uploads
Only post images are uploaded to Cloudinary. Post text details such as title, description, category, status, location, and user info remain stored in Firestore. Uploaded images are compressed locally and delivered through Cloudinary optimized URLs.

### 4. Firestore-backed post feed
The home feed loads posts from Firestore in reverse chronological order and supports filtering by `All`, `Lost`, and `Found`.

### 5. Post details and contact
Each post has a dedicated detail page showing the full item information and image. Users can contact the poster through email.

<img src="https://github.com/user-attachments/assets/85232110-a02c-445c-96b2-e69e32e40190" alt="Post details page" width="300" height="600">

### 6. Profile and post management
Users can view their own posts from the profile screen and delete posts they created.

<img src="https://github.com/user-attachments/assets/7a5ce29d-d97e-43cb-b806-ba3934299d30" alt="Profile page" width="300" height="600">

### 7. Authentication
The app uses Firebase Authentication for sign-in and account management.

### 8. Push notifications
Firebase Cloud Messaging is integrated for notifications.

<img src="https://github.com/user-attachments/assets/a572e0c3-46ff-47f9-aac3-6dcbd772542a" alt="Notification" width="300" height="600">

### 9. Donation feature
DropZone includes a donation screen that opens UPI-enabled apps such as PhonePe, Paytm, and Google Pay using Android intents.

### 10. Splash screen and dark mode
The app includes an animated splash experience and supports dark mode.

---

## Tech Stack

- Kotlin
- Android SDK / AndroidX
- Firebase Authentication
- Firebase Cloud Firestore
- Firebase Cloud Messaging
- Firebase Crashlytics
- Cloudinary
- Gemini API
- Retrofit + Gson
- Glide
- RecyclerView
- CircleImageView
- Splash Screen API

---

## Data Storage

- Firestore stores:
  - post title
  - description
  - category
  - location
  - status
  - user details
  - image URL
  - Cloudinary image public ID
- Cloudinary stores:
  - image files only

Old posts that already contain Firebase Storage image URLs in Firestore will still display correctly because the app loads images from the stored `imageUrl`, regardless of whether that URL points to Firebase Storage or Cloudinary.

---

## Local Setup

1. Clone the repository.
2. Open the project in Android Studio.
3. Add the required keys to `local.properties`.
4. Sync Gradle.
5. Run the app on an emulator or Android device.

Example `local.properties` entries:

```properties
sdk.dir=YOUR_ANDROID_SDK_PATH
GEMINI_API_KEY=your_gemini_api_key
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_UPLOAD_PRESET=your_unsigned_upload_preset
```

Notes:
- `local.properties` is gitignored and should not be committed.
- The current app upload flow expects an unsigned Cloudinary upload preset.
- Cloudinary API secrets should not be shipped inside a production Android app.

---

## Build

To verify the Kotlin source compiles:

```bash
./gradlew :app:compileDebugKotlin
```

On Windows:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

---

## Demo Video

[Watch Demo](https://github.com/user-attachments/assets/72aeaf22-e086-4b85-a660-0c3c4e8b8ac6)

---

## Future Improvements

- Migrate old Firebase Storage images to Cloudinary
- Add secure server-side Cloudinary deletion
- Improve search and matching beyond category and keyword shortlist
- Add richer notification workflows for possible matches
