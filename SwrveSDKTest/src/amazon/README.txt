To create the STUBBED amazon jar, run following commands after compiling this project in Android Studio:
cp -R ../../build/intermediates/classes/amazon/debug/com/amazon/ com/
jar cvf amazon-device-messaging-STUBBED.jar com/
rm -rf com/

