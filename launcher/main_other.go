//go:build !windows

package main

import (
	"fmt"
	"os"
)

// On other systems the launcher only offers the install self-test (the window needs Windows WebView2).
func main() {
	if len(os.Args) >= 3 && os.Args[1] == "--selftest" {
		os.Exit(selfTest(os.Args[2]))
	}
	if len(os.Args) >= 2 && os.Args[1] == "--html" {
		fmt.Print(buildHTML())
		return
	}
	fmt.Println("JalyClient launcher " + AppVersion + " runs on Windows. Use --selftest <dir> to test the installer.")
}

func showError(msg string) { fmt.Println(msg) }
