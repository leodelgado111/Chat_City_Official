# Splash Screen Fix Instructions

## Issue
The file "Splash Screen.png" in the drawable folder has an invalid name for Android resources.

## Solution
You need to rename the file from "Splash Screen.png" to "splash_screen.png" (all lowercase, underscore instead of space).

## How to Fix

### Option 1: Using PowerShell (Recommended)
Run this command in PowerShell from your project directory:

```powershell
cd C:\src\Chat_City_Official
Rename-Item -Path "app\src\main\res\drawable\Splash Screen.png" -NewName "splash_screen.png"
```

### Option 2: Manual Rename
1. Navigate to: `C:\src\Chat_City_Official\app\src\main\res\drawable\`
2. Find the file: `Splash Screen.png`
3. Right-click and rename to: `splash_screen.png`

### Option 3: Using Git to rename
```bash
cd C:\src\Chat_City_Official
git mv "app/src/main/res/drawable/Splash Screen.png" "app/src/main/res/drawable/splash_screen.png"
git commit -m "Rename splash screen file to valid Android resource name"
git push
```

## After Renaming
1. Pull the latest changes from GitHub
2. Clean and rebuild:
   ```
   .\gradlew clean assembleDebug
   .\gradlew installDebug
   ```

## What Changed
- **Removed duplication**: The splash_background.xml now only shows the gradient, not the logo
- **Single splash image**: The layout now displays your provided splash screen image only once
- **Simplified animation**: Clean fade-in effect for the splash image
- **No text duplication**: Removed all the "Chat City" text elements that were causing triplication
