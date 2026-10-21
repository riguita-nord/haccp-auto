@echo off
cd /d %~dp0
if not exist out mkdir out
if not exist data mkdir data
javac -encoding UTF-8 -d out src\haccp\Main.java || exit /b 1
java -cp out haccp.Main
