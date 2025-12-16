<img width="500" height="500" alt="DropZone Logo" src="https://github.com/user-attachments/assets/8383e75a-d594-4e86-b5b5-b3dd7cf184a6" />

# 📦 DropZone

**DropZone** is an Android application designed to simplify the process of reporting and recovering lost items.  
It allows users to post **lost or found belongings** with details and images, helping the community efficiently reconnect owners with their items.

A key highlight of DropZone is its **AI-powered matching system**, which intelligently suggests relevant found items for lost posts using semantic similarity.

---

## ✨ Features

### 1️⃣ Create and Manage Posts
Users can create posts for **lost or found items**, including a title, detailed description, location, and optional image.  
Users can also **edit or delete their own posts**.

<img src="https://github.com/user-attachments/assets/72dd1ecb-d97e-469b-95f8-af68bdab8314" alt="Home Page" width="300" height="600">

---

### 2️⃣ Filtered Feed
The main feed displays all posts and allows users to **filter by status** (Lost or Found), making browsing faster and more efficient.

---

### 3️⃣ Post Details Page
Each post has a dedicated detail screen showing:
- Item description
- Location
- Status
- Posted time
- Uploaded image (or a placeholder if no image is provided)

<img src="https://github.com/user-attachments/assets/85232110-a02c-445c-96b2-e69e32e40190" alt="Post Details Page" width="300" height="600">

---

### 4️⃣ AI-Powered Suggested Matches 🤖
For **lost posts created by the owner**, DropZone uses an **AI semantic matching backend** to suggest relevant **found posts**, even when descriptions differ in wording.

- Powered by **FastAPI + Sentence Transformers**
- Uses **cosine similarity** to rank matches
- Clicking a suggestion opens the matched found post directly

This reduces manual searching and significantly improves recovery chances.

---

### 5️⃣ Direct Contact
Users can directly **contact the post owner via email** to arrange item recovery.

---

### 6️⃣ Profile Page
A dedicated profile screen allows users to:
- View all posts they have created
- Manage their activity in one place

<img src="https://github.com/user-attachments/assets/7a5ce29d-d97e-43cb-b806-ba3934299d30" alt="Profile Page" width="300" height="600">

---

### 7️⃣ Edit Profile
Users can update personal details such as:
- Name
- Phone number

---

### 8️⃣ Push Notifications (FCM)
DropZone integrates **Firebase Cloud Messaging (FCM)** to send push notifications for important updates.

<img src="https://github.com/user-attachments/assets/a572e0c3-46ff-47f9-aac3-6dcbd772542a" alt="Notification" width="300" height="600">

---

### 9️⃣ Animated Splash Screen
A smooth **animated logo splash screen** enhances the first-time user experience using modern Android APIs.

---

### 🔟 Donation Feature 💖
DropZone includes a **donation feature** that allows users to contribute via UPI-enabled apps such as:
- PhonePe
- Paytm
- Google Pay  

This is implemented using **Android Intents** to launch the selected payment app directly.

---

### 🌙 Dark Mode Support
The app supports **Dark Mode**, providing a better experience in low-light environments.

---

## 🛠️ Tech Stack

### Android
- Kotlin
- XML Layouts
- RecyclerView
- AndroidX Libraries
- Glide
- CircleImageView
- Splash Screen API
- AnimatedVectorDrawable

### Firebase
- Firebase Authentication
- Firebase Cloud Firestore
- Firebase Cloud Storage
- Firebase Cloud Messaging (FCM)

### AI Backend
- FastAPI (Python)
- Sentence Transformers (`all-MiniLM-L6-v2`)
- Cosine Similarity (scikit-learn)

---

## 🎥 Demo Video
👉 [Watch Demo](https://github.com/user-attachments/assets/2d6655a4-eb0c-4276-bcd6-c565edceb8df)


---

## 🚀 Future Scope
DropZone is designed with scalability in mind.

Planned improvements include:
- Expanding support beyond IIIT Lucknow
- Location-based matching
- In-app chat between users
- Enhanced AI filtering using categories and images

---

## 🏁 Conclusion
DropZone combines **community-driven reporting** with **AI-powered intelligence** to make lost & found recovery faster, smarter, and more reliable.

Built for real-world impact.
