@echo off
set /p version=Entrez la note de version : 
git add .
git commit -m "%version%"
git remote add origin https://github.com/Paulem79/Launcher.git
git push -u origin master
