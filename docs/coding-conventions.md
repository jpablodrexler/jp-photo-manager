# Coding Conventions

- **Target framework:** `net8.0-windows10.0.17763.0` for UI; `net8.0-windows7.0` for other projects.
- **Nullable references:** enabled project-wide; respect nullability annotations.
- **Code style enforcement:** `<EnforceCodeStyleInBuild>True</EnforceCodeStyleInBuild>` — the build fails on style violations.
- **Logging:** log4net throughout; use the existing logger pattern, not `Console.WriteLine`.
- **DI registration:** register new services as Singletons in `App.xaml.cs ConfigureServices()`, following the existing pattern.
- **MVVM:** all UI logic belongs in ViewModels; code-behind (`.xaml.cs`) is only for wiring or WPF-specific concerns.
