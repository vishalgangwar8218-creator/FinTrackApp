# 📊 FinTrack AI — Smart Expense Tracker & AI Assistant

**FinTrack AI** is an intelligent Android application that simplifies personal finance management. Users can automatically scan receipt images using AWS Textract, store expense data in DynamoDB, and interact with a personalized AI Financial Advisor powered by **Google Gemini AI**.

---

## 📽️ Demo Video

![FinTrack AI Demo](https://github.com/vishalgangwar8218-creator/FinTrackApp/blob/master/demo.mp4)

> 💡 *Replace the link above with your actual GIF or embedded YouTube/Vimeo video link.*

---

## ✨ Features

* 📜 **Automated Receipt Scanning:** Upload receipts/bills to automatically extract the vendor name, date, and total amount via **AWS Textract**.
* 🏷️ **Smart Categorization:** Automatically assigns expenses to categories (e.g., *Food & Dining*, *Groceries*, *Transportation*, *Medical*).
* 🤖 **Gemini AI Financial Advisor:** Integrated AI assistant that analyzes your recent spending trends and answers questions in English or Hinglish.
* ☁️ **Cloud-Native Backend:** Built on a serverless architecture using **AWS Lambda**, **Amazon S3**, and **Amazon DynamoDB**.
* 📱 **Modern Android UI:** Clean user interface supporting ViewBinding, MVVM principles, and custom fonts.

---

## 🛠️ Tech Stack & Architecture

### **Mobile Frontend**
* **Language:** Kotlin
* **Architecture:** MVVM Pattern
* **UI Components:** RecyclerView, View Binding, Lottie Animations, Poppins Font
* **Networking:** Retrofit / OkHttp

### **Backend & Cloud Infrastructure**
* **Runtime:** Python 3.11+ (AWS Lambda)
* **Storage:** AWS S3 (Receipt Image Hosting)
* **Database:** AWS DynamoDB (User Expenses & Metadata)
* **OCR Service:** AWS Textract (Expense Analysis)
* **AI Engine:** Google Gemini API (`gemini-1.5-flash`)

---

## ⚙️ System Architecture Workflow

```
[ Android App ]
        │
        ├─► (POST Receipt Image) ──► [ AWS API Gateway ]
        │                                   │
        │                                   ▼
        │                            [ AWS Lambda ]
        │                                │  ├─► Uploads image to AWS S3
        │                                │  ├─► Processes text via AWS Textract
        │                                │  └─► Saves transaction to DynamoDB
        │                                │
        └─► (POST Chat Query) ───► [ AWS Lambda ]
                                            │
                                            ├─► Fetches context from DynamoDB
                                            ├─► Calls Google Gemini API
                                            └─► Returns AI financial summary
```

---

## 🚀 Setup & Installation

### **1. AWS Lambda Setup**
1. Create an AWS Lambda function with **Python 3.11+** runtime.
2. Grant permissions to the Lambda Execution Role for **S3**, **Textract**, and **DynamoDB**.
3. Add Environment Variables under **Lambda -> Configuration -> Environment variables**:
   * `S3_BUCKET_NAME`: `your-s3-bucket-name`
   * `DYNAMODB_TABLE`: `FinTrackExpenses`
   * `GEMINI_API_KEY`: `your_google_gemini_api_key`

### **2. Google Gemini API Key**
1. Get a free API key from [Google AI Studio](https://aistudio.google.com/).
2. Paste the key inside your AWS Lambda environment variable named `GEMINI_API_KEY`.

### **3. Android App Configuration**
1. Clone this repository:
   ```bash
   git clone https://github.com/your-username/FinTrack-AI.git
   ```
2. Open the project in **Android Studio**.
3. Update your API Gateway Base URL in your Network/Retrofit Client class.
4. Build and run the app on an emulator or physical device.

---

## 🧪 Testing the API

To test the chat handler via AWS Lambda Test Events, use the following API Gateway proxy structure:

```json
{
  "path": "/chat",
  "httpMethod": "POST",
  "body": "{\"query\": \"How much did I spend on food this month?\", \"userId\": \"user_123\"}"
}
```

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
