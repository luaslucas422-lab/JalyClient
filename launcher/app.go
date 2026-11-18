package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"os"
	"strings"
	"time"
)

// State is everything the UI needs when it starts.
type State struct {
	Version   string                `json:"version"`
	MC        string                `json:"mc"`
	DaysLeft  int                   `json:"daysLeft"`
	Settings  Settings              `json:"settings"`
	Catalog   map[string][]ExtraMod `json:"catalog"`
	MCFound   bool                  `json:"mcFound"`
	Installed bool                  `json:"installed"`
	GameDir   string                `json:"gameDir"`
}

func daysUntilChristmas(now time.Time) int {
	y := now.Year()
	x := time.Date(y, 12, 25, 0, 0, 0, 0, now.Location())
	today := time.Date(y, now.Month(), now.Day(), 0, 0, 0, 0, now.Location())
	if today.After(x) {
		x = time.Date(y+1, 12, 25, 0, 0, 0, 0, now.Location())
	}
	return int(x.Sub(today).Hours()/24 + 0.5)
}

func currentState() State {
	p := paths()
	_, mcErr := os.Stat(p.MCDir)
	_, jalyErr := os.Stat(p.GameDir + string(os.PathSeparator) + "jaly-installed.json")
	return State{
		Version:   AppVersion,
		MC:        MCVersion,
		DaysLeft:  daysUntilChristmas(time.Now()),
		Settings:  loadSettings(p),
		Catalog:   catalogByCategory(),
		MCFound:   mcErr == nil,
		Installed: jalyErr == nil,
		GameDir:   p.GameDir,
	}
}

// buildHTML inlines every asset as a data URI so the window needs no web server.
func buildHTML() string {
	html, _ := uiFiles.ReadFile("ui/index.html")
	out := string(html)
	entries, _ := uiFiles.ReadDir("ui")
	for _, e := range entries {
		name := e.Name()
		mime := ""
		switch {
		case strings.HasSuffix(name, ".png"):
			mime = "image/png"
		case strings.HasSuffix(name, ".ogg"):
			mime = "audio/ogg"
		default:
			continue
		}
		b, _ := uiFiles.ReadFile("ui/" + name)
		out = strings.ReplaceAll(out, "{{"+name+"}}", "data:"+mime+";base64,"+base64.StdEncoding.EncodeToString(b))
	}
	return out
}

func jsonString(v any) string {
	b, _ := json.Marshal(v)
	return string(b)
}

// selfTest installs into a scratch APPDATA without any window; used by the build server.
func selfTest(dir string) int {
	_ = os.Setenv("APPDATA", dir)
	p := paths()
	_ = os.MkdirAll(p.MCDir, 0o755)
	_ = os.WriteFile(p.MCDir+string(os.PathSeparator)+"launcher_profiles.json", []byte(`{"profiles":{"other":{"name":"Keep me","type":"custom"}},"settings":{"keepThis":true},"version":3}`), 0o644)
	s := loadSettings(p)
	for _, m := range Catalog {
		s.Mods[m.Slug] = true // test every mod in the catalog
	}
	_ = saveSettings(p, s)
	rep, err := Install(p, s, func(pct float64, msg string) { fmt.Printf("[%3.0f%%] %s\n", pct*100, msg) })
	fmt.Println("REPORT " + jsonString(rep))
	if err != nil {
		fmt.Println("FAIL install: " + err.Error())
		return 1
	}
	b, _ := os.ReadFile(p.MCDir + string(os.PathSeparator) + "launcher_profiles.json")
	var root map[string]any
	_ = json.Unmarshal(b, &root)
	profiles, _ := root["profiles"].(map[string]any)
	ok := profiles["other"] != nil && profiles[ProfileKey] != nil && root["settings"] != nil
	fmt.Printf("CHECK profile kept other entries: %v\n", ok)
	if !ok {
		return 1
	}
	if rep.Installed["fabric-api"] == "" || rep.JalyJar == "" {
		fmt.Println("FAIL fabric api or JalyClient missing")
		return 1
	}
	// switching a mod off must remove it
	s.Mods["jade"] = false
	if _, err := Install(p, s, func(float64, string) {}); err != nil {
		fmt.Println("FAIL reinstall: " + err.Error())
		return 1
	}
	if _, err := os.Stat(p.GameDir + "/mods/" + rep.Installed["jade"]); rep.Installed["jade"] != "" && err == nil {
		fmt.Println("FAIL jade was not removed")
		return 1
	}
	// Mods tab: install a mod and the most popular resource pack from Modrinth, then remove them
	if err := InstallContent(p, "mod", "xaeros-minimap", "xaeros-minimap", "Xaero's Minimap"); err != nil {
		fmt.Println("FAIL mod install: " + err.Error())
		return 1
	}
	var sr struct {
		Hits []struct {
			ID string `json:"project_id"`
		} `json:"hits"`
	}
	_ = getJSON(`https://api.modrinth.com/v2/search?limit=1&index=downloads&facets=[["project_type:resourcepack"],["versions:1.21.1"]]`, &sr)
	if len(sr.Hits) == 0 {
		fmt.Println("FAIL resource pack search")
		return 1
	}
	if err := InstallContent(p, "resourcepack", sr.Hits[0].ID, sr.Hits[0].ID, "pack"); err != nil {
		fmt.Println("FAIL pack install: " + err.Error())
		return 1
	}
	u := loadUser(p)
	ob, _ := os.ReadFile(p.GameDir + string(os.PathSeparator) + "options.txt")
	fmt.Printf("CHECK Mods tab installs: mods=%d packs=%d packEnabled=%v\n", len(u.Mods), len(u.Resourcepacks), strings.Contains(string(ob), "file/"))
	if len(u.Mods) == 0 || len(u.Resourcepacks) != 1 || !strings.Contains(string(ob), "file/") {
		fmt.Println("FAIL Mods tab install")
		return 1
	}
	_ = RemoveContent(p, "resourcepack", sr.Hits[0].ID)
	if len(loadUser(p).Resourcepacks) != 0 {
		fmt.Println("FAIL pack remove")
		return 1
	}
	fmt.Printf("CHECK update check: %+v\n", CheckUpdate())
	if err := RemoveProfile(p); err != nil {
		fmt.Println("FAIL remove profile: " + err.Error())
		return 1
	}
	fmt.Println("PASS launcher self-test")
	return 0
}
