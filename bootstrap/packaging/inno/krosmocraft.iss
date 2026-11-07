; Windows installer (Inno Setup 6), same look as the old launcher's installer.
; Built by `gradlew :bootstrap:innoInstaller` around the jpackage app image (AppDir), output in OutDir.
; New AppId: installs next to the old launcher, it does not replace it.

#ifndef AppVersion
  #define AppVersion "1.0.0"
#endif
#ifndef AppDir
  #define AppDir "..\..\build\jpackage\Krosmocraft"
#endif
#ifndef OutDir
  #define OutDir "..\..\build\inno"
#endif

[Setup]
AppId={{D0C40D39-07D9-4C5B-9F9E-836B026B7D37}
AppName=Krosmocraft
AppVersion={#AppVersion}
AppVerName=Krosmocraft
AppPublisher=SaintPepsi
AppPublisherURL=https://krosmocraft.fr/
AppSupportURL=https://krosmocraft.fr/
AppUpdatesURL=https://krosmocraft.fr/
VersionInfoVersion={#AppVersion}
VersionInfoProductVersion={#AppVersion}
VersionInfoCopyright=SaintPepsi
DefaultDirName={localappdata}\Programs\Krosmocraft
DefaultGroupName=Krosmocraft
PrivilegesRequired=lowest
OutputDir={#OutDir}
OutputBaseFilename=Krosmocraft-Setup
SetupIconFile=icon.ico
WizardStyle=classic
WizardImageFile=home_cover.bmp
WizardSmallImageFile=icon.bmp
DisableWelcomePage=no
DisableProgramGroupPage=yes
DisableReadyPage=yes
SolidCompression=yes
Compression=lzma2/max
UninstallDisplayIcon={app}\Krosmocraft.exe
UninstallDisplayName=Krosmocraft

[Languages]
Name: "french"; MessagesFile: "compiler:Languages\French.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "{#AppDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\Krosmocraft"; Filename: "{app}\Krosmocraft.exe"
Name: "{group}\{cm:UninstallProgram,Krosmocraft}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\Krosmocraft"; Filename: "{app}\Krosmocraft.exe"; Tasks: desktopicon

[UninstallDelete]
Type: filesandordirs; Name: "{app}"

[Run]
Filename: "{app}\Krosmocraft.exe"; Description: "{cm:LaunchProgram,Krosmocraft}"; Flags: nowait postinstall skipifsilent
