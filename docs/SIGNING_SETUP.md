# APK Signing Setup with GitHub Secrets

This repository uses GitHub Secrets to securely sign release APKs in CI/CD workflows.

## Prerequisites

You need a Java keystore file (`.jks` or `.keystore`) with your signing key. If you don't have one, create it:

```bash
keytool -genkey -v -keystore my-release-key.jks \
  -alias apksignkey -keyalg RSA -keysize 2048 -validity 10000
```

## Setting Up GitHub Secrets

### Step 1: Encode Your Keystore to Base64

On Linux/macOS:
```bash
base64 -i my-release-key.jks | tr -d '\n' > keystore-base64.txt
```

On Windows (PowerShell):
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("my-release-key.jks")) | Out-File -FilePath keystore-base64.txt -NoNewline
```

### Step 2: Add GitHub Repository Secrets

Go to your repository settings → Secrets and variables → Actions → New repository secret

Add these 4 secrets:

| Secret Name | Value | Description |
|------------|-------|-------------|
| `SIGNING_KEYSTORE_BASE64` | Content of `keystore-base64.txt` | Your keystore encoded as base64 |
| `SIGNING_STORE_PASSWORD` | Your keystore password | Password you used when creating the keystore |
| `SIGNING_KEY_ALIAS` | `apksignkey` | The alias you used (default: `apksignkey`) |
| `SIGNING_KEY_PASSWORD` | Your key password | Usually same as store password |

### Step 3: Trigger a Build

Once secrets are configured, push to `main` branch or trigger a workflow manually. The release APK will be signed automatically.

## Security Notes

- ⚠️ **Never commit your keystore file or passwords to the repository**
- ✅ GitHub Secrets are encrypted and only exposed to workflow runs
- ✅ Secrets are not available to pull request workflows from forks (security feature)
- ✅ The keystore file is temporarily created during the build and deleted after

## Verifying Signed APKs

To verify an APK is signed:

```bash
# Check signature
apksigner verify --print-certs InstallerPlusFork-release-signed.apk

# Or using jarsigner
jarsigner -verify -verbose -certs InstallerPlusFork-release-signed.apk
```

## Local Builds

Local release builds will be unsigned unless you:
1. Create a `keystore.properties` file in the project root (add to `.gitignore`)
2. Set environment variables before building

Without signing configuration, local builds will still work but produce unsigned APKs.
