; JalyClient festive setup wizard (Inno Setup 6)
#define AppVer "1.1.0"

[Setup]
AppId={{8F3C2A1E-6B7D-4C1A-9E2F-4A5B6C7D8E9F}
AppName=JalyClient
AppVersion={#AppVer}
AppVerName=JalyClient {#AppVer}
AppPublisher=DarkerDawnFR
AppComments=The jolliest free Minecraft client
DefaultDirName={localappdata}\Programs\JalyClient
DefaultGroupName=JalyClient
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
OutputDir=..\dist
OutputBaseFilename=JalyClient-Setup
SetupIconFile=icon.ico
UninstallDisplayIcon={app}\JalyClient.exe
UninstallDisplayName=JalyClient
WizardStyle=modern
WizardImageFile=wizard.bmp
WizardSmallImageFile=wizard_small.bmp
Compression=lzma2/max
SolidCompression=yes
CloseApplications=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Messages]
WelcomeLabel1=Welcome to JalyClient!
WelcomeLabel2=The jolliest free Minecraft client.%n%nSnowy animated menus, festive HUD mods, free hats and capes, Christmas resource packs, Sky Lyrics for Spotify and 25 daily gifts.%n%nEverything is free, forever. Click Next to unwrap it!
FinishedHeadingLabel=Ho ho ho! JalyClient is ready
FinishedLabel=JalyClient is installed. Open it, press the big PLAY button and it sets everything up for you.%n%nMerry Christmas!

[Tasks]
Name: "desktopicon"; Description: "Put a JalyClient shortcut on my desktop"; GroupDescription: "Shortcuts:"

[Files]
Source: "..\dist\JalyClient.exe"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{autoprograms}\JalyClient"; Filename: "{app}\JalyClient.exe"; Comment: "The jolliest free Minecraft client"
Name: "{autodesktop}\JalyClient"; Filename: "{app}\JalyClient.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\JalyClient.exe"; Description: "Open JalyClient now"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{app}"
