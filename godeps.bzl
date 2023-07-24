load("@gazelle//:deps.bzl", "go_repository")

def go_dependencies():
    go_repository(
        name = "com_github_evanw_esbuild",
        importpath = "github.com/evanw/esbuild",
        sum = "h1:O1kmBW8O6lTJel3n1J8Hyp0WKulVE8vIvygF8AQQxhY=",
        version = "v0.17.4",
    )
    go_repository(
        name = "org_golang_x_sys",
        importpath = "golang.org/x/sys",
        sum = "h1:w8ZOecv6NaNa/zC8944JTU3vz4u6Lagfk4RPQxv92NQ=",
        version = "v0.3.0",
    )
