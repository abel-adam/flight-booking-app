# Flight Booking Android App ✈️

A modern Android application for searching, booking, and managing flights. This project features a robust authentication system, real-time database integration via Firebase, and a custom SMTP-based OTP verification system.

## 🚀 Features

- **User Authentication**: Secure Login and Registration using Firebase Auth.
- **OTP Verification**: Custom 6-digit verification codes sent via JavaMail API (Gmail SMTP) for new accounts and password resets.
- **Flight Search**: Search for available flights based on destination and preferences.
- **Booking Management**: Users can view and manage their flight bookings.
- **Admin Dashboard**: Specialized interface for managing flight listings and user bookings.
- **Profile Management**: Update user details and profile pictures (integrated with Cloudinary).
- **Secure Password Reset**: Multi-step verification using custom OTP followed by official Firebase secure reset links.

## 🛠 Tech Stack

- **Language**: Java
- **UI Framework**: XML (Material Design)
- **Backend**: Firebase (Authentication, Firestore, Cloud Functions)
- **Email Service**: JavaMail API via Gmail SMTP
- **Image Handling**: Glide, CircleImageView
- **Image Storage**: Cloudinary (Unsigned Presets)
- **Networking**: OkHttp

## 🔑 Key Configurations

### SMTP Configuration (EmailUtils.java)
The app uses a custom `EmailUtils` class to send OTPs. To enable this, ensure you have a Gmail App Password:
```java
final String username = "your-email@gmail.com";
final String password = "your-16-character-app-password";
```

### Firebase Setup
1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/).
2. Enable **Email/Password** authentication.
3. Enable **Cloud Firestore** in test or production mode.
4. (Optional) Configure **SMTP Settings** in Firebase Authentication -> Templates -> Password Reset to use your Gmail for secure links.

## 📸 Screenshots

| Login | Registration | Flight Search |
|-------|--------------|---------------|
| ![Login](https://via.placeholder.com/200x400) | ![Reg](https://via.placeholder.com/200x400) | ![Search](https://via.placeholder.com/200x400) |

## 🏗 Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Flight-Booking.git
   ```
2. Open the project in **Android Studio**.
3. Connect your Firebase project and add the `google-services.json` file to the `/app` directory.
4. Sync Gradle and run the app on an emulator or physical device.

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
