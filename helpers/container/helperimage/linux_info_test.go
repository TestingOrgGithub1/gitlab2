package helperimage

import (
	"fmt"
	"runtime"
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_linuxInfo_create(t *testing.T) {
	for _, shell := range []string{"sh", "bash"} {
		tests := map[string]struct {
			shell          string
			dockerArch     string
			revision       string
			gitlabRegistry bool
			expectedInfo   Info
		}{
			"When dockerArch not specified we fallback to runtime arch": {
				shell:      shell,
				dockerArch: "",
				revision:   "2923a43",
				expectedInfo: Info{
					Architecture:            getExpectedArch(),
					Name:                    DockerHubName,
					Tag:                     fmt.Sprintf("%s-2923a43", getExpectedArch()),
					IsSupportingLocalImport: true,
					Cmd:                     bashCmd,
				},
			},
			"Docker runs on armv6l": {
				shell:      shell,
				dockerArch: "armv6l",
				revision:   "2923a43",
				expectedInfo: Info{
					Architecture:            "arm",
					Name:                    DockerHubName,
					Tag:                     "arm-2923a43",
					IsSupportingLocalImport: true,
					Cmd:                     bashCmd,
				},
			},
			"Docker runs on amd64": {
				shell:      shell,
				dockerArch: "amd64",
				revision:   "2923a43",
				expectedInfo: Info{
					Architecture:            "x86_64",
					Name:                    DockerHubName,
					Tag:                     "x86_64-2923a43",
					IsSupportingLocalImport: true,
					Cmd:                     bashCmd,
				},
			},
			"Docker runs on arm64": {
				shell:      shell,
				dockerArch: "aarch64",
				revision:   "2923a43",
				expectedInfo: Info{
					Architecture:            "arm64",
					Name:                    DockerHubName,
					Tag:                     "arm64-2923a43",
					IsSupportingLocalImport: true,
					Cmd:                     bashCmd,
				},
			},
			"Docker runs on s390x": {
				shell:      shell,
				dockerArch: "s390x",
				revision:   "2923a43",
				expectedInfo: Info{
					Architecture:            "s390x",
					Name:                    DockerHubName,
					Tag:                     "s390x-2923a43",
					IsSupportingLocalImport: true,
					Cmd:                     bashCmd,
				},
			},
			"Configured architecture is unknown": {
				shell:      shell,
				dockerArch: "some-random-arch",
				revision:   "2923a43",
				expectedInfo: Info{
					Architecture:            "some-random-arch",
					Name:                    DockerHubName,
					Tag:                     "some-random-arch-2923a43",
					IsSupportingLocalImport: true,
					Cmd:                     bashCmd,
				},
			},
			"GitLab registry configured": {
				dockerArch:     "amd64",
				revision:       "2923a43",
				gitlabRegistry: true,
				expectedInfo: Info{
					Architecture:            "x86_64",
					Name:                    GitLabRegistryName,
					Tag:                     "x86_64-2923a43",
					IsSupportingLocalImport: true,
					Cmd:                     bashCmd,
				},
			},
		}

		t.Run(shell, func(t *testing.T) {
			for name, test := range tests {
				t.Run(name, func(t *testing.T) {
					l := new(linuxInfo)

					image, err := l.Create(
						test.revision,
						Config{
							Architecture:   test.dockerArch,
							Shell:          shell,
							GitLabRegistry: test.gitlabRegistry,
						},
					)

					assert.NoError(t, err)
					assert.Equal(t, test.expectedInfo, image)
				})
			}
		})
	}
}

// We re write amd64 to x86_64 for the helper image, and we don't want this test
// to be runtime dependant.
func getExpectedArch() string {
	if runtime.GOARCH == "amd64" {
		return "x86_64"
	}

	return runtime.GOARCH
}
