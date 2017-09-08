# About

The plugin development environment for Foilen Infra.

License: The MIT License (MIT)


# Projects

- foilen-infra-plugin-model: The common objects models.
- foilen-infra-plugin-model-outputter: Some helper to output configuration from the models.
- foilen-infra-plugin-core: All the services definitions for plugins.
- foilen-infra-plugin-core-system-junits: The junits for testing any system implementation.
- foilen-infra-plugin-core-system-fake: An implementation of the services for pluging used in unit tests and standalone tests.

# Usage

## Dependency

You can see the latest version and the Maven and Gradle settings here:

https://bintray.com/foilen/maven/com.foilen:foilen-infra-plugin

## Plugin

- See docs/plugin_creation.odt

## System

- Use foilen-infra-plugin-core-system-common
- Implement all the services:
 - com.foilen.infra.plugin.v1.core.service.IPResourceService
 - com.foilen.infra.plugin.v1.core.service.MessagingService
 - com.foilen.infra.plugin.v1.core.service.RealmPluginService
 - com.foilen.infra.plugin.v1.core.service.TimerService
 - com.foilen.infra.plugin.v1.core.service.internal.InternalChangeService
 - com.foilen.infra.plugin.v1.core.service.internal.InternalIPResourceService
- Init the system with InfraPluginCommonInit.init(commonServicesContext, internalServicesContext);

## System test

- Use foilen-infra-plugin-core-system-junits (can be in the same project, but in the "test" configuration)
- Create a unit test that extends AbstractIPResourceServiceTest

# Process

Versioning:
- The version number is in the format MAJOR.MINOR.BUGFIX (e.g 0.1.0).
- The API in a MAJOR release is stable. Everything that will be removed in the next MAJOR release are marked as deprecated.

For changes/removals in the stable API:
- When something is in the stable API, it will be there for all the releases in the same MAJOR version.
- Everything that will be removed in the next MAJOR version is marked as @deprecated and the Javadoc will explain what to use instead if there is a workaround.
