# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

WPF desktop application for Windows, .NET 8.0, clean architecture: `UI → Application → Domain ← Infrastructure`. See [docs/architecture.md](docs/architecture.md) for the project layout, startup sequence, key domain services, persistence, and configuration.

## Commands

Build, test, and coverage commands: see [docs/build-and-test-commands.md](docs/build-and-test-commands.md).

## Testing Conventions

xUnit + Autofac.Extras.Moq + FluentAssertions patterns, integration test setup, and test data location: see [docs/testing-conventions.md](docs/testing-conventions.md).

## Key Conventions

Target frameworks, nullability, code style enforcement, logging, DI registration, and MVVM rules: see [docs/coding-conventions.md](docs/coding-conventions.md).

## Web Application

The web rewrite under `JPPhotoManagerWeb/` is a separate sub-project with its own `JPPhotoManagerWeb/CLAUDE.md` — see that file for its commands, architecture, and conventions.
