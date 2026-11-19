package main

import (
	"archive/zip"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/url"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
)

// UserEntry / UserData mirror the mod's jaly-user-content.json so the game and launcher share one list.
type UserEntry struct {
	ID    string `json:"id"`
	Slug  string `json:"slug"`
	Title string `json:"title"`
	File  string `json:"file"`
}

type UserData struct {
	Mods            map[string]UserEntry `json:"mods"`
	Resourcepacks   map[string]UserEntry `json:"resourcepacks"`
	Shaderpacks     map[string]UserEntry `json:"shaderpacks"`
	PendingRemovals []string             `json:"pendingRemovals"`
	PendingInstalls []string             `json:"pendingInstalls"`
}

var kinds = map[string]struct{ loader, folder string }{
	"mod":          {"fabric", "mods"},
	"resourcepack": {"minecraft", "resourcepacks"},
	"shader":       {"iris", "shaderpacks"},
}

func userFile(p Paths) string { return filepath.Join(p.GameDir, "jaly-user-content.json") }

func loadUser(p Paths) UserData {
	d := UserData{}
	if b, err := os.ReadFile(userFile(p)); err == nil {
		_ = json.Unmarshal(b, &d)
	}
	if d.Mods == nil {
		d.Mods = map[string]UserEntry{}
	}
	if d.Resourcepacks == nil {
		d.Resourcepacks = map[string]UserEntry{}
	}
	if d.Shaderpacks == nil {
		d.Shaderpacks = map[string]UserEntry{}
	}
	return d
}

func saveUser(p Paths, d UserData) error {
	_ = os.MkdirAll(p.GameDir, 0o755)
	b, _ := json.MarshalIndent(d, "", "  ")
	return os.WriteFile(userFile(p), b, 0o644)
}

func (d *UserData) bucket(kind string) map[string]UserEntry {
	switch kind {
	case "resourcepack":
		return d.Resourcepacks
	case "shader":
		return d.Shaderpacks
	}
	return d.Mods
}

// FinishPending deletes mods the player removed in game (Windows kept them locked until the game closed).
func FinishPending(p Paths) {
	d := loadUser(p)
	if len(d.PendingRemovals) == 0 && len(d.PendingInstalls) == 0 {
		return
	}
	for _, rel := range d.PendingRemovals {
		_ = os.Remove(filepath.Join(p.GameDir, filepath.FromSlash(rel)))
		for _, kind := range []string{"mod", "resourcepack", "shader"} {
			b := d.bucket(kind)
			for id, e := range b {
				if kinds[kind].folder+"/"+e.File == rel {
					delete(b, id)
				}
			}
		}
	}
	d.PendingRemovals = nil
	d.PendingInstalls = nil
	_ = saveUser(p, d)
}

func jarModID(path string) string {
	z, err := zip.OpenReader(path)
	if err != nil {
		return ""
	}
	defer z.Close()
	for _, f := range z.File {
		if f.Name == "fabric.mod.json" {
			r, err := f.Open()
			if err != nil {
				return ""
			}
			b, _ := io.ReadAll(r)
			r.Close()
			var m struct {
				ID string `json:"id"`
			}
			_ = json.Unmarshal(b, &m)
			return m.ID
		}
	}
	return ""
}

func modIDsOnDisk(dir string) map[string]bool {
	ids := map[string]bool{"fabric-api": true}
	entries, _ := os.ReadDir(dir)
	for _, e := range entries {
		if strings.HasSuffix(e.Name(), ".jar") {
			if id := jarModID(filepath.Join(dir, e.Name())); id != "" {
				ids[id] = true
			}
		}
	}
	return ids
}

