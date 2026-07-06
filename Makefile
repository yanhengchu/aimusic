# -------------------------------------
# make
# macos vim ~/.zshrc
# alias gs='make gs'
# windows vim $PROFILE
# windows notepad $PROFILE
# function gs { mk gs }
# -------------------------------------
PACKAGE_NAME := com.android.bbkmusic

version:
	make -version

# -------------------------------------
# adb
# -------------------------------------

dvs:
	adb devices
size:
	adb shell wm size
top:
	adb shell dumpsys activity top | findstr ACTIVITY || ver > nul
ps:
	adb shell ps | findstr $(PACKAGE_NAME) || ver > nul
stop:
	adb shell am force-stop $(PACKAGE_NAME)
clear:
	adb shell pm clear $(PACKAGE_NAME)

layoutOn:
	adb shell setprop debug.layout true
	adb shell service call activity 1599295570
layoutOff:
	adb shell setprop debug.layout false
	adb shell service call activity 1599295570
touchOn:
	adb shell settings put system show_touches 1
	adb shell settings put system pointer_location 1
touchOff:
	adb shell settings put system show_touches 0
	adb shell settings put system pointer_location 0
profileOn:
	adb shell setprop debug.hwui.profile visual_bars
	adb shell stop
	adb shell start
profileOff:
	adb shell setprop debug.hwui.profile false
	adb shell stop
	adb shell start

# -------------------------------------
# android
# -------------------------------------

jk:
	-taskkill -f -im java.exe
clean: jk
	.\gradlew clean
debug:
	.\gradlew :app:assembleDemesticAndroid_35Debug
release:
	.\gradlew :app:assembleDemesticAndroid_35Release
install:
	.\gradlew installDebug
start:
	adb shell am start -n $(PACKAGE_NAME)/$(PACKAGE_NAME).MusicMainActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
run: clean debug install start

dp:
	$(eval URL := $(or $(url),$(word 2,$(MAKECMDGOALS))))
	adb shell am start -a android.intent.action.VIEW -d '$(subst &,\&,$(URL))'
log:
	powershell -Command "$$appPid = (adb shell pidof $(PACKAGE_NAME)).Trim(); adb logcat -v time --pid=$$appPid"

# -------------------------------------
# windows nssm
# -------------------------------------

nssm:
	powershell -Command "Get-CimInstance Win32_Service | Where-Object { $$_.PathName -match 'nssm.exe' } | Select Name, State, StartMode | Format-Table -AutoSize"

SERVICE_NAME := AutoLogPipeline
atq:
	sc.exe query $(SERVICE_NAME)
atd:
	nssm dump $(SERVICE_NAME)
ato:
	nssm start $(SERVICE_NAME)
atf:
	nssm stop $(SERVICE_NAME)

# -------------------------------------
# git
# -------------------------------------

CURRENT_BRANCH := $(shell git rev-parse --abbrev-ref HEAD)

gf:
	@echo $(CURRENT_BRANCH)
gs:
	git log -1 --oneline
	git status
ga:
	git add .
gam: ga
	git commit --amend --no-edit
gnc:
	git reset HEAD~1

greset:
	git checkout --orphan temp
	git add -A
	git commit -m "init reset"
	git branch -D main
	git branch -m main
	git push -f origin main

gpd:
	git diff HEAD > change.patch
gpf:
	git format-patch -1 HEAD
gpc:
	git apply --check change.patch
gpa:
	git apply change.patch
gpam:
	git am change.patch