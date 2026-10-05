# Compose 相关：保留 Compose Runtime 的反射调用点
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# Room 生成的实现类
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# Kotlin 元数据（Hilt、Room 反射需要）
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
