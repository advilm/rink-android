cargo build --release

cargo run --features bindgen --bin uniffi-bindgen -- \
                   generate \
                   --library target/release/librink_bridge.so \
                   --language kotlin \
                   --out-dir ../../app/src/main/java

read -r -a android_abis <<< "${ANDROID_ABIS:-arm64-v8a x86_64}"
ndk_targets=()
for abi in "${android_abis[@]}"; do
    ndk_targets+=( -t "$abi" )
done

cargo ndk "${ndk_targets[@]}" \
                 -o ../../app/src/main/jniLibs \
                 build --release
