# ✈️ Flight Booking App - Android

A comprehensive flight reservation and management system built with Java and Firebase. This application provides a seamless experience for users to search for flights, manage bookings, and generate digital tickets, while offering an extensive administrative dashboard for fleet and destination management.

## 📖 Project Overview
The Flight Booking App is designed to simplify the air travel process. It handles everything from user onboarding with OTP verification to real-time flight tracking and electronic boarding passes. The project leverages Firebase for real-time data consistency and Cloudinary for efficient image handling.

## ✨ Key Features

### 👤 User Module
- **Advanced Auth**: Email/Password login with Google Sign-In support.
- **OTP Verification**: Multi-step identity verification via custom JavaMail SMTP integration.
- **Flight Discovery**: Search flights by origin, destination, date, and class.
- **Booking Flow**: Intuitive seat selection and passenger details management.
- **Payment Simulation**: Secure payment fragment with validation and transaction feedback.
- **Digital Tickets**: Generate PDFs for boarding passes and booking summaries.
- **Profile Management**: Customizable user profiles with Cloudinary-backed image uploads.

### 🛡️ Admin Module
- **Flight Management**: Create, update, and cancel flight schedules in real-time.
- **Destination Control**: Manage global destinations with imagery and descriptions.
- **Booking Oversight**: Monitor and manage all user bookings and cancellation requests.
- **User Analytics**: View and manage user accounts and roles.

## 🛠 Technologies Used
- **Language**: Java (JDK 17)
- **Backend**: Firebase (Auth, Firestore, Cloud Functions)
- **Storage**: Cloudinary (Image Hosting)
- **Email**: JavaMail API (SMTP)
- **Networking**: OkHttp, Glide
- **Utils**: ZXing (QR Codes), PDFJet (Ticket Generation)
- **Architecture**: Fragment-based single-activity architecture for fluid navigation.

## 📂 Project Structure
```text
flight-booking/
├── app/
│   ├── src/main/java/com/example/flightbooking/
│   │   ├── adapters/      # RecyclerView adapters for flights, bookings, etc.
│   │   ├── models/        # Data models (User, Flight, Booking, Destination)
│   │   ├── util/          # Helper classes (EmailUtils, CloudinaryUploader)
│   │   ├── ui/            # Fragments and Activities
│   │   └── ...            # Logic for Search, Booking, Admin, and Payment
│   └── src/main/res/      # Layouts, Drawables, and Navigation XMLs
├── functions/             # Firebase Cloud Functions (Node.js)
└── local.properties       # Secure environment variables (Hidden from Git)
```

## ⚙️ Configuration & Setup

### 1. Prerequisites
- Android Studio Iguana or newer.
- A Firebase Project.
- A Cloudinary account.

### 2. Environment Setup
Add your secrets to `local.properties` (this file is ignored by Git for security):
```properties
GMAIL_USER="your-email@gmail.com"
GMAIL_PASS="your-app-password"
CLOUDINARY_URL="your-cloudinary-url"
```

### 3. Firebase Integration
1. Download `google-services.json` from the Firebase Console.
2. Place it in the `app/` directory.
3. Enable **Firestore** and **Authentication** in the console.

## 🚀 Usage Guide
1. **Register**: Sign up and verify your account using the OTP sent to your email.
2. **Search**: Use the home fragment to find flights.
3. **Book**: Select a flight, fill in passenger details, and complete the simulated payment.
4. **Ticket**: View your booking in "My Bookings" and download the PDF boarding pass.

## 🔮 Future Improvements
- [ ] Integration with real Payment Gateways (Stripe/Razorpay).
- [ ] Real-time flight status notifications via Push Notifications.
- [ ] Multi-language support (Localization).
- [ ] Dark Mode UI optimization.

## 👥 Author
**Abel Adam**
- GitHub: [@abel-adam](https://github.com/abel-adam)

## 📄 License
This project is licensed under the MIT License - see the LICENSE file for details.