func versionFor(kind, project, mc string) (*mrVersion, error) {
	k := kinds[kind]
	q := url.Values{}
	q.Set("loaders", `["`+k.loader+`"]`)
	base := "https://api.modrinth.com/v2/project/" + url.PathEscape(project) + "/version?"
	var vs []mrVersion
	q2 := url.Values{}
	q2.Set("loaders", q.Get("loaders"))
	q2.Set("game_versions", `["`+mc+`"]`)
	if err := getJSON(base+q2.Encode(), &vs); err != nil {
		return nil, err
	}
	if len(vs) == 0 && kind == "shader" {
		_ = getJSON(base+q.Encode(), &vs)
	}
	if len(vs) == 0 {
		return nil, fmt.Errorf("no version for Minecraft %s", mc)
	}
	return &vs[0], nil
}

// InstallContent installs a Modrinth project for the player (with required mod dependencies).
func InstallContent(p Paths, kind, projectID, slug, title string) error {
	k, ok := kinds[kind]
	if !ok {
		return errors.New("unknown kind")
	}
	dir := filepath.Join(p.GameDir, k.folder)
	_ = os.MkdirAll(dir, 0o755)
	d := loadUser(p)
	seen := map[string]bool{FabricAPI: true}
	var walk func(id, slug, title string) error
	walk = func(id, slug, title string) error {
		if seen[id] {
			return nil
		}
		seen[id] = true
		v, err := versionFor(kind, id, p.mc())
		if err != nil {
			return err
		}
		f := v.Files[0]
		for _, ff := range v.Files {
			if ff.Primary {
				f = ff
			}
		}
		dest := filepath.Join(dir, f.Filename)
		if kind == "mod" {
			if _, err := os.Stat(dest); err == nil {
				return nil
			}
			tmp := dest + ".check"
			if err := download(f.URL, tmp); err != nil {
				return err
			}
			if mid := jarModID(tmp); mid != "" && modIDsOnDisk(dir)[mid] {
				_ = os.Remove(tmp)
				return nil
			}
			if err := os.Rename(tmp, dest); err != nil {
				return err
			}
			for _, dep := range v.Dependencies {
				if dep.Type == "required" && dep.ProjectID != "" {
					if err := walk(dep.ProjectID, dep.ProjectID, dep.ProjectID); err != nil {
						return err
					}
				}
			}
		} else if err := download(f.URL, dest); err != nil {
			return err
		}
		d.bucket(kind)[id] = UserEntry{ID: id, Slug: slug, Title: title, File: f.Filename}
		return nil
	}
	if err := walk(projectID, slug, title); err != nil {
		return err
	}
	if err := saveUser(p, d); err != nil {
		return err
	}
	e := d.bucket(kind)[projectID]
	switch kind {
	case "resourcepack":
		setOptionPack(p, "file/"+e.File, true)
	case "shader":
		setIrisPack(p, e.File)
	}
	return nil
}

// RemoveContent removes something the player installed (the game is closed, so files can go right away).
func RemoveContent(p Paths, kind, projectID string) error {
	d := loadUser(p)
	b := d.bucket(kind)
	e, ok := b[projectID]
	if !ok {
		return nil
	}
	if kind == "resourcepack" {
		setOptionPack(p, "file/"+e.File, false)
	}
	_ = os.Remove(filepath.Join(p.GameDir, kinds[kind].folder, e.File))
	delete(b, projectID)
	return saveUser(p, d)
}

var packLine = regexp.MustCompile(`^resourcePacks:(.*)$`)

// setOptionPack switches a resource pack on/off in options.txt.
func setOptionPack(p Paths, id string, on bool) {
	file := filepath.Join(p.GameDir, "options.txt")
	b, _ := os.ReadFile(file)
	lines := strings.Split(string(b), "\n")
	found := false
	for i, l := range lines {
		m := packLine.FindStringSubmatch(strings.TrimRight(l, "\r"))
		if m == nil {
			continue
		}
		found = true
		var packs []string
		_ = json.Unmarshal([]byte(m[1]), &packs)
		out := []string{}
		for _, x := range packs {
			if x != id {
				out = append(out, x)
			}
		}
		if on {
			out = append(out, id)
		}
		j, _ := json.Marshal(out)
		lines[i] = "resourcePacks:" + string(j)
	}
	if !found && on {
		j, _ := json.Marshal([]string{"vanilla", "fabric", id})
		lines = append(lines, "resourcePacks:"+string(j))
	}
	_ = os.WriteFile(file, []byte(strings.Join(lines, "\n")), 0o644)
}

