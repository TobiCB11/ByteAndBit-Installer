jlink \
  --module-path $JAVA_HOME/jmods \
  --add-modules java.base,java.desktop,java.sql,java.xml,jdk.attach \
  --output installer-runtime \
  --strip-debug \
  --compress 2 \
  --no-header-files \
  --no-man-pages
# IMPORTANT: DO NOT USE WITH NON-OPENJDK BUILDS DUE TO LICENSE RESTRICTIONS (JAVA_HOME MUST POINT TO AN OPENJDK BUILD)