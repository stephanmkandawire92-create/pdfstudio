#!/bin/bash
sed -i '/CloudStatusIndicator(/,/isAutoBackupEnabled = isAutoBackupEnabled/d' app/src/main/java/com/example/ui/HomeScreen.kt
sed -i '/)/!b;//!d' app/src/main/java/com/example/ui/HomeScreen.kt
