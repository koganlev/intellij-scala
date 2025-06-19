### Regenerate entities

To regenerate entity implementations, follow these steps:
  1. remove generated code inside [src directory](src) (`//region generated code`)
  2. remove [gen directory](gen)
  3. invoke `Generate implementation` intention on [SbtModuleEntity](src/com/intellij/entities/SbtModuleEntity.kt) interface
