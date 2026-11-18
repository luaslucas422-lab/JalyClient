package main

import (
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"errors"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"time"
)

// A Profile is one Minecraft version with its own folder: its own mods, packs, worlds and settings.
type Profile struct {
	ID      string `json:"id"`
	Name    string `json:"name"`
	Version string `json:"version"`
	Color   string `json:"color"`
	Created int64  `json:"created"`
}

type ProfileStore struct {
	Active   string    `json:"active"`
	Profiles []Profile `json:"profiles"`
}

// GameVersions Fabric supports that the launcher offers, newest first.
var GameVersions = []string{"1.21.11", "1.21.10", "1.21.9", "1.21.8", "1.21.7", "1.21.6", "1.21.5", "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21",
	"1.20.6", "1.20.4", "1.20.2", "1.20.1", "1.19.4", "1.19.2", "1.18.2", "1.17.1", "1.16.5"}

// JalyVersions are the versions the JalyClient mod itself runs on. Other profiles get Fabric + their own mods.
var JalyVersions = map[string]bool{"1.21": true, "1.21.1": true}

var ProfileColors = []string{"#E0384F", "#C99E02", "#3FB950", "#4CC3FF", "#B35CFF", "#FF8A3D"}

const DefaultProfile = "default"

func defaultProfile() Profile {
	return Profile{ID: DefaultProfile, Name: "JalyClient", Version: MCVersion, Color: "#E0384F"}
}

func profileFile(base Paths) string { return filepath.Join(base.GameDir, "profiles.json") }

func loadProfiles(base Paths) ProfileStore {
	st := ProfileStore{}
	if b, err := os.ReadFile(profileFile(base)); err == nil {
		_ = json.Unmarshal(b, &st)
	}
	hasDefault := false
	for _, p := range st.Profiles {
		if p.ID == DefaultProfile {
			hasDefault = true
		}
	}
	if !hasDefault {
		st.Profiles = append([]Profile{defaultProfile()}, st.Profiles...)
	}
	if st.find(st.Active) == nil {
		st.Active = DefaultProfile
	}
	return st
}

func saveProfiles(base Paths, st ProfileStore) error {
	if err := os.MkdirAll(base.GameDir, 0o755); err != nil {
		return err
	}
	b, _ := json.MarshalIndent(st, "", "  ")
	return os.WriteFile(profileFile(base), b, 0o644)
}

func (st *ProfileStore) find(id string) *Profile {
	for i := range st.Profiles {
		if st.Profiles[i].ID == id {
			return &st.Profiles[i]
		}
	}
	return nil
}

func (st ProfileStore) active() Profile {
	if p := st.find(st.Active); p != nil {
		return *p
	}
	return defaultProfile()
}

// profilePaths gives each profile its own game folder and its own Minecraft Launcher entry.
func profilePaths(base Paths, pr Profile) Paths {
	p := base
	p.Version = pr.Version
	p.Name = pr.Name
	if pr.ID == DefaultProfile {
		p.Key = ProfileKey
		return p
	}
	p.GameDir = filepath.Join(base.GameDir, "profiles", pr.ID)
	p.Key = ProfileKey + "-" + pr.ID
	return p
}

func validVersion(v string) bool {
	for _, g := range GameVersions {
		if g == v {
			return true
		}
	}
	return false
}

var nonSlug = regexp.MustCompile(`[^a-z0-9]+`)

func cleanName(name string) (string, error) {
	name = strings.TrimSpace(name)
	if name == "" {
		return "", errors.New("give your profile a name")
	}
	if len([]rune(name)) > 32 {
		name = string([]rune(name)[:32])
	}
	return name, nil
}

