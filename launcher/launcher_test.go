package main

import (
	"encoding/json"
	"os"
	"path/filepath"
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
