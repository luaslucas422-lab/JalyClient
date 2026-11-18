package main

import (
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

func TestDaysUntilChristmas(t *testing.T) {
	cases := map[string]int{"2026-12-25": 0, "2026-12-24": 1, "2026-11-17": 38, "2026-12-26": 364}
	for d, want := range cases {
		tm, _ := time.Parse("2006-01-02", d)
		if got := daysUntilChristmas(tm); got != want {
			t.Errorf("%s: got %d want %d", d, got, want)
		}
	}
}

func TestSettingsDefaults(t *testing.T) {
	p := Paths{GameDir: t.TempDir()}
	s := loadSettings(p)
	if s.RAM != 4 || !s.Music || !s.Mods["sodium"] || s.Mods["iris"] {
		t.Fatalf("bad defaults: %+v", s)
	}
	s.RAM = 7
	s.Mods["sodium"] = false
	if err := saveSettings(p, s); err != nil {
		t.Fatal(err)
	}
	s2 := loadSettings(p)
	if s2.RAM != 7 || s2.Mods["sodium"] {
		t.Fatalf("settings not saved: %+v", s2)
	}
}

func TestProfileKeepsOtherProfiles(t *testing.T) {
	dir := t.TempDir()
	p := Paths{MCDir: filepath.Join(dir, ".minecraft"), GameDir: filepath.Join(dir, ".jalyclient")}
	_ = os.MkdirAll(p.MCDir, 0o755)
	orig := `{"profiles":{"abc":{"name":"Mine","type":"custom"}},"settings":{"crashAssistance":true},"version":3}`
	_ = os.WriteFile(filepath.Join(p.MCDir, "launcher_profiles.json"), []byte(orig), 0o644)
	if err := writeProfile(p, "fabric-loader-0.18.4-1.21.1", Settings{RAM: 6}); err != nil {
		t.Fatal(err)
	}
	b, _ := os.ReadFile(filepath.Join(p.MCDir, "launcher_profiles.json"))
	var root map[string]any
	if err := json.Unmarshal(b, &root); err != nil {
		t.Fatal(err)
	}
	profiles := root["profiles"].(map[string]any)
	j := profiles[ProfileKey].(map[string]any)
	if profiles["abc"] == nil || root["settings"] == nil {
		t.Fatal("other launcher data was lost")
	}
	if j["lastVersionId"] != "fabric-loader-0.18.4-1.21.1" || j["gameDir"] != p.GameDir || j["javaArgs"].(string)[:6] != "-Xmx6G" {
		t.Fatalf("bad profile: %v", j)
	}
	if err := RemoveProfile(p); err != nil {
		t.Fatal(err)
	}
	b, _ = os.ReadFile(filepath.Join(p.MCDir, "launcher_profiles.json"))
	_ = json.Unmarshal(b, &root)
	if root["profiles"].(map[string]any)[ProfileKey] != nil {
		t.Fatal("profile not removed")
	}
}

func TestInstallNeedsMinecraft(t *testing.T) {
	dir := t.TempDir()
	p := Paths{MCDir: filepath.Join(dir, "missing"), GameDir: filepath.Join(dir, ".jalyclient")}
	if _, err := Install(p, loadSettings(p), func(float64, string) {}); err == nil {
		t.Fatal("expected an error when Minecraft is not installed")
	}
}

func TestHTMLHasEveryAsset(t *testing.T) {
	h := buildHTML()
	for _, name := range []string{"logo.png", "icon.png", "music.ogg", "title_sky.png", "title_near.png", "title_sleigh.png"} {
		if contains(h, "{{"+name+"}}") {
			t.Errorf("%s was not inlined", name)
		}
	}
}

func contains(s, sub string) bool {
	return len(s) >= len(sub) && (func() bool {
		for i := 0; i+len(sub) <= len(s); i++ {
			if s[i:i+len(sub)] == sub {
				return true
			}
		}
		return false
	})()
}

func TestUserContentAndOptions(t *testing.T) {
	dir := t.TempDir()
	p := Paths{GameDir: dir}
	_ = os.MkdirAll(filepath.Join(dir, "mods"), 0o755)
	_ = os.WriteFile(filepath.Join(dir, "mods", "old.jar"), []byte("x"), 0o644)
	d := loadUser(p)
	d.Mods["abc"] = UserEntry{ID: "abc", Slug: "old", Title: "Old", File: "old.jar"}
	d.PendingRemovals = []string{"mods/old.jar"}
	_ = saveUser(p, d)
	FinishPending(p)
	if _, err := os.Stat(filepath.Join(dir, "mods", "old.jar")); err == nil {
		t.Fatal("pending removal not finished")
	}
	if len(loadUser(p).Mods) != 0 {
		t.Fatal("entry not removed")
	}
	_ = os.WriteFile(filepath.Join(dir, "options.txt"), []byte("fov:0.0\nresourcePacks:[\"vanilla\",\"fabric\"]\n"), 0o644)
	setOptionPack(p, "file/cool.zip", true)
	b, _ := os.ReadFile(filepath.Join(dir, "options.txt"))
	if !contains(string(b), `"file/cool.zip"`) || !contains(string(b), "fov:0.0") {
		t.Fatalf("options not updated: %s", b)
	}
	setOptionPack(p, "file/cool.zip", false)
	b, _ = os.ReadFile(filepath.Join(dir, "options.txt"))
	if contains(string(b), "cool.zip") {
		t.Fatal("pack not removed")
	}
	if !newerVersion("v1.2.1", "1.2.0") || newerVersion("1.2.0", "1.2.0") || newerVersion("1.1.9", "1.2.0") {
		t.Fatal("version compare wrong")
	}
}

func TestProfiles(t *testing.T) {
	dir := t.TempDir()
	base := Paths{MCDir: filepath.Join(dir, ".minecraft"), GameDir: filepath.Join(dir, ".jalyclient")}
	st := loadProfiles(base)
	if len(st.Profiles) != 1 || st.Active != DefaultProfile || st.active().Version != MCVersion {
		t.Fatalf("default profile missing: %+v", st)
	}
	if _, err := CreateProfile(base, "", "1.20.1", ""); err == nil {
		t.Fatal("empty name accepted")
	}
	if _, err := CreateProfile(base, "Old PvP", "1.7.10", ""); err == nil {
		t.Fatal("bad version accepted")
	}
	pr, err := CreateProfile(base, "Old PvP!", "1.20.1", "")
	if err != nil {
		t.Fatal(err)
	}
	st = loadProfiles(base)
	if st.Active != pr.ID || len(st.Profiles) != 2 {
		t.Fatalf("new profile not active: %+v", st)
	}
	pp := profilePaths(base, pr)
	if pp.mc() != "1.20.1" || pp.key() != "jalyclient-"+pr.ID || !strings.HasPrefix(pp.GameDir, filepath.Join(base.GameDir, "profiles")) {
		t.Fatalf("bad paths: %+v", pp)
	}
	_ = os.MkdirAll(base.MCDir, 0o755)
	if err := writeProfile(pp, "fabric-loader-x-1.20.1", Settings{RAM: 4}); err != nil {
		t.Fatal(err)
	}
	if err := writeProfile(profilePaths(base, defaultProfile()), "fabric-loader-x-1.21.1", Settings{RAM: 4}); err != nil {
		t.Fatal(err)
	}
	b, _ := os.ReadFile(filepath.Join(base.MCDir, "launcher_profiles.json"))
	var root map[string]any
	_ = json.Unmarshal(b, &root)
	profs := root["profiles"].(map[string]any)
	mine := profs[pp.key()].(map[string]any)
	if mine["name"] != "JalyClient - Old PvP!" || mine["gameDir"] != pp.GameDir || profs[ProfileKey] == nil {
		t.Fatalf("launcher entries wrong: %v", profs)
	}
	if err := EditProfile(base, DefaultProfile, "JalyClient", "1.20.1", ""); err == nil {
		t.Fatal("default profile moved off a JalyClient version")
	}
	if err := EditProfile(base, pr.ID, "Renamed", "1.21.4", ""); err != nil {
		t.Fatal(err)
	}
	if err := SelectProfile(base, DefaultProfile); err != nil || loadProfiles(base).Active != DefaultProfile {
		t.Fatal("select failed")
	}
	if err := DeleteProfile(base, DefaultProfile); err == nil {
		t.Fatal("default profile deleted")
	}
	if err := DeleteProfile(base, pr.ID); err != nil {
		t.Fatal(err)
	}
	b, _ = os.ReadFile(filepath.Join(base.MCDir, "launcher_profiles.json"))
	_ = json.Unmarshal(b, &root)
	if root["profiles"].(map[string]any)[pp.key()] != nil || len(loadProfiles(base).Profiles) != 1 {
		t.Fatal("profile not fully deleted")
	}
	if _, err := os.Stat(pp.GameDir); err == nil {
		t.Fatal("profile folder should move to deleted-profiles")
	}
}
