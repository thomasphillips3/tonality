#!/bin/bash

# Exit on error
set -e

# Function to print section headers
print_header() {
    echo "============================================="
    echo "$1"
    echo "============================================="
}

# Create build directories
mkdir -p build_mac build_ios build_windows build_linux

# Build for macOS (AU, VST3, VST2, AAX, Standalone)
print_header "Building for macOS"
cd build_mac
cmake .. -G "Xcode" -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
cd ..

# Build for iOS (AUv3)
print_header "Building for iOS"
cd build_ios
cmake .. -G "Xcode" -DCMAKE_TOOLCHAIN_FILE=../CMakeLists.iOS.txt -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
cd ..

# Build for Windows (VST3, VST2, AAX, Standalone)
print_header "Building for Windows"
cd build_windows
cmake .. -G "Visual Studio 17 2022" -A x64 -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
cd ..

# Build for Linux (VST3, VST2, Standalone)
print_header "Building for Linux"
cd build_linux
cmake .. -G "Unix Makefiles" -DCMAKE_BUILD_TYPE=Release
cmake --build .
cd ..

print_header "Build completed for all platforms!" 