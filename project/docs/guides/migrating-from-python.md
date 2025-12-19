# Migrating from Python to Elide

This guide walks you through converting a Python project to Elide using the `elide adopt python` command.

> **⚠️ Status**: Python adopter is currently in development. This guide documents the planned functionality and design.

## Table of Contents

- [Quick Start](#quick-start)
- [Basic Usage](#basic-usage)
- [Understanding the Output](#understanding-the-output)
- [Advanced Features](#advanced-features)
- [Virtual Environments](#virtual-environments)
- [Common Scenarios](#common-scenarios)
- [Limitations](#limitations)
- [Next Steps](#next-steps)

## Quick Start

Once implemented, converting your Python project to Elide will be simple:

```bash
# Navigate to your Python project
cd my-python-project

# Convert to Elide format
elide adopt python

# Review the generated elide.pkl
cat elide.pkl

# Build with Elide
elide build
```

The adopter will automatically:
- Parse `pyproject.toml`, `requirements.txt`, `setup.py`, or `Pipfile`
- Convert dependencies to PKL format
- Detect project metadata
- Configure Python version requirements
- Handle dev/optional dependencies

## Basic Usage

### Convert a Python Project

```bash
# Auto-detect Python configuration in current directory
elide adopt python

# Specify a specific configuration file
elide adopt python pyproject.toml
elide adopt python requirements.txt
elide adopt python setup.py
elide adopt python Pipfile

# Preview without writing (dry run)
elide adopt python --dry-run
```

### Command Options

```bash
elide adopt python [OPTIONS] [CONFIG_FILE]

Options:
  --dry-run              Preview PKL output without writing to file
  --output, -o FILE      Write output to specific file (default: elide.pkl)
  --force, -f            Overwrite existing elide.pkl if it exists
  --python-version VER   Specify Python version (default: auto-detect)
  --help                 Show help message
```

## Understanding the Output

The adopter will generate an `elide.pkl` file that mirrors your Python configuration.

### Example: pyproject.toml

**Before (pyproject.toml):**
```toml
[project]
name = "my-app"
version = "1.0.0"
description = "A sample Python application"
requires-python = ">=3.11"

dependencies = [
    "fastapi>=0.104.0",
    "uvicorn[standard]>=0.24.0",
    "pydantic>=2.5.0",
    "sqlalchemy>=2.0.0"
]

[project.optional-dependencies]
dev = [
    "pytest>=7.4.0",
    "pytest-asyncio>=0.21.0",
    "black>=23.11.0",
    "ruff>=0.1.0"
]

[tool.black]
line-length = 100
target-version = ['py311']

[tool.ruff]
line-length = 100
target-version = "py311"
```

**After (elide.pkl):**
```pkl
amends "elide:project.pkl"

name = "my-app"
version = "1.0.0"
description = "A sample Python application"

python {
  version = ">=3.11"
}

dependencies {
  pypi {
    packages {
      "fastapi>=0.104.0"
      "uvicorn[standard]>=0.24.0"
      "pydantic>=2.5.0"
      "sqlalchemy>=2.0.0"
    }
    devPackages {
      "pytest>=7.4.0"
      "pytest-asyncio>=0.21.0"
      "black>=23.11.0"
      "ruff>=0.1.0"
    }
  }
}

// Tool configurations detected (manual conversion may be needed):
//   black: line-length=100, target-version=py311
//   ruff: line-length=100, target-version=py311

sources {
  ["main"] = "src/**/*.py"
  ["test"] = "tests/**/*.py"
}
```

### Example: requirements.txt

**Before (requirements.txt):**
```
flask==3.0.0
requests>=2.31.0
python-dotenv~=1.0.0
psycopg2-binary>=2.9.0,<3.0.0

# Development dependencies
pytest>=7.4.0  # test framework
black>=23.11.0  # code formatter
```

**After (elide.pkl):**
```pkl
amends "elide:project.pkl"

name = "my-app"  // Derived from directory name

dependencies {
  pypi {
    packages {
      "flask==3.0.0"
      "requests>=2.31.0"
      "python-dotenv~=1.0.0"
      "psycopg2-binary>=2.9.0,<3.0.0"
    }
    // Note: Dev dependencies detected in comments:
    //   pytest>=7.4.0
    //   black>=23.11.0
  }
}

sources {
  ["main"] = "**/*.py"
}
```

### Output Structure

- **Project Metadata**: `name`, `version`, `description`, `authors`
- **Python Version**: `python.version` for version constraints
- **Dependencies**: Split into `packages` (dependencies) and `devPackages` (dev/test)
- **Tool Configurations**: Listed in comments (black, ruff, mypy, etc.)
- **Sources**: Standard Python directory layout

## Advanced Features

### Dependency Format Support

The adopter will handle different Python configuration formats:

| Format | File | Priority | Notes |
|--------|------|----------|-------|
| **pyproject.toml** | `pyproject.toml` | 1 | Preferred (PEP 621) |
| **Pipfile** | `Pipfile` / `Pipfile.lock` | 2 | Pipenv projects |
| **requirements.txt** | `requirements.txt` | 3 | Traditional format |
| **setup.py** | `setup.py` | 4 | Legacy packaging |
| **poetry.lock** | `poetry.lock` | - | Poetry (via pyproject.toml) |

If multiple formats exist, the adopter will use the highest priority format.

### pyproject.toml (PEP 621)

Full support for modern Python packaging:

```toml
[project]
name = "my-library"
version = "2.1.0"
description = "A useful library"
readme = "README.md"
requires-python = ">=3.9"
license = {text = "MIT"}
authors = [
    {name = "Your Name", email = "you@example.com"}
]

dependencies = [
    "httpx>=0.25.0",
    "pydantic>=2.5.0"
]

[project.optional-dependencies]
dev = [
    "pytest>=7.4.0",
    "mypy>=1.7.0"
]
test = [
    "pytest-cov>=4.1.0",
    "pytest-asyncio>=0.21.0"
]

[project.scripts]
my-cli = "my_library.cli:main"

[build-system]
requires = ["setuptools>=68.0", "wheel"]
build-backend = "setuptools.build_meta"
```

**Generated PKL:**
```pkl
amends "elide:project.pkl"

name = "my-library"
version = "2.1.0"
description = "A useful library"

python {
  version = ">=3.9"
}

dependencies {
  pypi {
    packages {
      "httpx>=0.25.0"
      "pydantic>=2.5.0"
    }
    devPackages {
      "pytest>=7.4.0"
      "mypy>=1.7.0"
      "pytest-cov>=4.1.0"
      "pytest-asyncio>=0.21.0"
    }
  }
}

// Entry points detected:
//   my-cli: my_library.cli:main

// Build system: setuptools.build_meta

sources {
  ["main"] = "src/**/*.py"
  ["test"] = "tests/**/*.py"
}
```

### Pipfile Support

Pipenv configuration support:

**Pipfile:**
```toml
[[source]]
url = "https://pypi.org/simple"
verify_ssl = true
name = "pypi"

[packages]
django = ">=4.2.0"
djangorestframework = "*"
celery = {extras = ["redis"], version = ">=5.3.0"}

[dev-packages]
pytest = "*"
pytest-django = "*"
black = "*"

[requires]
python_version = "3.11"
```

**Generated PKL:**
```pkl
amends "elide:project.pkl"

python {
  version = "3.11"
}

dependencies {
  pypi {
    repositories {
      ["pypi"] = "https://pypi.org/simple"
    }
    packages {
      "django>=4.2.0"
      "djangorestframework"
      "celery[redis]>=5.3.0"
    }
    devPackages {
      "pytest"
      "pytest-django"
      "black"
    }
  }
}

sources {
  ["main"] = "**/*.py"
}
```

### Version Specifiers

Python version specifiers are preserved:

```python
# requirements.txt
flask==3.0.0          # Exact version
requests>=2.31.0      # Minimum version
python-dotenv~=1.0.0  # Compatible release
psycopg2>=2.9.0,<3.0.0  # Version range
httpx[http2]>=0.25.0  # With extras
```

**Generated PKL:**
```pkl
dependencies {
  pypi {
    packages {
      "flask==3.0.0"
      "requests>=2.31.0"
      "python-dotenv~=1.0.0"
      "psycopg2>=2.9.0,<3.0.0"
      "httpx[http2]>=0.25.0"
    }
  }
}
```

### Extra Dependencies

Extras (optional features) are supported:

```toml
[project.optional-dependencies]
dev = ["pytest>=7.4.0", "black>=23.11.0"]
docs = ["sphinx>=7.2.0", "sphinx-rtd-theme>=2.0.0"]
all = ["my-library[dev,docs]"]
```

**Generated PKL:**
```pkl
dependencies {
  pypi {
    packages {
      // Base dependencies here
    }
    devPackages {
      "pytest>=7.4.0"
      "black>=23.11.0"
    }
  }
}

// Note: Additional extras detected:
//   docs: sphinx>=7.2.0, sphinx-rtd-theme>=2.0.0
//   all: my-library[dev,docs]
```

## Virtual Environments

The adopter will detect and note virtual environment configurations:

### venv/virtualenv

```bash
# Standard Python virtual environment
python -m venv .venv
```

Detection: The adopter will note if `.venv`, `venv`, or `.virtualenv` directories exist.

### Poetry

```toml
[tool.poetry]
name = "my-app"
version = "1.0.0"

[tool.poetry.dependencies]
python = "^3.11"
fastapi = "^0.104.0"

[tool.poetry.dev-dependencies]
pytest = "^7.4.0"
```

The adopter will parse Poetry's dependency format and convert to standard Elide format.

### conda

```yaml
# environment.yml
name: my-env
channels:
  - conda-forge
  - defaults
dependencies:
  - python=3.11
  - numpy>=1.26.0
  - pandas>=2.1.0
  - pip:
      - fastapi>=0.104.0
```

**Note**: Conda-specific packages may need manual configuration in Elide.

## Common Scenarios

### Scenario 1: FastAPI Application

**pyproject.toml:**
```toml
[project]
name = "fastapi-app"
version = "1.0.0"
requires-python = ">=3.11"

dependencies = [
    "fastapi>=0.104.0",
    "uvicorn[standard]>=0.24.0",
    "pydantic>=2.5.0",
    "sqlalchemy>=2.0.0",
    "alembic>=1.13.0"
]

[project.optional-dependencies]
dev = [
    "pytest>=7.4.0",
    "pytest-asyncio>=0.21.0",
    "httpx>=0.25.0"
]
```

**Generated elide.pkl:**
```pkl
amends "elide:project.pkl"

name = "fastapi-app"
version = "1.0.0"

python {
  version = ">=3.11"
}

dependencies {
  pypi {
    packages {
      "fastapi>=0.104.0"
      "uvicorn[standard]>=0.24.0"
      "pydantic>=2.5.0"
      "sqlalchemy>=2.0.0"
      "alembic>=1.13.0"
    }
    devPackages {
      "pytest>=7.4.0"
      "pytest-asyncio>=0.21.0"
      "httpx>=0.25.0"
    }
  }
}

sources {
  ["main"] = "app/**/*.py"
  ["test"] = "tests/**/*.py"
}
```

### Scenario 2: Django Project

**requirements.txt:**
```
Django==5.0.0
djangorestframework==3.14.0
django-cors-headers==4.3.0
psycopg2-binary==2.9.9
celery[redis]==5.3.4
python-dotenv==1.0.0

# Dev dependencies
pytest-django==4.7.0
black==23.12.0
django-debug-toolbar==4.2.0
```

**Generated elide.pkl:**
```pkl
amends "elide:project.pkl"

name = "django-project"

python {
  version = ">=3.10"  // Auto-detected from Django version
}

dependencies {
  pypi {
    packages {
      "Django==5.0.0"
      "djangorestframework==3.14.0"
      "django-cors-headers==4.3.0"
      "psycopg2-binary==2.9.9"
      "celery[redis]==5.3.4"
      "python-dotenv==1.0.0"
    }
    // Note: Dev dependencies detected in comments:
    //   pytest-django==4.7.0
    //   black==23.12.0
    //   django-debug-toolbar==4.2.0
  }
}

sources {
  ["main"] = "**/*.py"
}
```

### Scenario 3: Data Science Project

**pyproject.toml:**
```toml
[project]
name = "ml-pipeline"
version = "0.1.0"
requires-python = ">=3.10"

dependencies = [
    "numpy>=1.26.0",
    "pandas>=2.1.0",
    "scikit-learn>=1.3.0",
    "matplotlib>=3.8.0",
    "jupyter>=1.0.0"
]

[project.optional-dependencies]
dev = [
    "pytest>=7.4.0",
    "ipython>=8.18.0",
    "black>=23.11.0"
]
```

**Generated elide.pkl:**
```pkl
amends "elide:project.pkl"

name = "ml-pipeline"
version = "0.1.0"

python {
  version = ">=3.10"
}

dependencies {
  pypi {
    packages {
      "numpy>=1.26.0"
      "pandas>=2.1.0"
      "scikit-learn>=1.3.0"
      "matplotlib>=3.8.0"
      "jupyter>=1.0.0"
    }
    devPackages {
      "pytest>=7.4.0"
      "ipython>=8.18.0"
      "black>=23.11.0"
    }
  }
}

sources {
  ["main"] = "src/**/*.py"
  ["notebooks"] = "notebooks/**/*.ipynb"
}
```

### Scenario 4: React + Python Monorepo

This is a key use case for Elide's polyglot support:

```
fullstack-app/
├── web/                 # React frontend
│   └── package.json
├── api/                 # Python backend
│   └── pyproject.toml
└── elide.pkl           # Root configuration
```

**api/pyproject.toml:**
```toml
[project]
name = "api"
version = "1.0.0"
requires-python = ">=3.11"

dependencies = [
    "fastapi>=0.104.0",
    "uvicorn[standard]>=0.24.0"
]
```

**web/package.json:**
```json
{
  "name": "web",
  "dependencies": {
    "react": "^18.2.0",
    "axios": "^1.6.0"
  }
}
```

**Root elide.pkl (after running adopters):**
```pkl
amends "elide:project.pkl"

name = "fullstack-app"

workspaces {
  "web"
  "api"
}

dependencies {
  npm {
    packages {
      "react@^18.2.0"
      "axios@^1.6.0"
    }
  }
  pypi {
    packages {
      "fastapi>=0.104.0"
      "uvicorn[standard]>=0.24.0"
    }
  }
}
```

## Limitations

### Not Yet Supported

1. **setup.py Parsing**: Complex `setup.py` files with programmatic logic
2. **Conda Packages**: Conda-specific packages (non-PyPI)
3. **Custom Indices**: Private PyPI indices may need manual configuration
4. **Build Hooks**: Custom build scripts and hooks
5. **C Extensions**: Extension build configuration

### Partially Supported

1. **requirements.txt Comments**: Dev dependency detection from comments is heuristic
2. **Extras**: Complex extra dependency specifications
3. **Platform-Specific**: Platform markers (`sys_platform == "win32"`)
4. **Python Version Ranges**: Complex version specifications

### Known Issues

1. **Git Dependencies**: Dependencies from git URLs require special handling
2. **Local Packages**: Editable installs (`pip install -e .`) need manual configuration
3. **Environment Variables**: Variable substitution in configuration files

## Troubleshooting

### Issue: "No Python configuration found"

**Problem**: The adopter can't find any Python configuration files

**Solution**:
```bash
# Check what files exist
ls -la pyproject.toml requirements.txt setup.py Pipfile

# Specify the file explicitly
elide adopt python pyproject.toml
```

### Issue: "Failed to parse pyproject.toml"

**Problem**: Invalid TOML syntax

**Solution**:
```bash
# Validate your pyproject.toml
python -m pip install tomli
python -c "import tomli; tomli.load(open('pyproject.toml', 'rb'))"

# Or use a TOML linter
pip install toml-sort
toml-sort pyproject.toml --check
```

### Issue: Python version not detected

**Problem**: No `requires-python` or version info found

**Solution**:
```bash
# Specify explicitly
elide adopt python --python-version ">=3.11"

# Or add to pyproject.toml
# [project]
# requires-python = ">=3.11"
```

## Next Steps

After conversion:

1. **Review Generated PKL**: Check that all dependencies were captured correctly
   ```bash
   cat elide.pkl
   ```

2. **Verify Python Version**: Ensure Python version requirements are correct
   ```pkl
   python {
     version = ">=3.11"
   }
   ```

3. **Test Installation**: Verify dependencies install correctly
   ```bash
   elide install
   ```

4. **Run Tests**: Verify your tests still pass
   ```bash
   elide test
   ```

5. **Commit**: Add the new `elide.pkl` to version control
   ```bash
   git add elide.pkl
   git commit -m "feat: adopt Elide build system"
   ```

6. **Optional: Keep Python Config**: You can keep your original files during transition
   ```bash
   # Use Elide for development
   elide build

   # Keep pip/poetry for CI/CD during migration
   pip install -r requirements.txt
   ```

## Implementation Status

The Python adopter is currently in development. Planned timeline:

- **Phase 1**: `pyproject.toml` (PEP 621) support ⏳
- **Phase 2**: `requirements.txt` parsing ⏳
- **Phase 3**: `Pipfile` support ⏳
- **Phase 4**: `setup.py` legacy support ⏳
- **Phase 5**: Poetry-specific features ⏳

Want to contribute or track progress? See the [Elide GitHub repository](https://github.com/elide-dev/elide).

## Related Documentation

- [Migrating from Node.js](./migrating-from-nodejs.md)
- [Migrating from Maven](./migrating-from-maven.md)
- [Migrating from Gradle](./migrating-from-gradle.md)
- [Troubleshooting Guide](./adopt-troubleshooting.md)
- [Elide PKL Reference](../elide-pkl-reference.md) _(coming soon)_
- [Elide CLI Guide](../cli-guide.md) _(coming soon)_

---

**Note**: This guide documents planned functionality. Actual implementation may vary. Check the Elide documentation for the latest status.
