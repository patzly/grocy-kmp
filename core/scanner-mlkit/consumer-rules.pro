# ML Kit creates its component registrars by reflection from the manifest. Firebase's own rule
# keeps only the classes, and R8's full mode then removes their constructors, so every ML Kit
# client (barcode and text) finds no component and crashes with a NullPointerException.
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
}
