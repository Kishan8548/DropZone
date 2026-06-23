<p align="center">
  <img src="https://github.com/user-attachments/assets/8383e75a-d594-4e86-b5b5-b3dd7cf184a6" alt="DropZone Banner" width="300"/>
</p>

<h1 align="center">DropZone</h1>

<p align="center">
  <strong>Your smart hub for lost and found items.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Firebase-%23039BE5.svg?style=for-the-badge&logo=firebase" alt="Firebase"/>
  <img src="https://img.shields.io/badge/Google%20Gemini-8E75B2?style=for-the-badge&logo=googlebard&logoColor=white" alt="Gemini"/>
  <img src="https://img.shields.io/badge/Cloudinary-3448C5?style=for-the-badge&logo=cloudinary&logoColor=white" alt="Cloudinary"/>
  <img src="https://img.shields.io/badge/Min%20SDK-24-green?style=for-the-badge" alt="Min SDK"/>
</p>

---

**DropZone** is a modern Android app for reporting lost and found items. Users can create posts with item details, browse recent reports, contact the poster by email, and manage their own posts. The app uses **Google Gemini AI** to intelligently suggest similar found posts when someone creates a lost-item post, ensuring reunited belongings faster than ever.

---

## Screenshots

### Core Experience

| Create Post | Post Details | Profile |
| :---: | :---: | :---: |
| <img src="https://github.com/user-attachments/assets/72dd1ecb-d97e-469b-95f8-af68bdab8314" width="220"/> | <img src="https://github.com/user-attachments/assets/85232110-a02c-445c-96b2-e69e32e40190" width="220"/> | <img src="https://github.com/user-attachments/assets/7a5ce29d-d97e-43cb-b806-ba3934299d30" width="220"/> |

### Alerts

| Notification | 
| :---: |
| <img src="https://github.com/user-attachments/assets/a572e0c3-46ff-47f9-aac3-6dcbd772542a" width="220"/> |

---

## Demo Video

[Watch Demo](https://github.com/user-attachments/assets/72aeaf22-e086-4b85-a660-0c3c4e8b8ac6)

---

## Features

### AI-Powered Suggestions
- When a user creates a `Lost` post, the app checks recent `Found` posts
- Uses **Google Gemini API** to identify likely matches based on category and keywords
- Users see intelligent suggested posts before finalizing their report

### Smart Post Management
- Create lost and found posts with title, description, category, optional location, and image
- Images uploaded directly to **Cloudinary** and compressed locally for optimized delivery
- Text details remain securely stored in **Firebase Firestore**

### Rich Home Feed & Details
- Home feed loads posts from Firestore in reverse chronological order
- Filter by `All`, `Lost`, and `Found`
- Dedicated detail page showing full item information and image
- Contact the poster directly through email from the app

### User Profile & Interaction
- View account info and manage own posts from the profile screen
- Delete posts you created easily
- Push notifications integrated via **Firebase Cloud Messaging**

### Donation & UI Polish
- Donation screen that opens UPI-enabled apps (PhonePe, Paytm, Google Pay) via intents
- Animated splash screen experience
- Full support for dark mode

---

## Tech Stack

- **Language:** Kotlin
- **Framework:** Android SDK / AndroidX
- **Backend:** Firebase Authentication, Cloud Firestore, Cloud Messaging, Crashlytics
- **Cloud Storage:** Cloudinary
- **AI Integration:** Google Gemini API
- **Networking:** Retrofit + Gson
- **Image Loading:** Glide
- **UI Components:** RecyclerView, CircleImageView, Splash Screen API

---

## Data Storage

- **Firestore stores:** Post title, description, category, location, status, user details, image URL, and Cloudinary image public ID.
- **Cloudinary stores:** Image files only.
- *Note:* Old posts that already contain Firebase Storage image URLs in Firestore will still display correctly because the app loads images from the stored `imageUrl`, regardless of whether that URL points to Firebase Storage or Cloudinary.

---

## Local Setup

1. Clone the repository.
2. Open the project in **Android Studio**.
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

> **Note:**
> - `local.properties` is gitignored and should not be committed.
> - The current app upload flow expects an unsigned Cloudinary upload preset.
> - Cloudinary API secrets should not be shipped inside a production Android app.

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

## Future Improvements

- Migrate old Firebase Storage images to Cloudinary
- Add secure server-side Cloudinary deletion
- Improve search and matching beyond category and keyword shortlist
- Add richer notification workflows for possible matches