// CreateProfile adds a profile and makes it the active one.
func CreateProfile(base Paths, name, version, color string) (Profile, error) {
	name, err := cleanName(name)
	if err != nil {
		return Profile{}, err
	}
	if !validVersion(version) {
		return Profile{}, errors.New("pick a Minecraft version")
	}
	st := loadProfiles(base)
	if len(st.Profiles) >= 30 {
		return Profile{}, errors.New("that's a lot of profiles! Delete one first")
	}
	rb := make([]byte, 3)
	_, _ = rand.Read(rb)
	slug := strings.Trim(nonSlug.ReplaceAllString(strings.ToLower(name), "-"), "-")
	if slug == "" {
		slug = "profile"
	}
	if len(slug) > 20 {
		slug = slug[:20]
	}
	if color == "" {
		color = ProfileColors[len(st.Profiles)%len(ProfileColors)]
	}
	pr := Profile{ID: slug + "-" + hex.EncodeToString(rb), Name: name, Version: version, Color: color, Created: time.Now().Unix()}
	st.Profiles = append(st.Profiles, pr)
	st.Active = pr.ID
	if err := os.MkdirAll(profilePaths(base, pr).GameDir, 0o755); err != nil {
		return Profile{}, err
	}
	return pr, saveProfiles(base, st)
}

// EditProfile renames a profile or moves it to another Minecraft version.
func EditProfile(base Paths, id, name, version, color string) error {
	name, err := cleanName(name)
	if err != nil {
		return err
	}
	if !validVersion(version) {
		return errors.New("pick a Minecraft version")
	}
	st := loadProfiles(base)
	pr := st.find(id)
	if pr == nil {
		return errors.New("profile not found")
	}
	if id == DefaultProfile && !JalyVersions[version] {
		return errors.New("the main JalyClient profile has to stay on a JalyClient version (1.21 or 1.21.1). Make a new profile for other versions")
	}
	pr.Name, pr.Version = name, version
	if color != "" {
		pr.Color = color
	}
	return saveProfiles(base, st)
}

func SelectProfile(base Paths, id string) error {
	st := loadProfiles(base)
	if st.find(id) == nil {
		return errors.New("profile not found")
	}
	st.Active = id
	return saveProfiles(base, st)
}

// DeleteProfile removes a profile and its Minecraft Launcher entry. Its folder (worlds!) is moved to a trash folder, not deleted.
func DeleteProfile(base Paths, id string) error {
	if id == DefaultProfile {
		return errors.New("the main JalyClient profile can't be deleted")
	}
	st := loadProfiles(base)
	pr := st.find(id)
	if pr == nil {
		return errors.New("profile not found")
	}
	pp := profilePaths(base, *pr)
	_ = RemoveProfile(pp)
	if _, err := os.Stat(pp.GameDir); err == nil {
		trash := filepath.Join(base.GameDir, "deleted-profiles")
		_ = os.MkdirAll(trash, 0o755)
		_ = os.Rename(pp.GameDir, filepath.Join(trash, id+"-"+time.Now().Format("20060102-150405")))
	}
	out := st.Profiles[:0]
	for _, p := range st.Profiles {
		if p.ID != id {
			out = append(out, p)
		}
	}
	st.Profiles = out
	if st.Active == id {
		st.Active = DefaultProfile
	}
	return saveProfiles(base, st)
}

// ProfileView is what the Profiles tab shows.
type ProfileView struct {
	Profile
	Jaly      bool   `json:"jaly"`
	Installed bool   `json:"installed"`
	Mods      int    `json:"mods"`
	Dir       string `json:"dir"`
}

func profileViews(base Paths) []ProfileView {
	st := loadProfiles(base)
	var out []ProfileView
	for _, pr := range st.Profiles {
		pp := profilePaths(base, pr)
		_, err := os.Stat(filepath.Join(pp.GameDir, "jaly-installed.json"))
		n := 0
		if es, e := os.ReadDir(filepath.Join(pp.GameDir, "mods")); e == nil {
			for _, f := range es {
				if strings.HasSuffix(f.Name(), ".jar") {
					n++
				}
			}
		}
		out = append(out, ProfileView{Profile: pr, Jaly: JalyVersions[pr.Version], Installed: err == nil, Mods: n, Dir: pp.GameDir})
	}
	return out
}
