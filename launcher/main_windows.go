//go:build windows

package main

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"sync"

	webview2 "github.com/jchv/go-webview2"
)

func main() {
	if len(os.Args) >= 3 && os.Args[1] == "--selftest" {
		os.Exit(selfTest(os.Args[2]))
	}
	p := basePaths()
	_ = os.MkdirAll(p.GameDir, 0o755)
	w := webview2.NewWithOptions(webview2.WebViewOptions{
		Debug:     false,
		AutoFocus: true,
		DataPath:  filepath.Join(p.GameDir, "webview"),
		WindowOptions: webview2.WindowOptions{
			Title:  "JalyClient",
			Width:  1120,
			Height: 700,
			IconId: 1,
			Center: true,
		},
	})
	if w == nil {
		showError("JalyClient needs Microsoft Edge WebView2. Install it from microsoft.com/edge/webview2 and try again.")
		return
	}
	defer w.Destroy()
	w.SetSize(1000, 640, webview2.HintMin)

	var busy sync.Mutex
	call := func(js string) { w.Dispatch(func() { w.Eval(js) }) }

	_ = w.Bind("jalyState", func() State { return currentState() })
	_ = w.Bind("jalySave", func(raw string) error {
		var s Settings
		if err := json.Unmarshal([]byte(raw), &s); err != nil {
			return err
		}
		if err := saveSettings(paths(), s); err != nil {
			return err
		}
		base := basePaths()
		bs := loadSettings(base)
		bs.Music = s.Music
		if paths().GameDir == base.GameDir {
			return nil
		}
		return saveSettings(base, bs)
	})
	_ = w.Bind("jalyCreateProfile", func(name, version, color string) (Profile, error) {
		return CreateProfile(basePaths(), name, version, color)
	})
	_ = w.Bind("jalyEditProfile", func(id, name, version, color string) error { return EditProfile(basePaths(), id, name, version, color) })
	_ = w.Bind("jalySelectProfile", func(id string) error { return SelectProfile(basePaths(), id) })
	_ = w.Bind("jalyDeleteProfile", func(id string) error { return DeleteProfile(basePaths(), id) })
	_ = w.Bind("jalyOpenProfile", func(id string) {
		base := basePaths()
		st := loadProfiles(base)
		if pr := st.find(id); pr != nil {
			OpenFolder(profilePaths(base, *pr).GameDir)
		}
	})
	_ = w.Bind("jalyPlay", func(launch bool) {
		go func() {
			if !busy.TryLock() {
				return
			}
			defer busy.Unlock()
			p := paths()
			s := loadSettings(p)
			rep, err := Install(p, s, func(pct float64, msg string) {
				call(fmt.Sprintf("window.onProgress(%f, %s)", pct, jsonString(msg)))
			})
			if err != nil {
				call(fmt.Sprintf("window.onDone(false, %s, null)", jsonString(err.Error())))
				return
			}
			msg := "JalyClient is ready! Press PLAY in the Minecraft Launcher."
			if p.key() != ProfileKey {
				msg = "\"" + p.Name + "\" (" + p.mc() + ") is ready! Press PLAY in the Minecraft Launcher."
			}
			if launch {
				if err := OpenMinecraftLauncher(); err != nil {
					msg = "Installed! Open the Minecraft Launcher and pick the JalyClient profile."
				}
			}
			call(fmt.Sprintf("window.onDone(true, %s, %s)", jsonString(msg), jsonString(rep.Skipped)))
		}()
	})
	_ = w.Bind("jalyOpen", func(which string) {
		p := paths()
		switch which {
		case "mods":
			OpenFolder(filepath.Join(p.GameDir, "mods"))
		case "screenshots":
			OpenFolder(filepath.Join(p.GameDir, "screenshots"))
		case "resourcepacks":
			OpenFolder(filepath.Join(p.GameDir, "resourcepacks"))
		default:
			OpenFolder(p.GameDir)
		}
	})
	_ = w.Bind("jalyRemoveProfile", func() error { return RemoveProfile(paths()) })
	_ = w.Bind("jalyCheckUpdate", func() Release { return CheckUpdate() })
	_ = w.Bind("jalyApplyUpdate", func(r Release) error {
		if err := ApplyUpdate(r); err != nil {
			return err
		}
		go func() { w.Dispatch(func() { w.Terminate() }) }()
		return nil
	})
	_ = w.Bind("jalyUser", func() UserData { return loadUser(paths()) })
	_ = w.Bind("jalyInstall", func(kind, id, slug, title string) {
		go func() {
			err := InstallContent(paths(), kind, id, slug, title)
			msg := ""
			if err != nil {
				msg = err.Error()
			}
			call(fmt.Sprintf("window.onInstalled(%s, %s)", jsonString(id), jsonString(msg)))
		}()
	})
	_ = w.Bind("jalyRemove", func(kind, id string) error { return RemoveContent(paths(), kind, id) })
	if len(os.Args) >= 2 && os.Args[1] == "--play" {
		w.Init("window.autoPlay = true;")
	}
	w.SetHtml(buildHTML())
	w.Run()
}
