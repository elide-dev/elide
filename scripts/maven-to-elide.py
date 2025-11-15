#!/usr/bin/env python3
"""
Maven to Elide Converter

Converts single-module Maven projects to Elide pkl format.
Works for simple Maven projects without custom plugins.

Usage: python3 maven-to-elide.py [path-to-pom.xml]
"""

import xml.etree.ElementTree as ET
import sys
import os
from pathlib import Path


def parse_maven_pom(pom_path):
    """Parse Maven pom.xml and extract relevant information."""
    tree = ET.parse(pom_path)
    root = tree.getroot()

    # Handle Maven namespace
    ns = {'mvn': 'http://maven.apache.org/POM/4.0.0'}
    if root.tag.startswith('{'):
        # Extract namespace from root tag
        ns['mvn'] = root.tag[1:root.tag.index('}')]

    # Extract project info
    def find_text(path, default=''):
        elem = root.find(path, ns)
        return elem.text if elem is not None else default

    project_info = {
        'groupId': find_text('.//mvn:groupId'),
        'artifactId': find_text('.//mvn:artifactId'),
        'version': find_text('.//mvn:version'),
        'name': find_text('.//mvn:name'),
        'description': find_text('.//mvn:description'),
    }

    # Use artifactId as name if name is not specified
    if not project_info['name']:
        project_info['name'] = project_info['artifactId']

    # Build dependency management version map
    version_map = {}
    dep_mgmt = root.findall('.//mvn:dependencyManagement/mvn:dependencies/mvn:dependency', ns)
    for dep in dep_mgmt:
        group = dep.find('mvn:groupId', ns)
        artifact = dep.find('mvn:artifactId', ns)
        version = dep.find('mvn:version', ns)
        if group is not None and artifact is not None and version is not None:
            key = f"{group.text}:{artifact.text}"
            version_map[key] = version.text

    # Extract dependencies
    dependencies = {'compile': [], 'test': []}
    deps = root.findall('.//mvn:dependencies/mvn:dependency', ns)

    for dep in deps:
        group = dep.find('mvn:groupId', ns)
        artifact = dep.find('mvn:artifactId', ns)
        version = dep.find('mvn:version', ns)
        scope = dep.find('mvn:scope', ns)

        if group is not None and artifact is not None:
            coordinate = f"{group.text}:{artifact.text}"

            # Determine version: use explicit version, or look up in dependency management
            dep_version = None
            if version is not None and version.text:
                dep_version = version.text
            elif coordinate in version_map:
                dep_version = version_map[coordinate]

            # Only add dependency if we have a version
            if dep_version:
                coordinate += f":{dep_version}"
                scope_key = 'test' if scope is not None and scope.text == 'test' else 'compile'
                dependencies[scope_key].append(coordinate)
            else:
                print(f"Warning: Skipping {coordinate} (no version found)", file=sys.stderr)

    return project_info, dependencies


def generate_elide_pkl(project_info, dependencies, output_path):
    """Generate elide.pkl file from Maven project information."""

    pkl_content = f'''amends "elide:project.pkl"

name = "{project_info['artifactId']}"
description = "{project_info['description'] or project_info['name']}"

'''

    # Add dependencies section if there are any
    if dependencies['compile'] or dependencies['test']:
        pkl_content += 'dependencies {\n'
        pkl_content += '  maven {\n'

        if dependencies['compile']:
            pkl_content += '    packages {\n'
            for dep in dependencies['compile']:
                pkl_content += f'      "{dep}"\n'
            pkl_content += '    }\n'

        if dependencies['test']:
            pkl_content += '    testPackages {\n'
            for dep in dependencies['test']:
                pkl_content += f'      "{dep}"\n'
            pkl_content += '    }\n'

        pkl_content += '  }\n'
        pkl_content += '}\n\n'

    # Add source mappings
    pkl_content += '''sources {
  ["main"] = "src/main/java/**/*.java"
  ["test"] = "src/test/java/**/*.java"
}
'''

    # Write to file
    with open(output_path, 'w') as f:
        f.write(pkl_content)

    return pkl_content


def main():
    """Main entry point."""
    if len(sys.argv) > 1:
        pom_path = sys.argv[1]
    else:
        pom_path = 'pom.xml'

    if not os.path.exists(pom_path):
        print(f"Error: {pom_path} not found", file=sys.stderr)
        sys.exit(1)

    try:
        # Parse Maven POM
        print(f"Parsing {pom_path}...")
        project_info, dependencies = parse_maven_pom(pom_path)

        # Determine output path (same directory as pom.xml)
        pom_dir = os.path.dirname(os.path.abspath(pom_path))
        output_path = os.path.join(pom_dir, 'elide.pkl')

        # Generate elide.pkl
        print(f"Generating {output_path}...")
        content = generate_elide_pkl(project_info, dependencies, output_path)

        print(f"\n✓ Successfully created {output_path}")
        print(f"\nProject: {project_info['name']}")
        print(f"Compile dependencies: {len(dependencies['compile'])}")
        print(f"Test dependencies: {len(dependencies['test'])}")

        print("\nGenerated elide.pkl:")
        print("=" * 60)
        print(content)
        print("=" * 60)

    except ET.ParseError as e:
        print(f"Error parsing pom.xml: {e}", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
