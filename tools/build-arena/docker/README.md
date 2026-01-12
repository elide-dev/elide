# Build Arena Docker Images

Multi-platform Docker images for running AI-driven build comparisons.

## Base Images

Both images use **Eclipse Temurin 17 JDK** (Jammy/Ubuntu 22.04):
- Official OpenJDK distribution
- Full Java development toolchain included
- Multi-platform support (amd64, arm64)
- Maintained by the Eclipse Adoptium project

**Why Temurin?**
- ✅ Pre-built Java 17 JDK (no manual installation)
- ✅ Optimized for production workloads
- ✅ Regular security updates
- ✅ Works on both Intel and ARM architectures
- ✅ Smaller base image than installing OpenJDK on Ubuntu

## Images

### elide-builder:latest

Contains:
- Java 17 (Eclipse Temurin JDK)
- Node.js 20 (for Claude Code)
- Claude Code CLI
- Elide runtime
- Development tools (git, curl, wget, vim, jq, python3)

Used for: Building projects with Elide toolchain

### standard-builder:latest

Contains:
- Java 17 (Eclipse Temurin JDK)
- Node.js 20 (for Claude Code)
- Claude Code CLI
- Maven (latest from apt)
- Gradle 8.5
- Development tools (git, curl, wget, vim, jq, python3)

Used for: Building projects with standard Maven/Gradle

## Building Images

### Quick Start

```bash
# Build both images (detects your platform automatically)
./build-images.sh
```

### Platform-Specific Builds

The script automatically handles platform detection:

**On Mac (ARM64)**:
- Builds for `linux/amd64` (production servers)
- Uses Docker buildx with QEMU emulation
- Images will be slightly slower locally but production-ready

**On Linux (AMD64)**:
- Builds natively for `linux/amd64`
- No emulation needed
- Fastest build and runtime

### Manual Multi-Platform Build

```bash
# Build for specific platform
docker buildx build \
  --platform linux/amd64 \
  --tag elide-builder:latest \
  --file elide-builder.Dockerfile \
  --load \
  .

# Build for multiple platforms (push to registry)
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --tag yourregistry/elide-builder:latest \
  --file elide-builder.Dockerfile \
  --push \
  .
```

## Multi-Platform Support

### Why Build for linux/amd64 on Mac?

Even though you're developing on Mac (ARM64/M1/M2/M3), we build for `linux/amd64` because:

1. **Production servers** typically run AMD64/x86_64
2. **VPS providers** (DigitalOcean, Hetzner, Linode) use AMD64
3. **CI/CD runners** are usually AMD64
4. **Cost**: AMD64 VPS instances are cheaper

### How Does It Work?

Docker Desktop on Mac includes:
- **Docker buildx**: Multi-platform build tool
- **QEMU**: CPU emulator for cross-platform builds
- **binfmt_misc**: Kernel support for foreign binaries

This allows building AMD64 images on ARM64 Mac seamlessly!

### Performance Notes

**Local Development (Mac)**:
- Images built for AMD64 run via emulation
- ~10-20% slower than native ARM64
- Acceptable for development/testing

**Production (Linux AMD64)**:
- Native execution, no emulation
- Full performance

## Dockerfile Structure

### Layer Optimization

Both Dockerfiles are structured to maximize layer caching:

```dockerfile
# Layer 1: Base image (rarely changes)
FROM eclipse-temurin:17-jdk-jammy

# Layer 2: System packages (changes occasionally)
RUN apt-get update && apt-get install -y ...

# Layer 3: Node.js (changes rarely)
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash ...

# Layer 4: Claude Code (changes rarely)
RUN npm install -g @anthropic-ai/claude-code

# Layer 5: Elide/Gradle (changes occasionally)
RUN curl -fsSL https://elide.dev/install.sh | bash

# Layer 6: Verification (always runs)
RUN elide --version && java -version
```

### Build Cache

To rebuild from scratch (clear cache):
```bash
docker buildx build --no-cache ...
```

## Testing Images

### Verify Java Installation

```bash
docker run --rm elide-builder:latest java -version
# Expected: openjdk version "17.x.x"
```

### Verify Elide Installation

```bash
docker run --rm elide-builder:latest elide --version
# Expected: Elide version x.x.x
```

### Verify Gradle Installation

```bash
docker run --rm standard-builder:latest gradle --version
# Expected: Gradle 8.5
```

### Interactive Shell

```bash
# Open shell in elide-builder
docker run -it --rm elide-builder:latest /bin/bash

# Test commands
java -version
node --version
claude --version
elide --version
```

## Troubleshooting

### Docker Buildx Not Found

**On Mac**:
```bash
# Install Docker Desktop (includes buildx)
# Or install manually:
brew install docker-buildx
```

**On Linux**:
```bash
# Install buildx plugin
mkdir -p ~/.docker/cli-plugins
curl -L https://github.com/docker/buildx/releases/download/v0.12.0/buildx-v0.12.0.linux-amd64 \
  -o ~/.docker/cli-plugins/docker-buildx
chmod +x ~/.docker/cli-plugins/docker-buildx
```

### Build Fails on Mac

**Issue**: "exec format error" or platform mismatch

**Solution**: Ensure buildx and QEMU are installed:
```bash
# Check buildx
docker buildx version

# Check QEMU
docker run --rm --privileged multiarch/qemu-user-static --reset -p yes
```

### Slow Build on Mac

**Issue**: Building for linux/amd64 on ARM64 Mac is slow

**Workarounds**:
1. Use layer caching (subsequent builds are faster)
2. Build images on Linux CI/CD instead
3. Use pre-built images from registry
4. Accept the slower build time (only happens once)

### Image Too Large

Current image sizes:
- `elide-builder`: ~1.5-2GB
- `standard-builder`: ~1.5-2GB

**To reduce size**:
1. Use multi-stage builds (future)
2. Remove dev tools in production
3. Use Alpine base (requires more work)

## CI/CD Integration

### GitHub Actions

```yaml
- name: Set up Docker Buildx
  uses: docker/setup-buildx-action@v3

- name: Build images
  run: |
    cd docker
    ./build-images.sh
```

### Building for Production

```bash
# Build and push to registry
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --tag ghcr.io/yourorg/elide-builder:latest \
  --file elide-builder.Dockerfile \
  --push \
  .
```

## Best Practices

1. **Pin versions**: Use specific versions for production
   ```dockerfile
   FROM eclipse-temurin:17.0.9_9-jdk-jammy
   ```

2. **Security updates**: Rebuild images monthly
   ```bash
   docker buildx build --no-cache --pull ...
   ```

3. **Test before deploy**: Always test images locally first
   ```bash
   docker run -it --rm elide-builder:latest /bin/bash
   ```

4. **Tag properly**: Use semantic versioning
   ```bash
   docker tag elide-builder:latest elide-builder:1.0.0
   ```

## Resources

- [Eclipse Temurin](https://adoptium.net/)
- [Docker Buildx](https://docs.docker.com/buildx/working-with-buildx/)
- [Multi-platform images](https://docs.docker.com/build/building/multi-platform/)
- [Elide Documentation](https://elide.dev/docs)
