cargo build --release

cargo run --features bindgen --bin uniffi-bindgen -- \
                   generate \
                   --library target/release/librink_bridge.so \
                   --language kotlin \
                   --out-dir ../../app/src/main/java

cargo ndk \
                 -t arm64-v8a \
                 -t x86_64 \
                 -o ../../app/src/main/jniLibs \
                 build --release
