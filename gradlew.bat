@if "%DEBUG%" == "" @echo off
set DIRNAME=%~dp0
set WRAPPER_JAR="%DIRNAME%gradle\wrapper\gradle-wrapper.jar"
java -jar %WRAPPER_JAR% %*
