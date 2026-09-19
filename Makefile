.PHONY: aboutlibraries aboutlibraries_android apk apk_debug assets icon

build: assets apk

# The core ships as a prebuilt libbox AAR (composeApp/libs/libbox-1.14.0-lx.35.aar
# from the sing-box-lx release page) — no Go build step is required.

apk:
	./gradlew androidApp:assembleFossRelease

apk_debug:
	./gradlew androidApp:assembleFossDebug

assets:
	./run lib assets

icon:
	./run icon

aboutlibraries: aboutlibraries_android

aboutlibraries_android:
	./gradlew :composeApp:exportLibraryDefinitions
