@echo off
setlocal

echo ===================================================
echo   Staging, Committing, and Pushing to GitHub...
echo ===================================================
git add .
git commit -m "Add OCR reader, book theme with dark/light switcher, auth, top tabs, and animations"
git push origin main
echo ===================================================
pause
