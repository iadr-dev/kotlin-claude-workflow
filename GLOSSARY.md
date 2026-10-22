# Kotlin workflow

A reusable Claude Code setup for Kotlin projects. This glossary fixes the words the environment check uses, so the skills, docs and issues describe the same thing the same way.

## Environment check

**Tool**:
A program the workflow relies on, such as `git`, a JDK, `gh` or `adb`, reported as one row of the environment check.
_Avoid_: dependency (clashes with Gradle dependencies), package

**Status**:
The verdict the environment check gives a tool: OK, UPDATE, OLD, MISSING or OPTIONAL.

**OK**:
The tool is installed and at the latest version, or no latest version is known.

**UPDATE**:
The tool is installed but older than the latest release. Advisory: the workflow still works.
_Avoid_: outdated (ambiguous between UPDATE and OLD)

**OLD**:
The tool is installed but below the minimum version the workflow needs. Unlike UPDATE, it is a problem.
_Avoid_: outdated

**MISSING**:
A tool the workflow requires is not installed.

**OPTIONAL**:
A tool the workflow can use but does not require is not installed.

**Pinned tool**:
A tool whose newer releases you have deliberately chosen not to be told about. The environment check reports it as OK instead of UPDATE, and still reports OLD, MISSING and OPTIONAL normally.
_Avoid_: ignored tool, locked version, frozen tool (a pin does not lock a version)
