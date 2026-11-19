package main

import (
	"archive/zip"
	"encoding/json"
	"io"
	"os"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
)

// modInfo is the bit of fabric.mod.json we need to know if a mod fits a profile.
type modInfo struct {
	ID       string         `json:"id"`
	Version  string         `json:"version"`
	Depends  map[string]any `json:"depends"`
	Breaks   map[string]any `json:"breaks"`
	Provides []string       `json:"provides"`
}

func readModInfo(jar string) (*modInfo, error) {
	z, err := zip.OpenReader(jar)
	if err != nil {
		return nil, err
	}
	defer z.Close()
	for _, f := range z.File {
		if f.Name != "fabric.mod.json" {
			continue
		}
		rc, err := f.Open()
		if err != nil {
			return nil, err
		}
		b, _ := io.ReadAll(rc)
		rc.Close()
		var m modInfo
		// some mods have comments/trailing junk; be lenient
		if err := json.Unmarshal(b, &m); err != nil {
			return nil, err
		}
		return &m, nil
	}
	return nil, os.ErrNotExist
}

// ---------------------------------------------------------------- version predicates (Fabric style)

var numRe = regexp.MustCompile(`^\d+`)

// cmpVersion compares dotted versions; a pre-release ("-rc1") sorts before the release.
func cmpVersion(a, b string) int {
	a, b = strings.TrimSpace(a), strings.TrimSpace(b)
	ap, apre := splitPre(a)
	bp, bpre := splitPre(b)
	as, bs := strings.Split(ap, "."), strings.Split(bp, ".")
	for i := 0; i < len(as) || i < len(bs); i++ {
		x, y := 0, 0
		if i < len(as) {
			x, _ = strconv.Atoi(numRe.FindString(as[i]))
		}
		if i < len(bs) {
			y, _ = strconv.Atoi(numRe.FindString(bs[i]))
		}
		if x != y {
			if x < y {
				return -1
			}
			return 1
		}
	}
	switch {
	case apre == bpre:
		return 0
	case apre == "" && bpre != "":
		return 1
	case apre != "" && bpre == "":
		return -1
	case apre < bpre:
		return -1
	}
	return 1
}

func splitPre(v string) (string, string) {
	v = strings.SplitN(v, "+", 2)[0]
	if i := strings.Index(v, "-"); i >= 0 {
		return v[:i], v[i+1:]
	}
	return v, ""
}

// matchOne checks a single predicate like ">=1.21", "~1.21", "1.21.x", "1.21", "*".
func matchOne(pred, v string) bool {
	pred = strings.TrimSpace(pred)
	if pred == "" || pred == "*" {
		return true
	}
	for _, op := range []string{">=", "<=", ">", "<", "=", "~", "^"} {
		if strings.HasPrefix(pred, op) {
			want := strings.TrimSpace(pred[len(op):])
			want = strings.TrimSuffix(want, "-") // "1.21-" means 1.21 incl. pre-releases
			c := cmpVersion(v, want)
			switch op {
			case ">=":
				return c >= 0
			case "<=":
				return c <= 0
			case ">":
				return c > 0
			case "<":
				return c < 0
			case "=":
				return c == 0
			case "~": // same major.minor
				p := strings.Split(want, ".")
				if len(p) >= 2 {
					minor, _ := strconv.Atoi(numRe.FindString(p[1]))
					return c >= 0 && cmpVersion(v, p[0]+"."+strconv.Itoa(minor+1)) < 0
				}
				return c >= 0
			case "^": // same major
				return c >= 0 && strings.Split(v, ".")[0] == strings.Split(want, ".")[0]
			}
		}
	}
	if strings.ContainsAny(pred, "xX*") { // wildcard 1.21.x
		pp := strings.Split(pred, ".")
		vv := strings.Split(v, ".")
		for i, p := range pp {
			if p == "x" || p == "X" || p == "*" {
				return true
			}
			if i >= len(vv) || numRe.FindString(vv[i]) != numRe.FindString(p) {
				return false
			}
		}
		return true
	}
	return cmpVersion(v, strings.TrimSuffix(pred, "-")) == 0
}

// matches handles "a b" (all must match) and a list (any must match).
func matches(constraint any, v string) bool {
	switch c := constraint.(type) {
	case string:
		for _, part := range strings.Fields(c) {
			if !matchOne(part, v) {
				return false
			}
		}
		return true
	case []any:
		if len(c) == 0 {
			return true
		}
		for _, e := range c {
			if s, ok := e.(string); ok && matches(s, v) {
				return true
			}
		}
		return false
	}
	return true
}

// ModFits says whether a mod jar can run on Minecraft `mc`.
func ModFits(jar, mc string) bool {
	m, err := readModInfo(jar)
	if err != nil || m.Depends == nil {
		return true // not a Fabric mod we can read: leave it alone
	}
	if c, ok := m.Depends["minecraft"]; ok {
		return matches(c, mc)
	}
	return true
}

// SweepMods takes out mods that can't run on this profile's Minecraft version, or that clash with
// another installed mod, so the game doesn't stop with "Incompatible mods found". They go to
// mods-removed/ (nothing is lost). Returns what was taken out and why.
func SweepMods(gameDir, mc string) map[string]string {
	removed := map[string]string{}
	mods := filepath.Join(gameDir, "mods")
	out := filepath.Join(gameDir, "mods-removed")
	move := func(name, why string) {
		_ = os.MkdirAll(out, 0o755)
		if err := os.Rename(filepath.Join(mods, name), filepath.Join(out, name)); err == nil {
			removed[name] = why
		}
	}
	for pass := 0; pass < 4; pass++ {
		entries, _ := os.ReadDir(mods)
		infos := map[string]*modInfo{}
		present := map[string]string{}
		for _, e := range entries {
			if e.IsDir() || !strings.HasSuffix(strings.ToLower(e.Name()), ".jar") {
				continue
			}
			m, err := readModInfo(filepath.Join(mods, e.Name()))
			if err != nil {
				continue
			}
			infos[e.Name()] = m
			present[m.ID] = m.Version
			for _, p := range m.Provides {
				present[p] = m.Version
			}
		}
		changed := false
		for name, m := range infos {
			if c, ok := m.Depends["minecraft"]; ok && !matches(c, mc) {
				move(name, "not made for Minecraft "+mc)
				changed = true
				continue
			}
			for other, c := range m.Breaks {
				if v, ok := present[other]; ok && other != m.ID && matches(c, v) {
					move(name, "doesn't work with "+other+" "+v)
					changed = true
					break
				}
			}
		}
		if !changed {
			// a mod that needs a newer version of another installed mod (e.g. Fabric API) can't load either
			for name, m := range infos {
				for dep, c := range m.Depends {
					if dep == "minecraft" || dep == "java" || dep == "fabricloader" || dep == "fabric" {
						continue
					}
					if v, ok := present[dep]; ok && !matches(c, v) {
						move(name, "needs "+dep+" "+asText(c))
						changed = true
						break
					}
				}
			}
		}
		if !changed {
			break
		}
	}
	return removed
}

func asText(c any) string {
	switch v := c.(type) {
	case string:
		return v
	case []any:
		var s []string
		for _, e := range v {
			if t, ok := e.(string); ok {
				s = append(s, t)
			}
		}
		return strings.Join(s, " or ")
	}
	return ""
}
