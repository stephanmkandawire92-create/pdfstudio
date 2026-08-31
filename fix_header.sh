#!/bin/bash
tail -n +5 app/src/main/java/com/example/ui/HomeScreen.kt > temp_hs.kt
echo "package com.example.ui" > new_hs.kt
echo "" >> new_hs.kt
echo "import com.example.engine.SyncState" >> new_hs.kt
echo "import android.content.Context" >> new_hs.kt
echo "import android.content.Intent" >> new_hs.kt
cat temp_hs.kt >> new_hs.kt
mv new_hs.kt app/src/main/java/com/example/ui/HomeScreen.kt
rm temp_hs.kt
