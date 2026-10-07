# Shared UI

Reusable visual foundations belong here, including the shared `Core5x5Theme`, measured dark tokens, and reusable workout, navigation, timer, history, and settings components. This is not a global presentation layer: feature screens, ViewModels, and business rules do not belong in this module.

There are no project-module dependencies. Feature modules and `:shared` may use this module. The design handoff is implemented incrementally; full Light/System theme selection and unspecified states remain deferred.

See [the architecture guide](../../../docs/ARCHITECTURE.md).
