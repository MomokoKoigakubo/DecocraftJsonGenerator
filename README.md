# Decocraft JSON Generator

A JavaFX application for generating JSON configuration files for the Decocraft Minecraft mod.

## Features

- Drag & drop models (.bbmodel), textures, and icons
- Auto-match textures to models by naming convention
- Auto-create entries from icons with shared textures (decoref system)
- Rainbow chain linking (cycle through colors with tool)
- Wood chain linking (cycle through wood types)
- Animation support (parse from .bbmodel files)
- Flipbook animation support
- Light emission settings
- Auto-detect type (bed, seat, animated, etc.)
- Live JSON preview
- Automatic update notifications

## Installation

### Download
1. Go to [Releases](https://github.com/MomokoKoigakubo/DecocraftJsonGenerator/releases/latest)
2. Download the file for your platform:
   - **Linux:** `DecocraftJsonGenerator-X.X.X-linux.tar.gz`
   - **Windows:** `DecocraftJsonGenerator-X.X.X-windows.zip`

### Run

**Linux:**
```bash
tar -xzvf DecocraftJsonGenerator-*-linux.tar.gz
./DecocraftJsonGenerator/bin/DecocraftJsonGenerator
```
Or just extract and double-click `DecocraftJsonGenerator/bin/DecocraftJsonGenerator`

**Windows:**
Extract the zip and double-click `DecocraftJsonGenerator/DecocraftJsonGenerator.exe`

**No Java installation required** - the app bundles its own runtime.

## Usage

1. **Drop a model** (.bbmodel file) into the Models drop zone
2. **Drop icons** (unique item icons) into the Icons drop zone
3. **Drop textures** (shared materials) into the Textures drop zone
4. Click **Auto-Create All Entries** to match icons with textures
5. Select entries and edit properties in the right panel
6. Use **Rainbow Chain** or **Wood Chain** to link variants
7. Click **Export JSON** to save

## Building from Source

```bash
# Clone the repo
git clone https://github.com/MomokoKoigakubo/DecocraftJsonGenerator.git
cd DecocraftJsonGenerator

# Build the JAR
./gradlew jar

# Run
java -jar build/libs/DecocraftJsonGenerator-1.0.0.jar
```

## License

MIT License
