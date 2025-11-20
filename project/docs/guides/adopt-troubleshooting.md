# Build Adopter Troubleshooting Guide

This guide covers common issues and solutions when using `elide adopt` to convert Maven or Gradle projects to Elide.

## Table of Contents

- [General Issues](#general-issues)
- [Maven-Specific Issues](#maven-specific-issues)
- [Gradle-Specific Issues](#gradle-specific-issues)
- [Generated PKL Issues](#generated-pkl-issues)
- [Getting Help](#getting-help)

## General Issues

### Issue: Command Not Found

**Symptom:**
```bash
$ elide adopt maven
zsh: command not found: elide
```

**Solution:**
1. Ensure Elide CLI is installed and in your PATH
2. Try using the full path to the Elide binary
3. Verify installation:
   ```bash
   which elide
   elide --version
   ```

### Issue: Permission Denied

**Symptom:**
```bash
$ elide adopt maven
Error: Permission denied writing to elide.pkl
```

**Solutions:**
1. Check file permissions in current directory:
   ```bash
   ls -la elide.pkl
   ```

2. Remove write-protected file:
   ```bash
   chmod +w elide.pkl
   ```

3. Or write to a different location:
   ```bash
   elide adopt maven --output /tmp/elide.pkl
   ```

### Issue: Dry Run Shows No Dependencies

**Symptom:**
```bash
$ elide adopt maven --dry-run
📦 Dependencies
  Compile: 0
  Test: 0
```

**Possible Causes:**
1. **All dependencies use `provided` scope**: Elide doesn't include `provided` scope dependencies
2. **Dependencies defined in parent POM only**: Check if parent POM is being resolved
3. **Profile-specific dependencies**: Try activating the profile with `-P`

**Solutions:**
```bash
# Activate profile
elide adopt maven -P development --dry-run

# Check parent POM resolution
elide adopt maven --dry-run 2>&1 | grep "Resolving parent"
```

## Maven-Specific Issues

### Issue: Parent POM Not Found

**Symptom:**
```bash
Error: Failed to resolve parent POM: org.example:parent:1.0.0
  Tried: filesystem, local repository, Maven Central
```

**Causes:**
- Parent POM is in a custom repository
- Parent POM doesn't exist in Maven Central
- Network connectivity issues
- Incorrect parent coordinates

**Solutions:**

1. **Check parent coordinates in pom.xml:**
   ```xml
   <parent>
     <groupId>org.example</groupId>
     <artifactId>parent</artifactId>
     <version>1.0.0</version>
   </parent>
   ```

2. **Verify parent exists in Maven Central:**
   ```bash
   curl -I https://repo.maven.apache.org/maven2/org/example/parent/1.0.0/parent-1.0.0.pom
   ```

3. **Check if parent is in local repo:**
   ```bash
   ls ~/.m2/repository/org/example/parent/1.0.0/
   ```

4. **Install parent locally:**
   ```bash
   cd ../parent-project
   mvn install
   cd -
   elide adopt maven
   ```

5. **Skip parent resolution (may result in unresolved versions):**
   Currently not supported - you may need to manually edit the POM to remove parent reference temporarily.

### Issue: Property Not Resolved

**Symptom:**
```bash
Warning: Could not resolve property: ${my.custom.version}
```

**Causes:**
- Property defined in a profile that wasn't activated
- Property defined in `settings.xml` (not supported)
- Property uses expression evaluation (not supported)

**Solutions:**

1. **Activate the profile containing the property:**
   ```bash
   elide adopt maven -P production
   ```

2. **Check where property is defined:**
   ```bash
   grep -r "my.custom.version" pom.xml */pom.xml
   ```

3. **Manually replace property in pom.xml temporarily:**
   ```xml
   <!-- Before -->
   <version>${my.custom.version}</version>

   <!-- After -->
   <version>1.0.0</version>
   ```

### Issue: BOM Dependencies Not Resolved

**Symptom:**
```
Warning: Failed to download BOM: org.springframework.boot:spring-boot-dependencies:3.2.0
```

**Causes:**
- Network connectivity issues
- BOM doesn't exist in Maven Central
- Incorrect BOM coordinates

**Solutions:**

1. **Check network connectivity:**
   ```bash
   curl -I https://repo.maven.apache.org/maven2/
   ```

2. **Verify BOM exists:**
   ```bash
   curl -I https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/3.2.0/spring-boot-dependencies-3.2.0.pom
   ```

3. **Clear cache and retry:**
   ```bash
   rm -rf ~/.elide/cache/maven
   elide adopt maven
   ```

### Issue: Multi-Module Project Not Detected

**Symptom:**
```
✓ Project parsed successfully
  Modules: 0
```

**Causes:**
- POM has `<modules>` but doesn't have `<packaging>pom</packaging>`
- Module directories don't exist
- Incorrect module paths

**Solutions:**

1. **Check POM packaging:**
   ```xml
   <packaging>pom</packaging>
   <modules>
     <module>module-a</module>
     <module>module-b</module>
   </modules>
   ```

2. **Verify module directories exist:**
   ```bash
   ls -d module-a module-b
   ```

3. **Check module paths in parent POM:**
   ```bash
   grep -A5 "<modules>" pom.xml
   ```

### Issue: Dependency Versions Missing

**Symptom:**
```pkl
packages {
  "org.junit.jupiter:junit-jupiter"  // Missing version!
}
```

**Causes:**
- Dependency version is managed in parent's `<dependencyManagement>`
- Parent POM wasn't resolved successfully
- BOM wasn't downloaded

**Solutions:**

1. **Check parent POM resolution:**
   ```bash
   elide adopt maven --dry-run 2>&1 | grep "parent"
   ```

2. **Verify dependencyManagement in parent:**
   ```bash
   # If parent is local
   grep -A10 "dependencyManagement" ../parent/pom.xml
   ```

3. **Temporarily add explicit version:**
   ```xml
   <dependency>
     <groupId>org.junit.jupiter</groupId>
     <artifactId>junit-jupiter</artifactId>
     <version>5.10.1</version>  <!-- Add explicit version -->
     <scope>test</scope>
   </dependency>
   ```

## Gradle-Specific Issues

### Issue: Build File Not Found

**Symptom:**
```bash
Error: Could not find build file in current directory
  Looked for: build.gradle, build.gradle.kts
```

**Solutions:**

1. **Specify build file explicitly:**
   ```bash
   elide adopt gradle path/to/build.gradle.kts
   ```

2. **Check current directory:**
   ```bash
   ls build.gradle*
   pwd
   ```

### Issue: Dependencies Show as Unresolved

**Symptom:**
```pkl
packages {
  "com.example:library:unspecified"
}
```

**Causes:**
- Dependency uses dynamic version (`+`, `latest.release`)
- Version defined in Gradle version catalog (not yet supported)
- Version defined in `gradle.properties` (not supported)

**Solutions:**

1. **Check for dynamic versions:**
   ```kotlin
   // build.gradle.kts
   dependencies {
     implementation("com.google.guava:guava:+")  // Don't use dynamic versions
   }
   ```

2. **Replace with explicit version:**
   ```kotlin
   dependencies {
     implementation("com.google.guava:guava:32.1.3-jre")
   }
   ```

3. **Check version catalog:**
   ```bash
   cat gradle/libs.versions.toml
   ```

### Issue: Multi-Project Not Detected

**Symptom:**
```
✓ Project parsed successfully
  Modules: 0
```

**Causes:**
- Missing `settings.gradle[.kts]` file
- Incorrect `include()` statements
- Subproject directories don't exist

**Solutions:**

1. **Check for settings file:**
   ```bash
   ls settings.gradle*
   ```

2. **Verify settings.gradle.kts:**
   ```kotlin
   rootProject.name = "my-project"
   include("module-a", "module-b")
   ```

3. **Check subproject directories:**
   ```bash
   ls -d module-a module-b
   ```

### Issue: Kotlin DSL Parsing Errors

**Symptom:**
```bash
Error: Failed to parse build.gradle.kts
```

**Causes:**
- Complex Kotlin DSL syntax
- Custom functions or DSL extensions
- Gradle plugins that modify the DSL

**Solutions:**

1. **Simplify build file temporarily:**
   - Remove custom functions
   - Extract complex logic to separate files
   - Use simple dependency declarations

2. **Convert to Groovy DSL:**
   ```bash
   # Groovy DSL is sometimes easier to parse
   mv build.gradle.kts build.gradle
   # Simplify to Groovy syntax
   ```

3. **File an issue with example:**
   The adopter uses text-based parsing which may not handle all Kotlin DSL features.

### Issue: Plugin Configuration Ignored

**Symptom:**
```pkl
// Build plugins detected (manual conversion may be needed):
//   - org.springframework.boot:3.2.0

// But Spring Boot configuration is missing!
```

**Expected Behavior:**
Plugin *configuration* blocks (like `springBoot {}`) are not converted - only dependencies are extracted.

**Solutions:**

1. **Manually configure in Elide** (if supported)
2. **Keep Gradle file for plugin configuration** during transition
3. **Extract plugin configuration to external files**

### Issue: compileOnly Dependencies Missing

**Symptom:**
```pkl
// No Lombok in generated PKL, but it's in build.gradle.kts
```

**Cause:**
`compileOnly` dependencies are listed in comments, not in the `packages` section.

**Solution:**

Check the generated PKL for comments:
```pkl
// Note: compileOnly dependencies detected:
//   - org.projectlombok:lombok:1.18.30
```

Decide if you need these in Elide - many `compileOnly` dependencies (like Lombok) are annotation processors that may need different handling.

## Generated PKL Issues

### Issue: elide.pkl Already Exists

**Symptom:**
```bash
Error: File already exists: elide.pkl
  Use --output to specify a different file, or delete the existing file
```

**Solutions:**

1. **Preview first with dry-run:**
   ```bash
   elide adopt maven --dry-run
   ```

2. **Write to different file:**
   ```bash
   elide adopt maven --output elide-new.pkl
   diff elide.pkl elide-new.pkl
   ```

3. **Backup and overwrite:**
   ```bash
   mv elide.pkl elide.pkl.backup
   elide adopt maven
   ```

### Issue: Dependencies Appear Twice

**Symptom:**
```pkl
packages {
  "com.google.guava:guava:32.1.3-jre"
  "com.google.guava:guava:32.1.3-jre"  // Duplicate!
}
```

**Causes:**
- Dependency declared in multiple scopes
- Dependency in both parent and child POM
- Bug in deduplication logic

**Solutions:**

1. **Manually edit PKL to remove duplicates**
2. **File a bug report** with your pom.xml/build.gradle

### Issue: Test Dependencies in Wrong Section

**Symptom:**
```pkl
packages {
  "org.junit.jupiter:junit-jupiter:5.10.1"  // Should be in testPackages!
}
```

**Causes:**
- Dependency missing `<scope>test</scope>` in Maven
- Dependency using `implementation` instead of `testImplementation` in Gradle

**Solutions:**

1. **Fix source build file:**
   ```xml
   <!-- Maven -->
   <dependency>
     <groupId>org.junit.jupiter</groupId>
     <artifactId>junit-jupiter</artifactId>
     <version>5.10.1</version>
     <scope>test</scope>  <!-- Add this -->
   </dependency>
   ```

   ```kotlin
   // Gradle
   dependencies {
     testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")  // Use testImplementation
   }
   ```

2. **Manually move in PKL:**
   ```pkl
   packages {
     // Remove from here
   }
   testPackages {
     "org.junit.jupiter:junit-jupiter:5.10.1"  // Add here
   }
   ```

### Issue: Repository URLs Incorrect

**Symptom:**
```pkl
repositories {
  ["central"] = "https://repo1.maven.org/maven2"  // Old URL!
}
```

**Expected:**
Maven Central should be `https://repo.maven.apache.org/maven2`.

**Solution:**
This is likely from an old POM. The URL still works, but you can manually update:
```pkl
repositories {
  ["central"] = "https://repo.maven.apache.org/maven2"
}
```

## Validation Issues

### Issue: Generated PKL Invalid

**Symptom:**
```bash
$ elide build
Error: Invalid PKL file: elide.pkl
  Line 15: Unexpected token '{'
```

**Solutions:**

1. **Check PKL syntax:**
   ```bash
   cat -n elide.pkl | sed -n '10,20p'  # Show lines 10-20
   ```

2. **Common syntax errors:**
   - Unbalanced braces `{}`
   - Missing quotes around strings
   - Invalid characters in dependency coordinates

3. **Validate with PKL CLI** (if available):
   ```bash
   pkl eval elide.pkl
   ```

4. **Regenerate:**
   ```bash
   rm elide.pkl
   elide adopt maven
   ```

### Issue: Build Fails After Conversion

**Symptom:**
```bash
$ elide build
Error: Could not resolve dependency: com.google.guava:guava:32.1.3-jre
```

**Causes:**
- Repository configuration incorrect
- Dependency coordinates wrong
- Network issues

**Solutions:**

1. **Check repository configuration:**
   ```pkl
   repositories {
     ["central"] = "https://repo.maven.apache.org/maven2"
   }
   ```

2. **Verify dependency exists:**
   ```bash
   curl -I https://repo.maven.apache.org/maven2/com/google/guava/guava/32.1.3-jre/guava-32.1.3-jre.pom
   ```

3. **Test with original build tool:**
   ```bash
   mvn clean compile  # Should work
   gradle build       # Should work
   ```

## Performance Issues

### Issue: Adoption Takes Too Long

**Symptom:**
```bash
# Command runs for 5+ minutes
elide adopt maven
```

**Causes:**
- Large multi-module project (100+ modules)
- Slow network (downloading many parent POMs/BOMs)
- Deep parent hierarchy

**Solutions:**

1. **Use --dry-run first** to see what will be downloaded:
   ```bash
   elide adopt maven --dry-run
   ```

2. **Check network speed:**
   ```bash
   time curl -o /dev/null https://repo.maven.apache.org/maven2/
   ```

3. **Populate local Maven repository first:**
   ```bash
   mvn dependency:go-offline
   elide adopt maven  # Will use local repo
   ```

4. **For multi-module, convert modules individually:**
   ```bash
   cd module-a
   elide adopt maven
   cd ../module-b
   elide adopt maven
   ```

## Getting Help

### Reporting Issues

When reporting issues, please include:

1. **Elide version:**
   ```bash
   elide --version
   ```

2. **Command used:**
   ```bash
   elide adopt maven --dry-run  # Full command
   ```

3. **Error output:**
   ```bash
   elide adopt maven 2>&1 | tee error.log
   ```

4. **Minimal reproduction:**
   - Simplified pom.xml or build.gradle.kts
   - Directory structure
   - Any parent POMs

5. **Environment:**
   - OS and version
   - Java version: `java -version`
   - Network setup (proxy, firewall, etc.)

### Diagnostic Commands

```bash
# Check if file exists
ls -la pom.xml build.gradle*

# Check if parent POM is accessible
curl -I https://repo.maven.apache.org/maven2/org/example/parent/1.0/parent-1.0.pom

# Check local Maven repository
ls -la ~/.m2/repository/

# Check Elide cache
ls -la ~/.elide/cache/maven/

# Test network connectivity
ping repo.maven.apache.org

# Check for DNS issues
nslookup repo.maven.apache.org
```

### Common Environment Issues

**Issue: Corporate Proxy**

**Solution:**
Set HTTP proxy environment variables:
```bash
export HTTP_PROXY=http://proxy.corp.com:8080
export HTTPS_PROXY=http://proxy.corp.com:8080
export NO_PROXY=localhost,127.0.0.1

elide adopt maven
```

**Issue: Certificate Validation Fails**

**Symptom:**
```bash
Error: SSL certificate verification failed
```

**Solution** (not recommended for production):
```bash
# Temporarily disable SSL verification (if your org uses internal CA)
# This is handled by your Java truststore
# You may need to import your corporate CA certificate
```

## Workarounds

### Temporary Manual Edits

If the adopter can't handle your project perfectly, you can:

1. **Simplify the build file temporarily**
2. **Run the adopter**
3. **Manually edit the generated PKL**
4. **Restore the original build file** (keep both during transition)

### Hybrid Approach

During migration, you can maintain both build systems:

```bash
# Development
elide build

# CI/CD (until fully migrated)
mvn clean install
# or
./gradlew build
```

This allows incremental migration and testing.

## Related Documentation

- [Migrating from Maven](./migrating-from-maven.md)
- [Migrating from Gradle](./migrating-from-gradle.md)
- [Elide CLI Guide](../cli-guide.md) _(coming soon)_

## FAQ

**Q: Can I convert both Maven and Gradle projects in the same directory?**

A: No - use `--output` to write to different files, or use subdirectories.

**Q: Will the adopter modify my pom.xml or build.gradle?**

A: No - the adopter only reads your build files and generates a new `elide.pkl`. Your original files are never modified.

**Q: Can I run the adopter multiple times?**

A: Yes - use `--dry-run` to preview, or `--output` to write to different files for comparison.

**Q: What if I have custom Maven plugins?**

A: Custom plugins are listed in comments. You'll need to find equivalent Elide functionality or configure manually.

**Q: Does the adopter work offline?**

A: Partially - it can work offline if:
- No parent POMs need to be downloaded
- No BOMs need to be resolved
- All dependency versions are explicit

Use `mvn dependency:go-offline` first to populate your local repository.
