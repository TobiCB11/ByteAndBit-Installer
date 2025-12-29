jpackage \
  --name ByteAndBit-Installer \
  --input ./jar \
  --main-jar installer-1.0-SNAPSHOT.jar \
  --main-class de.byteandbit.Installer \
  --type deb \
  --runtime-image installer-runtime
  # or exe/msi/deb/rpm