# kotlinx.serialization: @Serializable 클래스 유지
-keepclassmembers @kotlinx.serialization.Serializable class com.flightfocus.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
