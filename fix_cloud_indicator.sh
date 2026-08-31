#!/bin/bash
# Remove CloudStatusIndicator from HomeScreen.kt
sed -i '/@Composable/,/}$/d' app/src/main/java/com/example/ui/HomeScreen.kt
