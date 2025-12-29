### Platforms

The purpose of this folder is to provide platform specific entry points for launching the main jar installer
application. They can be manually generated using the tools here.

#### How-To:
Caution: DO NOT USE WITH NON-OPENJDK installed in JAVA_HOME. (license!)
- Place jar in ./jar folder
- Create installer runtime by running the create_min_runtime script
- Create binary by running create_executables after setting wanted binary type.