func setIrisPack(p Paths, pack string) {
	dir := filepath.Join(p.GameDir, "config")
	_ = os.MkdirAll(dir, 0o755)
	file := filepath.Join(dir, "iris.properties")
	b, _ := os.ReadFile(file)
	out := []string{}
	for _, l := range strings.Split(string(b), "\n") {
		t := strings.TrimSpace(l)
		if t == "" || strings.HasPrefix(t, "shaderPack=") || strings.HasPrefix(t, "enableShaders=") {
			continue
		}
		out = append(out, t)
	}
	out = append(out, "shaderPack="+pack, "enableShaders=true")
	_ = os.WriteFile(file, []byte(strings.Join(out, "\n")+"\n"), 0o644)
}

// ---------------------------------------------------------------- self-update

type Release struct {
	Version string `json:"version"`
	URL     string `json:"url"`
}

func newerVersion(a, b string) bool {
	pa, pb := strings.Split(strings.TrimPrefix(a, "v"), "."), strings.Split(strings.TrimPrefix(b, "v"), ".")
	for i := 0; i < 3; i++ {
		var x, y int
		if i < len(pa) {
			x, _ = strconv.Atoi(strings.SplitN(pa[i], "+", 2)[0])
		}
		if i < len(pb) {
			y, _ = strconv.Atoi(strings.SplitN(pb[i], "+", 2)[0])
		}
		if x != y {
			return x > y
		}
	}
	return false
}

// BuildCommit is the git commit this launcher was built from (set by CI).
var BuildCommit = ""

// BetaTag is the GitHub release that always holds the newest Beta Update.
const BetaTag = "beta"

// CheckBeta asks GitHub for the Beta Update. It's newer whenever it was built from a different commit.
func CheckBeta() Release {
	var r struct {
		Target string `json:"target_commitish"`
		Assets []struct {
			Name string `json:"name"`
			URL  string `json:"browser_download_url"`
		} `json:"assets"`
	}
	if err := getJSON("https://api.github.com/repos/luaslucas422-lab/JalyClient/releases/tags/"+BetaTag, &r); err != nil {
		return Release{}
	}
	if r.Target == "" || r.Target == BuildCommit {
		return Release{}
	}
	for _, a := range r.Assets {
		if a.Name == "JalyClient-Setup.exe" {
			short := r.Target
			if len(short) > 7 {
				short = short[:7]
			}
			return Release{Version: "Beta (" + short + ")", URL: a.URL}
		}
	}
	return Release{}
}

// CheckUpdate asks GitHub for the newest release (or the Beta Update when beta testing). Empty Version means up to date.
func CheckUpdate(beta bool) Release {
	if beta {
		if r := CheckBeta(); r.Version != "" {
			return r
		}
	}
	var r struct {
		Tag    string `json:"tag_name"`
		Assets []struct {
			Name string `json:"name"`
			URL  string `json:"browser_download_url"`
		} `json:"assets"`
	}
	if err := getJSON("https://api.github.com/repos/luaslucas422-lab/JalyClient/releases/latest", &r); err != nil {
		return Release{}
	}
	if !newerVersion(r.Tag, AppVersion) {
		return Release{}
	}
	for _, a := range r.Assets {
		if a.Name == "JalyClient-Setup.exe" {
			return Release{Version: strings.TrimPrefix(r.Tag, "v"), URL: a.URL}
		}
	}
	return Release{}
}

// ApplyUpdate downloads the new setup and runs it silently; the setup reopens JalyClient when done.
func ApplyUpdate(rel Release) error {
	dest := filepath.Join(os.TempDir(), "JalyClient-Setup-"+strings.NewReplacer(" ", "-", "(", "", ")", "").Replace(rel.Version)+".exe")
	if err := download(rel.URL, dest); err != nil {
		return err
	}
	return exec.Command(dest, "/VERYSILENT", "/SUPPRESSMSGBOXES", "/NORESTART").Start()
}
