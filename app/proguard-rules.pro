# Add project specific ProGuard/R8 rules here.
#
# PDFBox Android includes optional JPEG2000 support through the Gemalto JP2
# classes. Those classes are not required by the Android PDFBox runtime, but
# PDFBox's JPXFilter references them. R8 can otherwise report them as missing
# classes during a minified release build.
-dontwarn com.gemalto.jp2.**

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
