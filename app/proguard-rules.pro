# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Native callbacks are invoked from litehtml through JNI.
-keep class com.juhao.murexide.ui.components.litehtml.LiteHtmlView { *; }

# LiquidGlassSelectionContainer identifies Compose's pre-materialized text magnifier by class.
-keep class androidx.compose.ui.ComposedModifier { *; }

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
